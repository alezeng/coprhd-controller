/*
 * Copyright (c) 2017 Dell EMC
 * All Rights Reserved
 */
package com.emc.sa.service.vmware.file.tasks;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;

import com.emc.sa.engine.ExecutionUtils;
import com.emc.sa.service.vipr.tasks.ViPRExecutionTask;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.iwave.ext.vmware.VCenterAPI;
import com.iwave.ext.vmware.VMwareUtils;
import com.vmware.vim25.mo.ClusterComputeResource;
import com.vmware.vim25.mo.Datastore;
import com.vmware.vim25.mo.HostSystem;

/**
 * This class will take the subtraction of the hosts in a cluster and the hosts that currently sharing the datastore.
 * If a host is added then hosts in cluster will have an extra entry that needs to be added in share list.
 * 
 * @author sanjes
 *
 */
public class GetHostsAddedToBeShared extends ViPRExecutionTask<List<HostSystem>> {
    @Inject
    private VCenterAPI vcenter;
    private ClusterComputeResource cluster;
    private Datastore datastore;

    public GetHostsAddedToBeShared(ClusterComputeResource cluster, Datastore datastore) {
        this.cluster = cluster;
        this.datastore = datastore;
        provideDetailArgs(cluster.getName(), datastore.getName());
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<HostSystem> executeTask() throws Exception {
        Map<String, HostSystem> clusterHosts = Maps.newHashMap();
        for (HostSystem clusterHost : cluster.getHosts()) {
            clusterHosts.put(VMwareUtils.getPath(clusterHost), clusterHost);
        }
        Map<String, HostSystem> datastoreHosts = Maps.newHashMap();
        for (HostSystem datastoreHost : VMwareUtils.getHostsForDatastore(vcenter, datastore)) {
            datastoreHosts.put(VMwareUtils.getPath(datastoreHost), datastoreHost);
        }
        List<String> hostNamesAdded = (List<String>) CollectionUtils.subtract(clusterHosts.keySet(), datastoreHosts.keySet());
        List<HostSystem> hostAdded = Lists.newArrayList();
        if (CollectionUtils.isEmpty(hostNamesAdded)) {
            ExecutionUtils.fail("failTask.getHostsAddedToBeShared.noHostsFoundToBeShared", datastore.getName());
        } else {
            for (String host : hostNamesAdded) {
                hostAdded.add(clusterHosts.get(host));
            }
        }
        return hostAdded;

    }

}