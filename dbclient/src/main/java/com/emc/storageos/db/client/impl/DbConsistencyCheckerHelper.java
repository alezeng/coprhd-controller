/*
 * Copyright (c) 2008-2015 EMC Corporation
 * All Rights Reserved
 */

package com.emc.storageos.db.client.impl;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.emc.storageos.db.client.URIUtil;
import com.emc.storageos.db.client.model.DataObject;
import com.emc.storageos.db.client.model.NamedURI;
import com.emc.storageos.db.client.model.PasswordHistory;
import com.emc.storageos.db.client.model.ScopedLabel;
import com.emc.storageos.db.exceptions.DatabaseException;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.connectionpool.OperationResult;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.model.Rows;
import com.netflix.astyanax.query.ColumnFamilyQuery;
import com.netflix.astyanax.serializers.CompositeRangeBuilder;
import com.netflix.astyanax.util.RangeBuilder;

public class DbConsistencyCheckerHelper {
    private static final Logger _log = LoggerFactory.getLogger(DbConsistencyCheckerHelper.class);
    private static final int INDEX_OBJECTS_BATCH_SIZE = 10000;

    private static final String DELETE_INDEX_CQL = "delete from \"%s\" where key='%s' and column1='%s' and column2='%s' and column3='%s' and column4='%s' and column5=%s;";
    private static final String DELETE_INDEX_CQL_WITHOUT_UUID = "delete from \"%s\" where key='%s' and column1='%s' and column2='%s' and column3='%s' and column4='%s';";

    private DbClientImpl dbClient;
    private Set<Class<? extends DataObject>> excludeClasses = new HashSet<Class<? extends DataObject>>(Arrays.asList(PasswordHistory.class));

    public DbConsistencyCheckerHelper() {
    }

    public DbConsistencyCheckerHelper(DbClientImpl dbClient) {
        this.dbClient = dbClient;
    }

    /**
     * Find out all rows in DataObject CFs that can't be deserialized,
     * such as such as object id cannot be converted to URI.
     *
     * @return number of corrupted rows
     */
    public int checkDataObject(DataObjectType doType, boolean toConsole) {
        int dirtyCount = 0;
        _log.info("Check CF {}", doType.getDataObjectClass().getName());
        
        if (excludeClasses.contains(doType.getDataObjectClass())) {
        	_log.info("Skip CF {} since its URI is special", doType.getDataObjectClass());
        	return 0;
        }
        
        try {
            OperationResult<Rows<String, CompositeColumnName>> result = dbClient.getKeyspace(
                    doType.getDataObjectClass()).prepareQuery(doType.getCF())
                    .getAllRows().setRowLimit(dbClient.DEFAULT_PAGE_SIZE)
                    .withColumnRange(new RangeBuilder().setLimit(1).build())
                    .execute();
            for (Row<String, CompositeColumnName> row : result.getResult()) {
            	if (!isValidDataObjectKey(URI.create(row.getKey()), doType.getDataObjectClass())) {
            		dirtyCount++;
    		        logMessage(String.format("Inconsistency found: Row key '%s' failed to convert to URI in CF %s",
    		                row.getKey(), doType.getDataObjectClass().getName()), true, toConsole);
            	}
            }

        } catch (ConnectionException e) {
            throw DatabaseException.retryables.connectionFailed(e);
        }

        return dirtyCount;
    }

    /**
     * Scan all the data object records, to find out the object record is existing but the related index is missing.
     *
     * @param doType
     * @param toConsole whether print out in the console
     * @return the number of corrupted data
     * @throws ConnectionException
     */
    public int checkCFIndices(DataObjectType doType, boolean toConsole) throws ConnectionException {
        int dirtyCount = 0;
        Class objClass = doType.getDataObjectClass();
        _log.info("Check Data Object CF {}", objClass);

        Map<String, ColumnField> indexedFields = new HashMap<String, ColumnField>();
        for (ColumnField field : doType.getColumnFields()) {
            if (field.getIndex() == null) {
                continue;
            }
            indexedFields.put(field.getName(), field);
        }

        if (indexedFields.isEmpty()) {
            return dirtyCount;
        }
        
        Keyspace keyspace = dbClient.getKeyspace(objClass);
        ColumnFamilyQuery<String, CompositeColumnName> query = keyspace.prepareQuery(doType.getCF());
        OperationResult<Rows<String, CompositeColumnName>> result = query.getAllRows().setRowLimit(dbClient.DEFAULT_PAGE_SIZE).execute();

        for (Row<String, CompositeColumnName> objRow : result.getResult()) {
            boolean inactiveObject = false;
            
            for (Column<CompositeColumnName> column : objRow.getColumns()) {
                if (column.getName().equals(DataObject.INACTIVE_FIELD_NAME) && column.getBooleanValue() == true) {
                	inactiveObject = true;
                }
            }
            
            if (inactiveObject) {
            	continue;
            }

            for (Column<CompositeColumnName> column : objRow.getColumns()) {
            	if (!indexedFields.containsKey(column.getName().getOne())) {
            		continue;
            	}
            	
            	// we don't build index if the value is null, refer to ColumnField.
                if (!column.hasValue()) {
                    continue;
                }
            	
            	ColumnField indexedField = indexedFields.get(column.getName().getOne());
            	String indexKey = getIndexKey(indexedField, column);
            	
                if (indexKey == null) {
                    continue;
                }
                
                boolean isColumnInIndex = isColumnInIndex(keyspace, indexedField.getIndexCF(), indexKey,
                        getIndexColumns(indexedField, column, objRow.getKey()));
                
                if (!isColumnInIndex) {
                    dirtyCount++;
                    logMessage(String.format(
                            "Inconsistency found Object(%s, id: %s, field: %s) is existing, but the related Index(%s, type: %s, id: %s) is missing.",
                            indexedField.getDataObjectType().getSimpleName(), objRow.getKey(), indexedField.getName(),
                            indexedField.getIndexCF().getName(), indexedField.getIndex().getClass().getSimpleName(), indexKey),
                            true, toConsole);
                    DbCheckerFileWriter.writeTo(DbCheckerFileWriter.WRITER_REBUILD_INDEX,
                            String.format("id:%s, cfName:%s", objRow.getKey(),
                                    indexedField.getDataObjectType().getSimpleName()));
                }
            }
        }

        return dirtyCount;
    }

    /**
     * Scan all the indices and related data object records, to find out
     * the index record is existing but the related data object records is missing.
     *
     * @return number of the corrupted rows in this index CF
     * @throws ConnectionException
     */
    public int checkIndexingCF(IndexAndCf indexAndCf, boolean toConsole) throws ConnectionException {
        int corruptRowCount = 0;

        String indexCFName = indexAndCf.cf.getName();
        Map<String, ColumnFamily<String, CompositeColumnName>> objCfs = getDataObjectCFs();
        _log.info("Start checking the index CF {}", indexCFName);

        Map<ColumnFamily<String, CompositeColumnName>, Map<String, List<IndexEntry>>> objsToCheck = new HashMap<>();

        ColumnFamilyQuery<String, IndexColumnName> query = indexAndCf.keyspace
                .prepareQuery(indexAndCf.cf);

        OperationResult<Rows<String, IndexColumnName>> result = query.getAllRows()
                .setRowLimit(dbClient.DEFAULT_PAGE_SIZE)
                .withColumnRange(new RangeBuilder().setLimit(dbClient.DEFAULT_PAGE_SIZE).build())
                .execute();

        for (Row<String, IndexColumnName> row : result.getResult()) {
            ColumnList<IndexColumnName> columns = row.getColumns();
            
            for (Column<IndexColumnName> column : columns) {
                ObjectEntry objEntry = extractObjectEntryFromIndex(row.getKey(),
                        column.getName(), indexAndCf.indexType, toConsole);
                if (objEntry == null) {
                    continue;
                }
                ColumnFamily<String, CompositeColumnName> objCf = objCfs
                        .get(objEntry.getClassName());

                if (objCf == null) {
                    logMessage(String.format("DataObject does not exist for %s", row.getKey()), true, toConsole);
                    continue;
                }

                Map<String, List<IndexEntry>> objKeysIdxEntryMap = objsToCheck.get(objCf);
                if (objKeysIdxEntryMap == null) {
                    objKeysIdxEntryMap = new HashMap<>();
                    objsToCheck.put(objCf, objKeysIdxEntryMap);
                }
                List<IndexEntry> idxEntries = objKeysIdxEntryMap.get(objEntry.getObjectId());
                if (idxEntries == null) {
                    idxEntries = new ArrayList<>();
                    objKeysIdxEntryMap.put(objEntry.getObjectId(), idxEntries);
                }
                idxEntries.add(new IndexEntry(row.getKey(), column.getName()));
            }
            
            if (getObjsSize(objsToCheck) >= INDEX_OBJECTS_BATCH_SIZE ) {
                corruptRowCount += processBatchIndexObjects(indexAndCf, toConsole, objsToCheck);
            }
            
        }

        // Detect whether the DataObject CFs have the records
        corruptRowCount += processBatchIndexObjects(indexAndCf, toConsole, objsToCheck);

        return corruptRowCount;
    }

    private int getObjsSize(Map<ColumnFamily<String, CompositeColumnName>, Map<String, List<IndexEntry>>> objsToCheck) {
        int size = 0;
        for (Map<String, List<IndexEntry>> objMap : objsToCheck.values()) {
            for (List<IndexEntry> objs : objMap.values()) {
                size += objs.size();
            }
        }
        return size;
    }

    /*
     * We need to process index objects in batch to avoid occupy too many memory
     * */
    private int processBatchIndexObjects(IndexAndCf indexAndCf, boolean toConsole,
            Map<ColumnFamily<String, CompositeColumnName>, Map<String, List<IndexEntry>>> objsToCheck) throws ConnectionException {
        int corruptRowCount = 0;
        for (ColumnFamily<String, CompositeColumnName> objCf : objsToCheck.keySet()) {
            Map<String, List<IndexEntry>> objKeysIdxEntryMap = objsToCheck.get(objCf);

            OperationResult<Rows<String, CompositeColumnName>> objResult = indexAndCf.keyspace
                    .prepareQuery(objCf).getRowSlice(objKeysIdxEntryMap.keySet())
                    .withColumnRange(new RangeBuilder().setLimit(1).build())
                    .execute();
            for (Row<String, CompositeColumnName> row : objResult.getResult()) {
                if (row.getColumns().isEmpty()) { // Only support all the columns have been removed now
                    List<IndexEntry> idxEntries = objKeysIdxEntryMap.get(row.getKey());
                    for (IndexEntry idxEntry : idxEntries) {
                        corruptRowCount++;
                        logMessage(String.format("Inconsistency found: Index(%s, type: %s, id: %s, column: %s) is existing "
                                + "but the related object record(%s, id: %s) is missing.",
                                indexAndCf.cf.getName(), indexAndCf.indexType.getSimpleName(),
                                idxEntry.getIndexKey(), idxEntry.getColumnName(),
                                objCf.getName(), row.getKey()), true, toConsole);
                        UUID timeUUID = idxEntry.getColumnName().getTimeUUID();
                        DbCheckerFileWriter.writeTo(indexAndCf.keyspace.getKeyspaceName(),
                                String.format(timeUUID != null ? DELETE_INDEX_CQL : DELETE_INDEX_CQL_WITHOUT_UUID,
                                        indexAndCf.cf.getName(), idxEntry.getIndexKey(), idxEntry.getColumnName().getOne(),
                                        idxEntry.getColumnName().getTwo(),
                                        handleNullValue(idxEntry.getColumnName().getThree()),
                                        handleNullValue(idxEntry.getColumnName().getFour()),
                                        timeUUID));
                    }
                }
            }
        }
        objsToCheck.clear();
        return corruptRowCount;
    }

    public Map<String, IndexAndCf> getAllIndices() {
        // Map<Index_CF_Name, <DbIndex, ColumnFamily, Map<Class_Name, object-CF_Name>>>
        Map<String, IndexAndCf> allIdxCfs = new TreeMap<>();
        for (DataObjectType objType : TypeMap.getAllDoTypes()) {
            Map<String, IndexAndCf> idxCfs = getIndicesOfCF(objType);
            allIdxCfs.putAll(idxCfs);
        }

        return allIdxCfs;
    }

    public Map<String, IndexAndCf> getIndicesOfCF(DataObjectType objType) {
        Map<String, IndexAndCf> idxCfs = new TreeMap<>();
        Keyspace keyspace = dbClient.getKeyspace(objType.getDataObjectClass());
        for (ColumnField field : objType.getColumnFields()) {
            DbIndex index = field.getIndex();
            if (index == null) {
                continue;
            }

            IndexAndCf indexAndCf = new IndexAndCf(index.getClass(), field.getIndexCF(), keyspace);
            String key = indexAndCf.generateKey();
            IndexAndCf idxAndCf = idxCfs.get(key);
            if (idxAndCf == null) {
                idxAndCf = new IndexAndCf(index.getClass(), field.getIndexCF(), keyspace);
                idxCfs.put(key, idxAndCf);
            }
        }
        return idxCfs;
    }

    public Map<String, ColumnFamily<String, CompositeColumnName>> getDataObjectCFs() {
        Map<String, ColumnFamily<String, CompositeColumnName>> objCfs = new TreeMap<>();
        for (DataObjectType objType : TypeMap.getAllDoTypes()) {
            String simpleClassName = objType.getDataObjectClass().getSimpleName();
            ColumnFamily<String, CompositeColumnName> objCf = objCfs.get(simpleClassName);
            if (objCf == null) {
                objCfs.put(simpleClassName, objType.getCF());
            }
        }

        return objCfs;
    }

    void logMessage(String msg, boolean isError, boolean toConsole) {
        if (isError) {
            _log.error(msg);
            if (toConsole) {
                System.err.println(msg);
            }
            return;
        }

        _log.info(msg);
        if (toConsole) {
            System.out.println(msg);
        }
    }

    /*
     * This class records the Index Data's ColumnFamily and
     * the related DbIndex type and it belongs to which Keyspace.
     */
    public static class IndexAndCf implements Comparable {
        private ColumnFamily<String, IndexColumnName> cf;
        private Class<? extends DbIndex> indexType;
        private Keyspace keyspace;

        IndexAndCf(Class<? extends DbIndex> indexType,
                ColumnFamily<String, IndexColumnName> cf, Keyspace keyspace) {
            this.indexType = indexType;
            this.cf = cf;
            this.keyspace = keyspace;
        }

        @Override
        public String toString() {
            return generateKey();
        }

        String generateKey() {
            StringBuffer buffer = new StringBuffer();
            buffer.append(keyspace.getKeyspaceName()).append("/")
                    .append(indexType.getSimpleName()).append("/")
                    .append(cf.getName());
            return buffer.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof IndexAndCf)) {
                return false;
            }

            if (this == obj) {
                return true;
            }

            IndexAndCf that = (IndexAndCf) obj;
            if (cf != null ? !cf.equals(that.cf) : that.cf != null) {
                return false;
            }
            if (indexType != null ? !indexType.equals(that.indexType)
                    : that.indexType != null) {
                return false;
            }
            if (keyspace != null ? !keyspace.equals(that.keyspace)
                    : that.keyspace != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = cf != null ? cf.hashCode() : 0;
            result = 31 * result + (indexType != null ? indexType.hashCode() : 0);
            result = 31 * result + (keyspace != null ? keyspace.hashCode() : 0);
            return result;
        }

        @Override
        public int compareTo(Object object) {
            IndexAndCf other = (IndexAndCf) object;
            return this.generateKey().compareTo(other.generateKey());
        }
    }

    class IndexEntry {
        private String indexKey;
        private IndexColumnName columnName;

        public IndexEntry(String indexKey, IndexColumnName columnName) {
            this.indexKey = indexKey;
            this.columnName = columnName;
        }

        public String getIndexKey() {
            return indexKey;
        }

        public IndexColumnName getColumnName() {
            return columnName;
        }

    }

    private ObjectEntry extractObjectEntryFromIndex(String indexKey,
            IndexColumnName name, Class<? extends DbIndex> type, boolean toConsole) {
        // The className of a data object CF in a index record
        String className;
        // The id of the data object record in a index record
        String objectId;
        if (type.equals(AltIdDbIndex.class)) {
            objectId = name.getTwo();
            className = name.getOne();
        } else if (type.equals(RelationDbIndex.class)) {
            objectId = name.getTwo();
            className = name.getOne();
        } else if (type.equals(NamedRelationDbIndex.class)) {
            objectId = name.getFour();
            className = name.getOne();
        } else if (type.equals(DecommissionedDbIndex.class)) {
            objectId = name.getTwo();
            className = indexKey;
        } else if (type.equals(PermissionsDbIndex.class)) {
            objectId = name.getTwo();
            className = name.getOne();
        } else if (type.equals(PrefixDbIndex.class)) {
            objectId = name.getFour();
            className = name.getOne();
        } else if (type.equals(ScopedLabelDbIndex.class)) {
            objectId = name.getFour();
            className = name.getOne();
        } else if (type.equals(AggregateDbIndex.class)) {
            objectId = name.getTwo();
            int firstColon = indexKey.indexOf(':');
            className = firstColon == -1 ? indexKey : indexKey.substring(0, firstColon);
        } else {
            String msg = String.format("Unsupported index type %s.", type);
            logMessage(msg, false, toConsole);
            return null;
        }

        return new ObjectEntry(className, objectId);
    }

    public static class ObjectEntry {
        private String className;
        private String objectId;

        public ObjectEntry(String className, String objectId) {
            this.className = className;
            this.objectId = objectId;
        }

        public String getClassName() {
            return className;
        }

        public String getObjectId() {
            return objectId;
        }

        @Override
        public String toString() {
            StringBuffer buffer = new StringBuffer();
            buffer.append("ObjectEntry ClassName: ").append(className).append(" ObjectId: ").append(objectId);
            return buffer.toString();
        }
    }

    private boolean isColumnInIndex(Keyspace ks, ColumnFamily<String, IndexColumnName> indexCf, String indexKey, String[] indexColumns)
            throws ConnectionException {
        CompositeRangeBuilder builder = IndexColumnNameSerializer.get().buildRange();
        for (int i = 0; i < indexColumns.length; i++) {
            if (i == (indexColumns.length - 1)) {
                builder.greaterThanEquals(indexColumns[i]).lessThanEquals(indexColumns[i]).limit(1);
                break;
            }
            builder.withPrefix(indexColumns[i]);
        }

        ColumnList<IndexColumnName> result = ks.prepareQuery(indexCf).getKey(indexKey)
                .withColumnRange(builder)
                .execute().getResult();
        for (Column<IndexColumnName> indexColumn : result) {
            return true;
        }
        return false;
    }

    public static String getIndexKey(ColumnField field, Column<CompositeColumnName> column) {
        String indexKey = null;
        DbIndex dbIndex = field.getIndex();
        boolean indexByKey = field.isIndexByKey();
        if (dbIndex instanceof AltIdDbIndex) {
            indexKey = indexByKey ? column.getName().getTwo() : column.getStringValue();
        } else if (dbIndex instanceof RelationDbIndex) {
            indexKey = indexByKey ? column.getName().getTwo() : column.getStringValue();
        } else if (dbIndex instanceof NamedRelationDbIndex) {
            indexKey = NamedURI.fromString(column.getStringValue()).getURI().toString();
        } else if (dbIndex instanceof DecommissionedDbIndex) {
            indexKey = field.getDataObjectType().getSimpleName();
        } else if (dbIndex instanceof PermissionsDbIndex) {
            indexKey = column.getName().getTwo();
        } else if (dbIndex instanceof PrefixDbIndex) {
            indexKey = field.getPrefixIndexRowKey(column.getStringValue());
        } else if (dbIndex instanceof ScopedLabelDbIndex) {
            indexKey = field.getPrefixIndexRowKey(ScopedLabel.fromString(column.getStringValue()));
        } else if (dbIndex instanceof AggregateDbIndex) {
            // Not support this index type yet.
        } else {
            String msg = String.format("Unsupported index type %s.", dbIndex.getClass());
            _log.warn(msg);
        }

        return indexKey;
    }

    public static String[] getIndexColumns(ColumnField field, Column<CompositeColumnName> column, String rowKey) {
        String[] indexColumns = null;
        DbIndex dbIndex = field.getIndex();

        if (dbIndex instanceof AggregateDbIndex) {
            // Not support this index type yet.
            return indexColumns;
        }

        if (dbIndex instanceof NamedRelationDbIndex) {
            indexColumns = new String[4];
            indexColumns[0] = field.getDataObjectType().getSimpleName();
            NamedURI namedURI = NamedURI.fromString(column.getStringValue());
            String name = namedURI.getName();
            indexColumns[1] = name.toLowerCase();
            indexColumns[2] = name;
            indexColumns[3] = rowKey;

        } else if (dbIndex instanceof PrefixDbIndex) {
            indexColumns = new String[4];
            indexColumns[0] = field.getDataObjectType().getSimpleName();
            indexColumns[1] = column.getStringValue().toLowerCase();
            indexColumns[2] = column.getStringValue();
            indexColumns[3] = rowKey;

        } else if (dbIndex instanceof ScopedLabelDbIndex) {
            indexColumns = new String[4];
            indexColumns[0] = field.getDataObjectType().getSimpleName();
            ScopedLabel label = ScopedLabel.fromString(column.getStringValue());
            indexColumns[1] = label.getLabel().toLowerCase();
            indexColumns[2] = label.getLabel();
            indexColumns[3] = rowKey;

        } else if (dbIndex instanceof DecommissionedDbIndex) {
            indexColumns = new String[2];
            Boolean val = column.getBooleanValue();
            indexColumns[0] = val.toString();
            indexColumns[1] = rowKey;
        } else {
            // For AltIdDbIndex, RelationDbIndex, PermissionsDbIndex
            indexColumns = new String[2];
            indexColumns[0] = field.getDataObjectType().getSimpleName();
            indexColumns[1] = rowKey;
        }
        return indexColumns;
    }

    private String handleNullValue(String columnValue) {
        return columnValue == null ? "" : columnValue;
    }

    public DbClientImpl getDbClient() {
        return dbClient;
    }

    public void setDbClient(DbClientImpl dbClient) {
        this.dbClient = dbClient;
    }
    
    private boolean isValidDataObjectKey(URI uri, final Class<? extends DataObject> type) {
        if (uri == null) {
        	return false;
        }

        if (!URIUtil.isValid(uri) || !URIUtil.isType(uri, type)) {
            return false;
        }

        return true;
    }
}