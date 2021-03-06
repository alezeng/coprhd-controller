/*
 * Copyright (c) 2012-2015 iWave Software LLC
 * All Rights Reserved
 */
package com.emc.sa.service.vipr.block;

import static com.emc.sa.service.ServiceParams.CONSISTENCY_GROUP;
import static com.emc.sa.service.ServiceParams.NAME;
import static com.emc.sa.service.ServiceParams.NUMBER_OF_VOLUMES;
import static com.emc.sa.service.ServiceParams.PORT_GROUP;
import static com.emc.sa.service.ServiceParams.PROJECT;
import static com.emc.sa.service.ServiceParams.RDF_GROUP;
import static com.emc.sa.service.ServiceParams.REMOTE_REPLICATION_GROUP;
import static com.emc.sa.service.ServiceParams.REMOTE_REPLICATION_MODE;
import static com.emc.sa.service.ServiceParams.REMOTE_REPLICATION_SET;
import static com.emc.sa.service.ServiceParams.SIZE_IN_GB;
import static com.emc.sa.service.ServiceParams.VIRTUAL_ARRAY;
import static com.emc.sa.service.ServiceParams.VIRTUAL_POOL;
import static com.emc.sa.service.vipr.ViPRExecutionUtils.logInfo;

import java.net.URI;
import java.util.List;

import com.emc.sa.engine.bind.Param;

public class CreateBlockVolumeHelper {
    @Param(VIRTUAL_POOL)
    protected URI virtualPool;

    @Param(VIRTUAL_ARRAY)
    protected URI virtualArray;

    @Param(PROJECT)
    protected URI project;

    @Param(NAME)
    protected String nameParam;

    @Param(SIZE_IN_GB)
    protected Double sizeInGb;

    @Param(value = NUMBER_OF_VOLUMES, required = false)
    protected Integer count;

    @Param(value = CONSISTENCY_GROUP, required = false)
    protected URI consistencyGroup;
    
    @Param(value = RDF_GROUP, required = false)
    protected URI rdfGroup;

    @Param(value = PORT_GROUP, required = false)
    protected URI portGroup;

    @Param(value = REMOTE_REPLICATION_SET, required = false)
    public URI remoteReplicationSet;
    
    @Param(value = REMOTE_REPLICATION_MODE, required = false)
    public String remoteReplicationMode;

    @Param(value = REMOTE_REPLICATION_GROUP, required = false)
    public URI remoteReplicationGroup;

    public List<URI> createVolumes(URI computeResource) {
        List<URI> volumeIds = BlockStorageUtils.createVolumes(project, virtualArray, virtualPool, nameParam,
                sizeInGb, count, consistencyGroup, computeResource, portGroup, rdfGroup);
        for (URI volumeId : volumeIds) {
            logInfo("create.block.volume.create.volume", volumeId);
        }
        return volumeIds;
    }

    public String getName() {
        return this.nameParam;
    }

    public URI getProject() {
        return this.project;
    }

    public URI getVirtualArray() {
        return this.virtualArray;
    }

    public URI getVirtualPool() {
        return this.virtualPool;
    }

    public Double getSizeInGb() {
        return this.sizeInGb;
    }

    public URI getConsistencyGroup() {
        return this.consistencyGroup;
    }

    public URI getRemoteReplicationSet() {
        return this.remoteReplicationSet;
    }

    public String getRemoteReplicationMode() {
        return this.remoteReplicationMode;
    }

    public URI getRemoteReplicationGroup() {
        return this.remoteReplicationGroup;
    }

    public Integer getCount() {
        return this.count;
    }
    
    public URI getComputeResource(){
    	return null;
    }
    
    public URI getRdfGroup() {
        return this.rdfGroup;
    }
    
    public URI getPortGroup() {
        return portGroup;
    }
}
