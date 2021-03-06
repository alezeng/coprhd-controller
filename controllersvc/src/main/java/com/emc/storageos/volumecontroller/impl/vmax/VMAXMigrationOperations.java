/*
 * Copyright (c) 2017 Dell EMC
 * All Rights Reserved
 */
package com.emc.storageos.volumecontroller.impl.vmax;

import java.net.URI;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.emc.storageos.db.client.model.BlockConsistencyGroup;
import com.emc.storageos.db.client.model.BlockConsistencyGroup.MigrationStatus;
import com.emc.storageos.db.client.model.Migration;
import com.emc.storageos.db.client.model.StoragePool;
import com.emc.storageos.db.client.model.StorageProvider;
import com.emc.storageos.db.client.model.StorageSystem;
import com.emc.storageos.db.client.util.NullColumnValueGetter;
import com.emc.storageos.exceptions.DeviceControllerErrors;
import com.emc.storageos.exceptions.DeviceControllerException;
import com.emc.storageos.svcs.errorhandling.model.ServiceError;
import com.emc.storageos.vmax.restapi.VMAXApiClient;
import com.emc.storageos.vmax.restapi.model.AsyncJob;
import com.emc.storageos.vmax.restapi.model.response.migration.MigrationStorageGroupResponse;
import com.emc.storageos.vmax.restapi.model.response.provisioning.StorageGroupVolumeListResponse;
import com.emc.storageos.volumecontroller.ControllerException;
import com.emc.storageos.volumecontroller.MigrationOperations;
import com.emc.storageos.volumecontroller.TaskCompleter;
import com.emc.storageos.volumecontroller.impl.ControllerServiceImpl;
import com.emc.storageos.volumecontroller.impl.block.taskcompleter.MigrationCommitTaskCompleter;
import com.emc.storageos.volumecontroller.impl.block.taskcompleter.MigrationOperationTaskCompleter;
import com.emc.storageos.volumecontroller.impl.job.QueueJob;
import com.emc.storageos.volumecontroller.impl.vmax.rest.VMAXCreateMigrationJob;
import com.emc.storageos.volumecontroller.impl.vmax.rest.VMAXMigrationCommitJob;
import com.emc.storageos.volumecontroller.impl.vmax.rest.VMAXMigrationJob;

public class VMAXMigrationOperations extends VMAXOperations implements MigrationOperations {
    private static final Logger logger = LoggerFactory.getLogger(VMAXMigrationOperations.class);
    private static final String MIGRATION_ENV_EXISTS = "The requested migration environment resource already exists";
    private static final String MIGRATION_ENV_NON_EXIST = "A problem occurred deleting the migration environment resources: The migration session environment is not configured";
    private static final String COMMIT_STORAGE_GROUP_NOT_FOUND_ON_SOURCE_ARRAY = "Storage Group [%s] on Symmetrix [%s] cannot be found";

    @Override
    public void createMigrationEnvironment(StorageSystem sourceSystem, StorageSystem targetSystem, TaskCompleter taskCompleter)
            throws DeviceControllerException {
        logger.info(VMAXConstants.CREATE_MIGRATION_ENV + " started. Source: {}, Target: {}",
                sourceSystem.getSerialNumber(), targetSystem.getSerialNumber());
        try {
            VMAXApiClient apiClient = VMAXUtils.getApiClient(VMAXUtils.getRestProvider(sourceSystem, targetSystem, dbClient),
                    vmaxClientFactory);
            try {
                apiClient.createMigrationEnvironment(sourceSystem.getSerialNumber(), targetSystem.getSerialNumber());
            } catch (Exception e) {
                // ignore if migration environment already exists
                if (!e.getMessage().contains(MIGRATION_ENV_EXISTS)) {
                    throw e;
                }
            }

            taskCompleter.ready(dbClient);
        } catch (Exception e) {
            logger.error(VMAXConstants.CREATE_MIGRATION_ENV + " failed", e);
            ServiceError error = DeviceControllerErrors.vmax.methodFailed(VMAXConstants.CREATE_MIGRATION_ENV, e);
            taskCompleter.error(dbClient, error);
        }
        logger.info(VMAXConstants.CREATE_MIGRATION_ENV + " finished");
    }

    @Override
    public void removeMigrationEnvironment(StorageSystem sourceSystem, StorageSystem targetSystem, TaskCompleter taskCompleter)
            throws DeviceControllerException {
        logger.info(VMAXConstants.REMOVE_MIGRATION_ENV + " started. Source: {}, Target: {}",
                sourceSystem.getSerialNumber(), targetSystem.getSerialNumber());
        try {
            VMAXApiClient apiClient = VMAXUtils.getApiClient(VMAXUtils.getRestProvider(sourceSystem, targetSystem, dbClient),
                    vmaxClientFactory);
            try {
                apiClient.deleteMigrationEnvironment(sourceSystem.getSerialNumber(), targetSystem.getSerialNumber());
            } catch (Exception e) {
                // ignore if migration environment hasn't been configured yet
                if (!e.getMessage().contains(MIGRATION_ENV_NON_EXIST)) {
                    throw e;
                }
            }

            taskCompleter.ready(dbClient);
        } catch (Exception e) {
            logger.error(VMAXConstants.REMOVE_MIGRATION_ENV + " failed", e);
            ServiceError error = DeviceControllerErrors.vmax.methodFailed(VMAXConstants.REMOVE_MIGRATION_ENV, e);
            taskCompleter.error(dbClient, error);
        }
        logger.info(VMAXConstants.REMOVE_MIGRATION_ENV + " finished");
    }

    @Override
    public void createMigration(StorageSystem sourceSystem, URI cgURI, URI migrationURI, URI targetSystemURI,
            URI srp, Boolean enableCompression, TaskCompleter taskCompleter) throws ControllerException {
        logger.info(VMAXConstants.CREATE_MIGRATION + " started");
        try {
            StorageSystem targetSystem = dbClient.queryObject(StorageSystem.class, targetSystemURI);
            Migration migration = dbClient.queryObject(Migration.class, migrationURI);
            BlockConsistencyGroup cg = dbClient.queryObject(BlockConsistencyGroup.class, cgURI);
            String sgName = cg.getStorageGroupName();
            logger.info("Source: {}, Target: {}, Storage Group: {}",
                    sourceSystem.getSerialNumber(), targetSystem.getSerialNumber(), sgName);

            boolean noCompression = !enableCompression;
            String srpName = null;
            if (!NullColumnValueGetter.isNullURI(srp)) {
                StoragePool srpPool = dbClient.queryObject(StoragePool.class, srp);
                srpName = srpPool.getPoolName();
            }

            // update migration start time
            long currentTime = System.currentTimeMillis();
            migration.setStartTime(String.valueOf(currentTime));
            dbClient.updateObject(migration);

            StorageProvider restProvider = VMAXUtils.getRestProvider(sourceSystem, targetSystem, dbClient);
            VMAXApiClient apiClient = VMAXUtils.getApiClient(restProvider, vmaxClientFactory);
            MigrationStorageGroupResponse sgResponse = null;
            try {
                sgResponse = apiClient.getMigrationStorageGroup(sourceSystem.getSerialNumber(), targetSystem.getSerialNumber(), sgName);
                // If get migration/SG url works, that means migration had already been initiated. Update status and return as NO-OP.
                if (sgResponse != null) {
                    logger.info("Migration already initiated. Status: {}", sgResponse.getState());
                    ((MigrationOperationTaskCompleter) taskCompleter).setMigrationStatus(sgResponse.getState());
                    taskCompleter.ready(dbClient);
                    return;
                }
            } catch (Exception ex) {
                logger.info("Migration for Storage Group {} is not initiated yet.", sgName);
            }

            AsyncJob asyncJob = apiClient.createMigration(sourceSystem.getSerialNumber(), targetSystem.getSerialNumber(), sgName,
                    noCompression, srpName);
            VMAXCreateMigrationJob vmaxMigrationJob = new VMAXCreateMigrationJob(migrationURI, sourceSystem.getSerialNumber(), targetSystem.getSerialNumber(), sgName,
                    asyncJob.getJobId(), restProvider.getId(), taskCompleter);
            ControllerServiceImpl.enqueueJob(new QueueJob(vmaxMigrationJob));
        } catch (Exception e) {
            logger.error(VMAXConstants.CREATE_MIGRATION + " failed", e);
            ServiceError error = DeviceControllerErrors.vmax.methodFailed(VMAXConstants.CREATE_MIGRATION, e);
            taskCompleter.error(dbClient, error);
        }
        logger.info(VMAXConstants.CREATE_MIGRATION + " finished");
    }

    @Override
    public void cutoverMigration(StorageSystem sourceSystem, URI cgURI, URI migrationURI, TaskCompleter taskCompleter)
            throws ControllerException {
        logger.info(VMAXConstants.CUTOVER_MIGRATION + " started");
        try {
            Migration migration = dbClient.queryObject(Migration.class, migrationURI);
            StorageSystem targetSystem = dbClient.queryObject(StorageSystem.class, migration.getTargetSystem());
            BlockConsistencyGroup cg = dbClient.queryObject(BlockConsistencyGroup.class, cgURI);
            String sgName = cg.getStorageGroupName();
            logger.info("Source: {}, Target: {}, Storage Group: {}",
                    sourceSystem.getSerialNumber(), targetSystem.getSerialNumber(), sgName);

            StorageProvider restProvider = VMAXUtils.getRestProvider(sourceSystem, targetSystem, dbClient);
            VMAXApiClient apiClient = VMAXUtils.getApiClient(restProvider, vmaxClientFactory);
            MigrationStorageGroupResponse sgResponse = apiClient.getMigrationStorageGroup(sourceSystem.getSerialNumber(), targetSystem.getSerialNumber(), sgName);
            String migrationStatus = sgResponse.getState();
            if (!MigrationStatus.isAlreadyCutoverIntiated(migrationStatus)) {
                AsyncJob asyncJob = apiClient.cutoverMigration(sourceSystem.getSerialNumber(), sgName);
                VMAXMigrationJob vmaxMigrationJob = new VMAXMigrationJob(migrationURI, sourceSystem.getSerialNumber(), targetSystem.getSerialNumber(), sgName,
                        asyncJob.getJobId(), restProvider.getId(), taskCompleter, "cutoverMigration");
                ControllerServiceImpl.enqueueJob(new QueueJob(vmaxMigrationJob));
            } else {
                logger.info(
                        "Cutover step is already executed for this Storage Group {}. Hence no need to execute cutover operation one more time.",
                        sgName);
                ((MigrationOperationTaskCompleter) taskCompleter).setMigrationStatus(migrationStatus);
                taskCompleter.ready(dbClient);
            }
        } catch (Exception e) {
            logger.error(VMAXConstants.CUTOVER_MIGRATION + " failed", e);
            ServiceError error = DeviceControllerErrors.vmax.methodFailed(VMAXConstants.CUTOVER_MIGRATION, e);
            taskCompleter.error(dbClient, error);
        }
        logger.info(VMAXConstants.CUTOVER_MIGRATION + " finished");
    }

    @Override
    public void commitMigration(StorageSystem sourceSystem, URI cgURI, URI migrationURI, TaskCompleter taskCompleter)
            throws ControllerException {
        logger.info(VMAXConstants.COMMIT_MIGRATION + " started");
        String sgName = null;
        try {
            Migration migration = dbClient.queryObject(Migration.class, migrationURI);
            StorageSystem targetSystem = dbClient.queryObject(StorageSystem.class, migration.getTargetSystem());
            BlockConsistencyGroup cg = dbClient.queryObject(BlockConsistencyGroup.class, cgURI);
            sgName = cg.getStorageGroupName();
            logger.info("Source: {}, Target: {}, Storage Group: {}",
                    sourceSystem.getSerialNumber(), targetSystem.getSerialNumber(), sgName);

            StorageProvider restProvider = VMAXUtils.getRestProvider(sourceSystem, targetSystem, dbClient);
            VMAXApiClient apiClient = VMAXUtils.getApiClient(restProvider, vmaxClientFactory);
            // validate the SG status for this operation
            MigrationStorageGroupResponse sgResponse = apiClient.getMigrationStorageGroup(sourceSystem.getSerialNumber(), targetSystem.getSerialNumber(), sgName);

            // Post Commit, remove the vmware datastore names and mountpoints tags for the volumes involved in migration.
            // Query the volume ids for this SG.
            StorageGroupVolumeListResponse volumesList = apiClient.getStorageGroupVolumes(sourceSystem.getSerialNumber(), sgName);
            List<String> volumeIds = VMAXUtils.getStorageGroupVolumes(volumesList);
            ((MigrationCommitTaskCompleter) taskCompleter).setVolumeIds(volumeIds);

            try {
                AsyncJob asyncJob = apiClient.commitMigration(sourceSystem.getSerialNumber(), sgName);
                VMAXMigrationCommitJob vmaxCommitJob = new VMAXMigrationCommitJob(migrationURI, sourceSystem.getSerialNumber(), targetSystem.getSerialNumber(), sgName,
                        asyncJob.getJobId(), restProvider.getId(), taskCompleter);
                ControllerServiceImpl.enqueueJob(new QueueJob(vmaxCommitJob));
            } catch (Exception e) {
                // TODO OPT #526046 .Ignore the SG on source array not found error.
                // If the SG had DeleteWhenUnassociated flag set during its creation, we will get SG not found error after commit
                String msg = String.format(COMMIT_STORAGE_GROUP_NOT_FOUND_ON_SOURCE_ARRAY, sgName, sourceSystem.getSerialNumber());
                if (!e.getMessage().contains(msg)) {
                    throw e;
                }
            }
        } catch (Exception e) {
            logger.error(VMAXConstants.COMMIT_MIGRATION + " failed", e);
            ServiceError error = DeviceControllerErrors.vmax.methodFailed(VMAXConstants.COMMIT_MIGRATION, e);
            taskCompleter.error(dbClient, error);
        }
        logger.info(VMAXConstants.COMMIT_MIGRATION + " finished");
    }

    @Override
    public void cancelMigration(StorageSystem sourceSystem, URI cgURI, URI migrationURI, boolean cancelWithRevert,
            TaskCompleter taskCompleter)
            throws ControllerException {
        logger.info(VMAXConstants.CANCEL_MIGRATION + " started");
        try {
            Migration migration = dbClient.queryObject(Migration.class, migrationURI);
            StorageSystem targetSystem = dbClient.queryObject(StorageSystem.class, migration.getTargetSystem());
            BlockConsistencyGroup cg = dbClient.queryObject(BlockConsistencyGroup.class, cgURI);
            String sgName = cg.getStorageGroupName();
            logger.info("Source: {}, Target: {}, Storage Group: {}",
                    sourceSystem.getSerialNumber(), targetSystem.getSerialNumber(), sgName);

            VMAXApiClient apiClient = VMAXUtils.getApiClient(VMAXUtils.getRestProvider(sourceSystem, targetSystem, dbClient),
                    vmaxClientFactory);
            // validate the SG status for this operation
            MigrationStorageGroupResponse sgResponse = apiClient.getMigrationStorageGroup(sourceSystem.getSerialNumber(), targetSystem.getSerialNumber(), sgName);
            logger.info("Current migration state : {}", sgResponse.getState());
            if (cancelWithRevert) {
                apiClient.cancelMigrationWithRevert(sourceSystem.getSerialNumber(), sgName);
            } else {
                apiClient.cancelMigration(sourceSystem.getSerialNumber(), sgName);
            }

            // update migration status as Cancelled
            ((MigrationOperationTaskCompleter) taskCompleter).setMigrationStatus(MigrationStatus.Cancelled.name());
            taskCompleter.ready(dbClient);
        } catch (Exception e) {
            logger.error(VMAXConstants.CANCEL_MIGRATION + " failed", e);
            ServiceError error = DeviceControllerErrors.vmax.methodFailed(VMAXConstants.CANCEL_MIGRATION, e);
            taskCompleter.error(dbClient, error);
        }
        logger.info(VMAXConstants.CANCEL_MIGRATION + " finished");
    }

    @Override
    public void refreshMigration(StorageSystem sourceSystem, URI cgURI, URI migrationURI, TaskCompleter taskCompleter)
            throws ControllerException {
        logger.info(VMAXConstants.REFRESH_MIGRATION + " started");
        try {
            Migration migration = dbClient.queryObject(Migration.class, migrationURI);
            StorageSystem targetSystem = dbClient.queryObject(StorageSystem.class, migration.getTargetSystem());
            BlockConsistencyGroup cg = dbClient.queryObject(BlockConsistencyGroup.class, cgURI);
            String sgName = cg.getStorageGroupName();
            logger.info("Source: {}, Target: {}, Storage Group: {}",
                    sourceSystem.getSerialNumber(), targetSystem.getSerialNumber(), sgName);

            VMAXApiClient apiClient = VMAXUtils.getApiClient(VMAXUtils.getRestProvider(sourceSystem, targetSystem, dbClient),
                    vmaxClientFactory);
            MigrationStorageGroupResponse sgResponse = apiClient.getMigrationStorageGroup(sourceSystem.getSerialNumber(), targetSystem.getSerialNumber(), sgName);

            String migrationStatus = sgResponse.getState();
            VMAXUtils.updatePercentageDone(migrationURI, dbClient, sgResponse);
            ((MigrationOperationTaskCompleter) taskCompleter).setMigrationStatus(migrationStatus);
            taskCompleter.ready(dbClient);
        } catch (Exception e) {
            logger.error(VMAXConstants.REFRESH_MIGRATION + " failed", e);
            ServiceError error = DeviceControllerErrors.vmax.methodFailed(VMAXConstants.REFRESH_MIGRATION, e);
            taskCompleter.error(dbClient, error);
        }
        logger.info(VMAXConstants.REFRESH_MIGRATION + " finished");
    }

    @Override
    public void recoverMigration(StorageSystem sourceSystem, URI cgURI, URI migrationURI, boolean force, TaskCompleter taskCompleter)
            throws ControllerException {
        logger.info(VMAXConstants.RECOVER_MIGRATION + " started");
        try {
            Migration migration = dbClient.queryObject(Migration.class, migrationURI);
            StorageSystem targetSystem = dbClient.queryObject(StorageSystem.class, migration.getTargetSystem());
            BlockConsistencyGroup cg = dbClient.queryObject(BlockConsistencyGroup.class, cgURI);
            String sgName = cg.getStorageGroupName();
            logger.info("Source: {}, Target: {}, Storage Group: {}",
                    sourceSystem.getSerialNumber(), targetSystem.getSerialNumber(), sgName);

            StorageProvider restProvider = VMAXUtils.getRestProvider(sourceSystem, targetSystem, dbClient);
            VMAXApiClient apiClient = VMAXUtils.getApiClient(restProvider, vmaxClientFactory);
            // validate the SG status for this operation
            MigrationStorageGroupResponse sgResponse = apiClient.getMigrationStorageGroup(sourceSystem.getSerialNumber(), targetSystem.getSerialNumber(), sgName);

            AsyncJob asyncJob = apiClient.recoverMigration(sourceSystem.getSerialNumber(), sgName, force);

            VMAXMigrationJob vmaxMigrationJob = new VMAXMigrationJob(migrationURI, sourceSystem.getSerialNumber(), targetSystem.getSerialNumber(), sgName,
                    asyncJob.getJobId(), restProvider.getId(), taskCompleter, "recoverMigration");
            ControllerServiceImpl.enqueueJob(new QueueJob(vmaxMigrationJob));

        } catch (Exception e) {
            logger.error(VMAXConstants.RECOVER_MIGRATION + " failed", e);
            ServiceError error = DeviceControllerErrors.vmax.methodFailed(VMAXConstants.RECOVER_MIGRATION, e);
            taskCompleter.error(dbClient, error);
        }
        logger.info(VMAXConstants.RECOVER_MIGRATION + " finished");
    }

    @Override
    public void syncStopMigration(StorageSystem sourceSystem, URI cgURI, URI migrationURI, TaskCompleter taskCompleter)
            throws ControllerException {
        logger.info(VMAXConstants.SYNCSTOP_MIGRATION + " started");
        try {
            Migration migration = dbClient.queryObject(Migration.class, migrationURI);
            StorageSystem targetSystem = dbClient.queryObject(StorageSystem.class, migration.getTargetSystem());
            BlockConsistencyGroup cg = dbClient.queryObject(BlockConsistencyGroup.class, cgURI);
            String sgName = cg.getStorageGroupName();
            logger.info("Source: {}, Target: {}, Storage Group: {}",
                    sourceSystem.getSerialNumber(), targetSystem.getSerialNumber(), sgName);

            StorageProvider restProvider = VMAXUtils.getRestProvider(sourceSystem, targetSystem, dbClient);
            VMAXApiClient apiClient = VMAXUtils.getApiClient(restProvider, vmaxClientFactory);
            // validate the SG status for this operation
            MigrationStorageGroupResponse sgResponse = apiClient.getMigrationStorageGroup(sourceSystem.getSerialNumber(), targetSystem.getSerialNumber(), sgName);

            AsyncJob asyncJob = apiClient.stopMigrationSync(sourceSystem.getSerialNumber(), sgName);

            VMAXMigrationJob vmaxMigrationJob = new VMAXMigrationJob(migrationURI, sourceSystem.getSerialNumber(), targetSystem.getSerialNumber(), sgName,
                    asyncJob.getJobId(), restProvider.getId(), taskCompleter, "syncStopMigration");
            ControllerServiceImpl.enqueueJob(new QueueJob(vmaxMigrationJob));
        } catch (Exception e) {
            logger.error(VMAXConstants.SYNCSTOP_MIGRATION + " failed", e);
            ServiceError error = DeviceControllerErrors.vmax.methodFailed(VMAXConstants.SYNCSTOP_MIGRATION, e);
            taskCompleter.error(dbClient, error);
        }
        logger.info(VMAXConstants.SYNCSTOP_MIGRATION + " finished");
    }

    @Override
    public void syncStartMigration(StorageSystem sourceSystem, URI cgURI, URI migrationURI, TaskCompleter taskCompleter)
            throws ControllerException {
        logger.info(VMAXConstants.SYNCSTART_MIGRATION + " started");
        try {
            Migration migration = dbClient.queryObject(Migration.class, migrationURI);
            StorageSystem targetSystem = dbClient.queryObject(StorageSystem.class, migration.getTargetSystem());
            BlockConsistencyGroup cg = dbClient.queryObject(BlockConsistencyGroup.class, cgURI);
            String sgName = cg.getStorageGroupName();
            logger.info("Source: {}, Target: {}, Storage Group: {}",
                    sourceSystem.getSerialNumber(), targetSystem.getSerialNumber(), sgName);

            StorageProvider restProvider = VMAXUtils.getRestProvider(sourceSystem, targetSystem, dbClient);
            VMAXApiClient apiClient = VMAXUtils.getApiClient(restProvider, vmaxClientFactory);
            // validate the SG status for this operation
            MigrationStorageGroupResponse sgResponse = apiClient.getMigrationStorageGroup(sourceSystem.getSerialNumber(), targetSystem.getSerialNumber(), sgName);

            AsyncJob asyncJob = apiClient.startMigrationSync(sourceSystem.getSerialNumber(), sgName);

            VMAXMigrationJob vmaxMigrationJob = new VMAXMigrationJob(migrationURI, sourceSystem.getSerialNumber(), targetSystem.getSerialNumber(), sgName,
                    asyncJob.getJobId(), restProvider.getId(), taskCompleter, "syncStartMigration");
            ControllerServiceImpl.enqueueJob(new QueueJob(vmaxMigrationJob));
        } catch (Exception e) {
            logger.error(VMAXConstants.SYNCSTART_MIGRATION + " failed", e);
            ServiceError error = DeviceControllerErrors.vmax.methodFailed(VMAXConstants.SYNCSTART_MIGRATION, e);
            taskCompleter.error(dbClient, error);
        }
        logger.info(VMAXConstants.SYNCSTART_MIGRATION + " finished");
    }
}
