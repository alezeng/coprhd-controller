/*
 * Copyright (c) 2017 DELL EMC
 * All Rights Reserved
 */
package com.emc.storageos.volumecontroller.impl.vmax.rest;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.emc.storageos.db.client.model.BlockConsistencyGroup.MigrationStatus;
import com.emc.storageos.db.client.model.Migration;
import com.emc.storageos.volumecontroller.JobContext;
import com.emc.storageos.volumecontroller.TaskCompleter;
import com.emc.storageos.volumecontroller.impl.block.taskcompleter.MigrationCommitTaskCompleter;

public class VMAXMigrationCommitJob extends VMAXMigrationJob {
    private static final Logger logger = LoggerFactory.getLogger(VMAXMigrationCommitJob.class);

    public VMAXMigrationCommitJob(URI migrationURI, String sourceSerialNumber, String targetSerialNumber, String sgName, String jobId, URI storageProviderURI,
            TaskCompleter taskCompleter) {
        super(migrationURI, sourceSerialNumber, targetSerialNumber, sgName, jobId, storageProviderURI, taskCompleter, "commitMigration");
    }

    @Override
    public void updateStatus(JobContext jobContext) throws Exception {
        try {
            if (isJobInTerminalState()) {
                Migration migration = jobContext.getDbClient().queryObject(Migration.class, migrationURI);
                // update migration end time
                long currentTime = System.currentTimeMillis();
                migration.setEndTime(String.valueOf(currentTime));
                jobContext.getDbClient().updateObject(migration);
                logger.info("Updated end time in migration instance");
                if (isJobInTerminalSuccessState()) {
                    ((MigrationCommitTaskCompleter) taskCompleter).setMigrationStatus(MigrationStatus.Migrated.name());
                } else {
                    ((MigrationCommitTaskCompleter) taskCompleter).setMigrationStatus(MigrationStatus.CommitFailed.name());
                }
            }
        } catch (Exception e) {
            logger.error("Exception occurred while updating the migration status", e);
        } finally {
            super.updateStatus(jobContext);
        }

    }
}
