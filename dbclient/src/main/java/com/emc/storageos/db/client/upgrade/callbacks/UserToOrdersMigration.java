/*
 * Copyright (c) 2016 EMC Corporation
 * All Rights Reserved
 */
package com.emc.storageos.db.client.upgrade.callbacks;

import java.net.URI;

import com.emc.storageos.db.client.upgrade.InternalDbClient;
import com.netflix.astyanax.connectionpool.OperationResult;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.model.Rows;
import com.netflix.astyanax.serializers.StringSerializer;
import com.netflix.astyanax.util.RangeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.query.RowQuery;

import com.emc.storageos.db.client.constraint.impl.AlternateIdConstraintImpl;
import com.emc.storageos.db.client.constraint.impl.QueryHitIterator;
import com.emc.storageos.db.client.model.uimodels.Order;
import com.emc.storageos.db.client.upgrade.BaseCustomMigrationCallback;
import com.emc.storageos.db.client.impl.*;

import com.emc.storageos.svcs.errorhandling.resources.MigrationCallbackException;

public class UserToOrdersMigration extends BaseCustomMigrationCallback {
    private static final Logger log = LoggerFactory.getLogger(UserToOrdersMigration.class);

    InternalDbClient client;

    public UserToOrdersMigration(InternalDbClient dbclient) {
        client = dbclient;
    }

    @Override
    public void process() throws MigrationCallbackException {
        long start = System.currentTimeMillis();

        log.info("lbybb0");

        Keyspace ks = client.getLocalKeyspace();
        ColumnFamily<String, IndexColumnName> userToOrders =
                new ColumnFamily<String, IndexColumnName>("UserToOrders", StringSerializer.get(), IndexColumnNameSerializer.get());

        DataObjectType doType = TypeMap.getDoType(Order.class);
        ColumnField field = doType.getColumnField(Order.SUBMITTED_BY_USER_ID);
        ColumnFamily<String, ClassNameTimeSeriesIndexColumnName> newCf = field.getIndexCF();
        MutationBatch mutationBatch = ks.prepareMutationBatch();
        try {
            long n = 0;
            long m = 0;

            OperationResult<Rows<String, IndexColumnName>> result = ks.prepareQuery(userToOrders).getAllRows()
                    .setRowLimit(100)
                    .withColumnRange(new RangeBuilder().setLimit(0).build())
                    .execute();

            ColumnList<IndexColumnName> cols;
            for (Row<String, IndexColumnName> row : result.getResult()) {
                n++;
                RowQuery<String, IndexColumnName> rowQuery = ks.prepareQuery(userToOrders).getKey(row.getKey())
                        .autoPaginate(true)
                        .withColumnRange(new RangeBuilder().setLimit(5).build());

                while (!(cols = rowQuery.execute().getResult()).isEmpty()) {
                    m++;
                    for (Column<IndexColumnName> col : cols) {
                        String indexKey = row.getKey();
                        String orderId = col.getName().getTwo();
                        log.info("lbyd11: tid={} order={}", indexKey, orderId);

                        ClassNameTimeSeriesIndexColumnName newCol = new ClassNameTimeSeriesIndexColumnName(col.getName().getOne(), orderId,
                                col.getName().getTimeUUID());
                        mutationBatch.withRow(newCf, indexKey).putEmptyColumn(newCol, null);
                        if ( m % 10000 == 0) {
                            log.info("lbyd22 commit m={}", m);
                            mutationBatch.execute();
                        }
                    }
                }
            }
            mutationBatch.execute();
            long end = System.currentTimeMillis();
            log.info("Read5 "+n+" : "+ (end - start)/1000);
        }catch (Exception e) {
            log.error("lbyy0: e=", e);
        }
    }
}
