/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.vipr.client.core;

import static com.emc.vipr.client.core.util.ResourceUtils.defaultList;

import java.net.URI;
import java.util.List;

import javax.ws.rs.core.UriBuilder;

import com.emc.storageos.model.BulkIdParam;
import com.emc.storageos.model.NamedRelatedResourceRep;
import com.emc.storageos.model.SnapshotList;
import com.emc.storageos.model.TaskList;
import com.emc.storageos.model.block.BlockConsistencyGroupBulkRep;
import com.emc.storageos.model.block.BlockConsistencyGroupCreate;
import com.emc.storageos.model.block.BlockConsistencyGroupRestRep;
import com.emc.storageos.model.block.BlockConsistencyGroupSnapshotCreate;
import com.emc.storageos.model.block.BlockConsistencyGroupUpdate;
import com.emc.storageos.model.block.BlockSnapshotSessionList;
import com.emc.storageos.model.block.CopiesParam;
import com.emc.storageos.model.block.MigrationCreateParam;
import com.emc.storageos.model.block.MigrationList;
import com.emc.storageos.model.block.MigrationZoneCreateParam;
import com.emc.storageos.model.block.NamedVolumesList;
import com.emc.storageos.model.block.SnapshotSessionCreateParam;
import com.emc.storageos.model.block.VolumeDeleteTypeEnum;
import com.emc.storageos.model.block.VolumeFullCopyCreateParam;
import com.emc.storageos.model.host.HostRestRep;
import com.emc.vipr.client.Task;
import com.emc.vipr.client.Tasks;
import com.emc.vipr.client.ViPRCoreClient;
import com.emc.vipr.client.core.impl.PathConstants;
import com.emc.vipr.client.impl.RestClient;

/**
 * Block Consistency Group resources.
 * <p>
 * Base URL: <tt>/block/consistency-groups</tt>
 *
 * @see BlockConsistencyGroupRestRep
 */
public class BlockConsistencyGroups extends ProjectResources<BlockConsistencyGroupRestRep> implements
        TaskResources<BlockConsistencyGroupRestRep> {
    public BlockConsistencyGroups(ViPRCoreClient parent, RestClient client) {
        super(parent, client, BlockConsistencyGroupRestRep.class, PathConstants.BLOCK_CONSISTENCY_GROUP_URL);
    }

    @Override
    public BlockConsistencyGroups withInactive(boolean inactive) {
        return (BlockConsistencyGroups) super.withInactive(inactive);
    }

    @Override
    public BlockConsistencyGroups withInternal(boolean internal) {
        return (BlockConsistencyGroups) super.withInternal(internal);
    }

    @Override
    protected List<BlockConsistencyGroupRestRep> getBulkResources(BulkIdParam input) {
        BlockConsistencyGroupBulkRep response = client.post(BlockConsistencyGroupBulkRep.class, input, getBulkUrl());
        return defaultList(response.getConsistencyGroups());
    }

    @Override
    public Tasks<BlockConsistencyGroupRestRep> getTasks(URI id) {
        return doGetTasks(id);
    }

    @Override
    public Task<BlockConsistencyGroupRestRep> getTask(URI id, URI taskId) {
        return doGetTask(id, taskId);
    }

    /**
     * Begins creating a full copy of the given block volume.
     * <p>
     * API Call: <tt>POST /block/consistency-groups/{id}/protection/full-copies</tt>
     *
     * @param consistencyGroupId
     *            the ID of the consistency group.
     * @param input
     *            the create configuration.
     * @return tasks for monitoring the progress of the operation(s).
     */
    public Tasks<BlockConsistencyGroupRestRep> createFullCopy(URI consistencyGroupId, VolumeFullCopyCreateParam input) {
        final String url = getIdUrl() + "/protection/full-copies";
        return postTasks(input, url, consistencyGroupId);
    }

    /**
     * List full copies for a consistency group
     * <p>
     * API Call: <tt>GET /block/consistency-groups/{id}/protection/full-copies</tt>
     *
     * @param consistencyGroupId
     *            the ID of the consistency group
     * @return The list of full copies for the consistency group
     */
    public List<NamedRelatedResourceRep> getFullCopies(URI consistencyGroupId) {
        final String url = getIdUrl() + "/protection/full-copies";
        NamedVolumesList response = client.get(NamedVolumesList.class, url, consistencyGroupId);
        return defaultList(response.getVolumes());
    }

    /**
     * Activate consistency group full copy
     * <p>
     * API Call: <tt>POST /block/consistency-groups/{id}/protection/full-copies/{fcid}/activate</tt>
     *
     * @param consistencyGroupId
     *            the ID of the consistency group.
     * @param fullCopyId
     *            the Id of the full copy
     * @return An asychronous operation realized as a <code>Tasks</code> object
     */
    public Tasks<BlockConsistencyGroupRestRep> activateFullCopy(URI consistencyGroupId, URI fullCopyId) {
        final String url = getIdUrl() + "/protection/full-copies/{fcid}/activate";
        return postTasks(url, consistencyGroupId, fullCopyId);
    }

    /**
     * Detach consistency group full copy
     * <p>
     * API Call: <tt>POST /block/consistency-groups/{id}/protection/full-copies/{fcid}/detach</tt>
     *
     * @param consistencyGroupId
     *            the ID of the consistency group.
     * @param fullCopyId
     *            the Id of the full copy
     * @return An asychronous operation realized as a <code>Tasks</code> object
     */
    public Tasks<BlockConsistencyGroupRestRep> detachFullCopy(URI consistencyGroupId, URI fullCopyId) {
        final String url = getIdUrl() + "/protection/full-copies/{fcid}/detach";
        return postTasks(url, consistencyGroupId, fullCopyId);
    }

    /**
     * Restore consistency group full copy
     * <p>
     * API Call: <tt>POST /block/consistency-groups/{id}/protection/full-copies/{fcid}/restore</tt>
     *
     * @param consistencyGroupId
     *            the ID of the consistency group.
     * @param fullCopyId
     *            the Id of the full copy
     * @return An asychronous operation realized as a <code>Tasks</code> object
     */
    public Tasks<BlockConsistencyGroupRestRep> restoreFullCopy(URI consistencyGroupId, URI fullCopyId) {
        final String url = getIdUrl() + "/protection/full-copies/{fcid}/restore";
        return postTasks(url, consistencyGroupId, fullCopyId);
    }

    /**
     * Resynchronize consistency group full copy
     * <p>
     * API Call: <tt>POST /block/consistency-groups/{id}/protection/full-copies/{fcid}/resynchronize</tt>
     *
     * @param consistencyGroupId
     *            the ID of the consistency group.
     * @param fullCopyId
     *            the Id of the full copy
     * @return An asychronous operation realized as a <code>Tasks</code> object
     */
    public Tasks<BlockConsistencyGroupRestRep> resynchronizeFullCopy(URI consistencyGroupId, URI fullCopyId) {
        final String url = getIdUrl() + "/protection/full-copies/{fcid}/resynchronize";
        return postTasks(url, consistencyGroupId, fullCopyId);
    }

    /**
     * Deactivate consistency group full copy
     * <p>
     * API Call: <tt>POST /block/consistency-groups/{id}/protection/full-copies/{fcid}/deactivate</tt>
     *
     * @param consistencyGroupId
     *            the ID of the consistency group.
     * @param fullCopyId
     *            the Id of the full copy
     * @return An asychronous operation realized as a <code>Tasks</code> object
     */
    public Tasks<BlockConsistencyGroupRestRep> deactivateFullCopy(URI consistencyGroupId, URI fullCopyId) {
        final String url = getIdUrl() + "/protection/full-copies/{fcid}/deactivate";
        return postTasks(url, consistencyGroupId, fullCopyId);
    }

    /**
     * List snapshots in the consistency group
     * <p>
     * API Call: <tt>GET /block/consistency-groups/{id}/protection/snapshots</tt>
     *
     * @param consistencyGroupId
     *            the ID of the consistency group
     * @return The list of snapshots in the consistency group
     */
    public List<NamedRelatedResourceRep> getSnapshots(URI consistencyGroupId) {
        final String url = getIdUrl() + "/protection/snapshots";
        SnapshotList response = client.get(SnapshotList.class, url, consistencyGroupId);
        return response.getSnapList();
    }

    /**
     * Create consistency group snapshot
     * <p>
     * API Call: <tt>POST /block/consistency-groups/{id}/protection/snapshots</tt>
     *
     * @param consistencyGroupId
     *            the ID of the consistency group
     * @param input
     *            the create snapshot specification
     * @return An asychronous operation realized as a <code>Tasks</code> object
     */
    public Tasks<BlockConsistencyGroupRestRep> createSnapshot(URI consistencyGroupId, BlockConsistencyGroupSnapshotCreate input) {
        final String url = getIdUrl() + "/protection/snapshots";
        return postTasks(input, url, consistencyGroupId);
    }

    /**
     * Activate consistency group snapshot
     * <p>
     * API Call: <tt>POST /block/consistency-groups/{id}/protection/snapshots/{sid}/activate</tt>
     *
     * @param consistencyGroupId
     *            the ID of the consistency group
     * @param snapshotId
     *            the ID of the snapshot
     * @return An asychronous operation realized as a <code>Task</code> object
     */
    public Task<BlockConsistencyGroupRestRep> activateSnapshot(URI consistencyGroupId, URI snapshotId) {
        final String url = getIdUrl() + "/protection/snapshots/{fcid}/activate";
        return postTask(url, consistencyGroupId, snapshotId);
    }

    /**
     * Deactivate consistency group snapshot
     * <p>
     * API Call: <tt>POST /block/consistency-groups/{id}/protection/snapshots/{sid}/deactivate</tt>
     *
     * @param consistencyGroupId
     *            the ID of the consistency group
     * @param snapshotId
     *            the ID of the snapshot
     * @return An asychronous operation realized as a <code>Tasks</code> object
     */
    public Tasks<BlockConsistencyGroupRestRep> deactivateSnapshot(URI consistencyGroupId, URI snapshotId) {
        final String url = getIdUrl() + "/protection/snapshots/{fcid}/deactivate";
        return postTasks(url, consistencyGroupId, snapshotId);
    }

    /**
     * Restore consistency group snapshot
     * <p>
     * API Call: <tt>POST /block/consistency-groups/{id}/protection/snapshots/{sid}/restore</tt>
     *
     * @param consistencyGroupId
     *            the ID of the consistency group
     * @param snapshotId
     *            the ID of the snapshot
     * @return An asychronous operation realized as a <code>Task</code> object
     */
    public Task<BlockConsistencyGroupRestRep> restoreSnapshot(URI consistencyGroupId, URI snapshotId) {
        final String url = getIdUrl() + "/protection/snapshots/{fcid}/restore";
        return postTask(url, consistencyGroupId, snapshotId);
    }

    /**
     * Resynchronize consistency group snapshot
     * <p>
     * API Call: <tt>POST /block/consistency-groups/{id}/protection/snapshot/{fcid}/resynchronize</tt>
     *
     * @param consistencyGroupId
     *            the ID of the consistency group.
     * @param snapshotId
     *            the Id of the snapshot
     * @return An asychronous operation realized as a <code>Tasks</code> object
     */
    public Task<BlockConsistencyGroupRestRep> resynchronizeSnapshot(URI consistencyGroupId, URI snapshotId) {
        final String url = getIdUrl() + "/protection/snapshots/{fcid}/resynchronize";
        return postTask(url, consistencyGroupId, snapshotId);
    }

    /**
     * Begins initiating failover for a given consistency group.
     * <p>
     * API Call: <tt>POST /block/consistency-groups/{id}/protection/continuous-copies/failover</tt>
     *
     * @param consistencyGroupId
     *            the ID of the consistency group.
     * @param input
     *            the input configuration.
     * @return a task for monitoring the progress of the operation.
     */
    public Tasks<BlockConsistencyGroupRestRep> failover(URI consistencyGroupId, CopiesParam input) {
        final String url = getIdUrl() + "/protection/continuous-copies/failover";
        return postTasks(input, url, consistencyGroupId);
    }

    /**
     * Begins initiating failover cancel for a given consistency group.
     * <p>
     * API Call: <tt>POST /block/consistency-groups/{id}/protection/continuous-copies/failover-cancel</tt>
     *
     * @param consistencyGroupId
     *            the ID of the consistency group.
     * @param input
     *            the input configuration.
     * @return a task for monitoring the progress of the operation.
     */
    public Tasks<BlockConsistencyGroupRestRep> failoverCancel(URI consistencyGroupId, CopiesParam input) {
        final String url = getIdUrl() + "/protection/continuous-copies/failover-cancel";
        return postTasks(input, url, consistencyGroupId);
    }

    /**
     * Begins updating the access mode for a given consistency group copy.
     * <p>
     * API Call: <tt>POST /block/consistency-groups/{id}/protection/continuous-copies/accessmode</tt>
     *
     * @param consistencyGroupId
     *            the ID of the consistency group.
     * @param input
     *            the input configuration.
     * @return a task for monitoring the progress of the operation.
     */
    public Tasks<BlockConsistencyGroupRestRep> updateCopyAccessMode(URI consistencyGroupId, CopiesParam input) {
        final String url = getIdUrl() + "/protection/continuous-copies/accessmode";
        return postTasks(input, url, consistencyGroupId);
    }

    /**
     * Begins initiating swap for a given consistency group.
     * <p>
     * API Call: <tt>POST /block/consistency-groups/{id}/protection/continuous-copies/swap</tt>
     *
     * @param consistencyGroupId
     *            the ID of the consistency group.
     * @param input
     *            the input configuration.
     * @return a task for monitoring the progress of the operation.
     */
    public Tasks<BlockConsistencyGroupRestRep> swap(URI consistencyGroupId, CopiesParam input) {
        final String url = getIdUrl() + "/protection/continuous-copies/swap";
        return postTasks(input, url, consistencyGroupId);
    }

    /**
     * Creates a block consistency group.
     * <p>
     * API Call: <tt>POST /block/consistency-groups</tt>
     *
     * @param input
     *            the create configuration.
     * @return the created block consistency group.
     */
    public BlockConsistencyGroupRestRep create(BlockConsistencyGroupCreate input) {
        return client.post(BlockConsistencyGroupRestRep.class, input, baseUrl);
    }

    /**
     * Begins updating a block consistency group.
     * <p>
     * API Call: <tt>PUT /block/consistency-groups/{id}</tt>
     *
     * @param id
     *            the ID of the block consistency group to update.
     * @param input
     *            the update configuration.
     * @return a task for monitoring the progress of the update operation.
     */
    public Task<BlockConsistencyGroupRestRep> update(URI id, BlockConsistencyGroupUpdate input) {
        return putTask(input, getIdUrl(), id);
    }

    /**
     * Begins deactivating a block consistency group.
     * <p>
     * API Call: <tt>POST /block/consistency-groups/{id}/deactivate</tt>
     *
     * @param id
     *            the ID of the block consistency group to deactivate.
     * @return a task for monitoring the progres of the deactivate operation.
     *
     * @see #doDeactivateWithTask(URI)
     */
    public Task<BlockConsistencyGroupRestRep> deactivate(URI id) {
        return deactivate(id, VolumeDeleteTypeEnum.FULL);
    }

    /**
     * Begins deactivating a block consistency group.
     * <p>
     * API Call: <tt>POST /block/consistency-groups/{id}/deactivate?type={deletionType}</tt>
     *
     * @param id
     *            The ID of the block consistency group to deactivate.
     * @param deletionType
     *            {@code FULL} or {@code VIPR_ONLY}
     *
     * @return
     */
    public Task<BlockConsistencyGroupRestRep> deactivate(URI id, VolumeDeleteTypeEnum deletionType) {
        URI uri = client.uriBuilder(getIdUrl() + "/deactivate").queryParam("type", deletionType).build(id);
        return postTaskURI(uri);
    }

    /**
     * List snapshot sessions in the consistency group
     * <p>
     * API Call: <tt>GET /block/consistency-groups/{id}/protection/snapshot-sessions</tt>
     *
     * @param consistencyGroupId
     *            the ID of the consistency group
     * @return The list of snapshot sessions in the consistency group
     */
    public List<NamedRelatedResourceRep> getSnapshotSessions(URI consistencyGroupId) {
        final String url = getIdUrl() + "/protection/snapshot-sessions";
        BlockSnapshotSessionList response = client.get(BlockSnapshotSessionList.class, url, consistencyGroupId);
        return response.getSnapSessionRelatedResourceList();
    }

    /**
     * Create consistency group snapshot session
     * <p>
     * API Call: <tt>POST /block/consistency-groups/{id}/protection/snapshot-sessions</tt>
     *
     * @param consistencyGroupId
     *            the ID of the consistency group
     * @param input
     *            the create snapshot session specification
     * @return An asychronous operation realized as a <code>Tasks</code> object
     */
    public Tasks<BlockConsistencyGroupRestRep> createSnapshotSession(URI consistencyGroupId, SnapshotSessionCreateParam input) {
        final String url = getIdUrl() + "/protection/snapshot-sessions";
        return postTasks(input, url, consistencyGroupId);
    }

    /**
     * Deactivate consistency group snapshot session
     * <p>
     * API Call: <tt>POST /block/consistency-groups/{id}/protection/snapshot-sessions/{sid}/deactivate</tt>
     *
     * @param consistencyGroupId
     *            the ID of the consistency group
     * @param snapshotSessionId
     *            the ID of the snapshot session
     * @return An asychronous operation realized as a <code>Tasks</code> object
     */
    public Tasks<BlockConsistencyGroupRestRep> deactivateSnapshotSession(URI consistencyGroupId, URI snapshotSessionId) {
        final String url = getIdUrl() + "/protection/snapshot-sessions/{fcid}/deactivate";
        return postTasks(url, consistencyGroupId, snapshotSessionId);
    }

    /**
     * Restore consistency group snapshot session
     * <p>
     * API Call: <tt>POST /block/consistency-groups/{id}/protection/snapshot-sessions/{sid}/restore</tt>
     *
     * @param consistencyGroupId
     *            the ID of the consistency group
     * @param snapshotSessionId
     *            the ID of the snapshot session
     * @return An asychronous operation realized as a <code>Task</code> object
     */
    public Task<BlockConsistencyGroupRestRep> restoreSnapshotSession(URI consistencyGroupId, URI snapshotSessionId) {
        final String url = getIdUrl() + "/protection/snapshot-sessions/{fcid}/restore";
        return postTask(url, consistencyGroupId, snapshotSessionId);
    }

    /**
     * Create consistency group migration
     * <p>
     * API Call: <tt>POST /block/consistency-groups/{id}/migration/create</tt>
     *
     * @param consistencyGroupId
     *            the ID of the consistency group
     * @param input
     *            the create migration specification
     * @return a task for monitoring the progress of the operation.
     */
    public Task<BlockConsistencyGroupRestRep> migrationCreate(URI consistencyGroupId, MigrationCreateParam input) {
        final String url = getIdUrl() + "/migration/create";
        return postTask(input, url, consistencyGroupId);
    }

    /**
     * Cutover the consistency group migration
     * <p>
     * API Call: <tt>POST /block/consistency-groups/{id}/migration/cutover</tt>
     *
     * @param consistencyGroupId
     *            the ID of the consistency group
     * @return a task for monitoring the progress of the operation.
     */
    public Task<BlockConsistencyGroupRestRep> migrationCutover(URI consistencyGroupId) {
        final String url = getIdUrl() + "/migration/cutover";
        return postTask(url, consistencyGroupId);
    }

    /**
     * Commit the consistency group migration
     * <p>
     * API Call: <tt>POST /block/consistency-groups/{id}/migration/commit</tt>
     *
     * @param consistencyGroupId
     *            the ID of the consistency group
     * @return a task for monitoring the progress of the operation.
     */
    public Task<BlockConsistencyGroupRestRep> migrationCommit(URI consistencyGroupId) {
        final String url = getIdUrl() + "/migration/commit";
        return postTask(url, consistencyGroupId);
    }

    /**
     * Cancel the consistency group migration
     * <p>
     * API Call: <tt>POST /block/consistency-groups/{id}/migration/cancel</tt>
     *
     * @param consistencyGroupId
     *            the ID of the consistency group
     * @param revert
     *            pass true if cancel with revert operation
     * @return a task for monitoring the progress of the operation.
     */
    public Task<BlockConsistencyGroupRestRep> migrationCancel(URI consistencyGroupId, boolean revert) {

        UriBuilder builder = client.uriBuilder(getIdUrl() + "/migration/cancel");
        if (revert) {
            builder = builder.queryParam("revert", revert);
        }
        return postTaskURI(builder.build(consistencyGroupId));
    }

    /**
     * Refresh the consistency group migration
     * <p>
     * API Call: <tt>POST /block/consistency-groups/{id}/migration/refresh</tt>
     *
     * @param consistencyGroupId
     *            the ID of the consistency group
     * @return a task for monitoring the progress of the operation.
     */
    public Task<BlockConsistencyGroupRestRep> migrationRefresh(URI consistencyGroupId) {
        final String url = getIdUrl() + "/migration/refresh";
        return postTask(url, consistencyGroupId);
    }

    /**
     * Recover the consistency group migration
     * <p>
     * API Call: <tt>POST /block/consistency-groups/{id}/migration/recover</tt>
     *
     * @param consistencyGroupId
     *            the ID of the consistency group
     * @param force
     *            the force flag
     * @return a task for monitoring the progress of the operation.
     */
    public Task<BlockConsistencyGroupRestRep> migrationRecover(URI consistencyGroupId, boolean force) {
        UriBuilder builder = client.uriBuilder(getIdUrl() + "/migration/recover");
        if (force) {
            builder = builder.queryParam("force", force);
        }
        return postTaskURI(builder.build(consistencyGroupId));
    }

    /**
     * Sync-stop the consistency group migration
     * <p>
     * API Call: <tt>POST /block/consistency-groups/{id}/migration/sync-stop</tt>
     *
     * @param consistencyGroupId
     *            the ID of the consistency group
     * @return a task for monitoring the progress of the operation.
     */
    public Task<BlockConsistencyGroupRestRep> migrationSyncStop(URI consistencyGroupId) {
        final String url = getIdUrl() + "/migration/sync-stop";
        return postTask(url, consistencyGroupId);
    }

    /**
     * Sync-start the consistency group migration
     * <p>
     * API Call: <tt>POST /block/consistency-groups/{id}/migration/sync-start</tt>
     *
     * @param consistencyGroupId
     *            the ID of the consistency group
     * @return a task for monitoring the progress of the operation.
     */
    public Task<BlockConsistencyGroupRestRep> migrationSyncStart(URI consistencyGroupId) {
        final String url = getIdUrl() + "/migration/sync-start";
        return postTask(url, consistencyGroupId);
    }

    /**
     * Create zones for the initiators in the consistency group
     * <p>
     * API Call: <tt>POST /block/consistency-groups/{id}/migration/create-zones</tt>
     *
     * @param consistencyGroupId
     *            the ID of the consistency group
     * @param input
     *            zone create configuration
     * @return tasks for monitoring the progress of the operation.
     */
    public Tasks<BlockConsistencyGroupRestRep> createZonesForMigration(URI consistencyGroupId, MigrationZoneCreateParam input) {
        final String url = getIdUrl() + "/migration/create-zones";
        return postTasks(input, url, consistencyGroupId);
    }

    /**
     * Rescan the hosts in the consistency group
     * <p>
     * API Call: <tt>POST /block/consistency-groups/{id}/migration/rescan-hosts</tt>
     *
     * @param consistencyGroupId
     *            the ID of the consistency group
     * @return tasks for monitoring the progress of the operation.
     */
    public Tasks<HostRestRep> rescanHostsForMigration(URI consistencyGroupId) {
        final String url = getIdUrl() + "/migration/rescan-hosts";
        TaskList tasks = client.post(TaskList.class, url, consistencyGroupId);
        return new Tasks<HostRestRep>(client, tasks.getTaskList(), HostRestRep.class);
    }

    /**
     * Gets the migrations for a consistency group
     * <p>
     * API Call: <tt>POST /block/consistency-groups/{id}/migrations</tt>
     *
     * @param consistencyGroupId
     *            the ID of the consistency group
     * @return the migrations.
     */
    public MigrationList getConsistencyGroupMigrations(URI consistencyGroupId) {
        return client.get(MigrationList.class, getIdUrl() + "/migrations", consistencyGroupId);
    }

}
