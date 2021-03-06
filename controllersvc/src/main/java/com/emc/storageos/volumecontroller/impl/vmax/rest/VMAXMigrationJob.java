/*
 * Copyright (c) 2017 DELL EMC
 * All Rights Reserved
 */
package com.emc.storageos.volumecontroller.impl.vmax.rest;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.emc.storageos.db.client.model.StorageProvider;
import com.emc.storageos.vmax.restapi.VMAXApiClient;
import com.emc.storageos.vmax.restapi.model.response.migration.MigrationStorageGroupResponse;
import com.emc.storageos.volumecontroller.JobContext;
import com.emc.storageos.volumecontroller.TaskCompleter;
import com.emc.storageos.volumecontroller.impl.block.taskcompleter.MigrationOperationTaskCompleter;
import com.emc.storageos.volumecontroller.impl.vmax.VMAXUtils;

public class VMAXMigrationJob extends VMAXJob {
    /**
     * 
     */
    private static final long serialVersionUID = -3214255718263558929L;
    private static final Logger logger = LoggerFactory.getLogger(VMAXMigrationJob.class);
    URI migrationURI;
    String sourceSerialNumber;
    String targetSerialNumber;
    String sgName;

    public VMAXMigrationJob(URI migrationURI, String sourceSerialNumber, String targetSerialNumber, String sgName, String jobId, URI storageProviderURI,
            TaskCompleter taskCompleter, String jobName) {
        super(jobId, storageProviderURI, taskCompleter, jobName);
        this.migrationURI = migrationURI;
        this.sourceSerialNumber = sourceSerialNumber;
        this.targetSerialNumber = targetSerialNumber;
        this.sgName = sgName;
    }

    @Override
    public void updateStatus(JobContext jobContext) throws Exception {
        StorageProvider provider = null;
        try {
            provider = jobContext.getDbClient().queryObject(StorageProvider.class, getStorageProviderURI());
            logger.info("VMAXJob: Looking up job: id {}, provider: {} ", getJobId(), provider.getIPAddress());
            VMAXApiClient vmaxApiClient = jobContext.getVmaxClientFactory().getClient(provider.getIPAddress(), provider.getPortNumber(),
                    provider.getUseSSL(), provider.getUserName(), provider.getPassword());
            MigrationStorageGroupResponse sgResponse = vmaxApiClient.getMigrationStorageGroup(sourceSerialNumber, targetSerialNumber, sgName);
            String migrationStatus = sgResponse.getState();
            logger.info("Migration status {}", migrationStatus);
            VMAXUtils.updatePercentageDone(migrationURI, jobContext.getDbClient(), sgResponse);
            ((MigrationOperationTaskCompleter) getTaskCompleter()).setMigrationStatus(migrationStatus);
        } catch (Exception e) {
            logger.error("Exception occurred", e);
        } finally {
            super.updateStatus(jobContext);
        }
    }
}
