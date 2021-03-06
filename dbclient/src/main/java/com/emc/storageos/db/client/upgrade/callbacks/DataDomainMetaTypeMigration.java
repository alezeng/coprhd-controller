/*
 * Copyright (c) 2017 EMC Corporation
 * All Rights Reserved
 */
package com.emc.storageos.db.client.upgrade.callbacks;

import java.net.URI;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.emc.storageos.db.client.DbClient;
import com.emc.storageos.db.client.model.StorageSystemType;
import com.emc.storageos.db.client.upgrade.BaseCustomMigrationCallback;
import com.emc.storageos.svcs.errorhandling.resources.MigrationCallbackException;

public class DataDomainMetaTypeMigration extends BaseCustomMigrationCallback {

    private static final Logger logger = LoggerFactory
            .getLogger(DataDomainMetaTypeMigration.class);

    private static final String DATA_DOMAIN = "datadomain";

    @Override
    public void process() throws MigrationCallbackException {
        logger.info("StorageSystemType MetaType for datadomain migration started");

        DbClient dbClient = getDbClient();
        try {
            List<URI> ids = dbClient.queryByType(StorageSystemType.class, true);
            Iterator<StorageSystemType> it = dbClient.queryIterativeObjects(StorageSystemType.class, ids);
            String metatype = StorageSystemType.META_TYPE.FILE.toString().toLowerCase();
            while (it.hasNext()) {
                StorageSystemType type = it.next();
                if (DATA_DOMAIN.equalsIgnoreCase(type.getStorageTypeName())) {
                    type.setMetaType(metatype);
                    type.setDriverClassName(metatype);
                    logger.info("Updating StorageSystemType MetaType for id : {}", type.getId());
                    dbClient.updateObject(type);
                    break;
                }
            }
            logger.info("StorageSystemType MetaType for datadomain migration completed successfully");
        } catch (Exception e) {
            logger.error("Exception occured while migrating datadomain StorageSystemType MetaType");
            logger.error(e.getMessage(), e);
        }

    }
}
