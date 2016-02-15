/*
 * Copyright (c) 2012-2015 iWave Software LLC
 * All Rights Reserved
 */
package com.emc.sa.service.vipr.plugins.tasks;

import java.net.URI;
import java.net.URISyntaxException;




//import com.emc.sa.engine.ExecutionTask;
import com.emc.sa.engine.service.ExternalTaskApdapterInterface;
import com.emc.storageos.vasa.async.TaskInfo;

public class CustomSample implements ExternalTaskApdapterInterface {
//public class CustomSample extends ExecutionTask<Object> {
    private URI vpoolId;
    private URI varrayId;
    private URI projectId;
    private String size;
    private Integer count;
    private String name;
    private URI consistencyGroupId;

    public CustomSample(){
    	
    }
    public CustomSample(String vpoolId, String varrayId, String projectId, String size, Integer count,
            String name, String consistencyGroupId) throws URISyntaxException {
    	this(new URI(vpoolId), new URI(varrayId), new URI(projectId), size, count, name, new URI(consistencyGroupId));
        
    }

    public CustomSample(URI vpoolId, URI varrayId, URI projectId, String size, Integer count, String name,
            URI consistencyGroupId) {
        this.vpoolId = vpoolId;
        this.varrayId = varrayId;
        this.projectId = projectId;
        this.size = size;
        this.count = count;
        this.name = name;
        this.consistencyGroupId = consistencyGroupId;
    }

 	@Override
	public void init() throws Exception {
		System.out.println("Custom Task init");
		
	}
	@Override
	public void precheck() throws Exception {
		System.out.println("Custom Task precheck");
		
	}
	@Override
	public void preLuanch() throws Exception {
		System.out.println("Custom Task preLuanch");
		
	}
	@Override
	public TaskInfo execute() throws Exception {
		TaskInfo taskInfo= new TaskInfo();
		System.out.println("Custom Task execute" + name+size +vpoolId+ varrayId +count+consistencyGroupId+projectId);
		taskInfo.setProgress(100);
		taskInfo.setTaskState("SUCCESS");
		
		taskInfo.setResult("Extrenal Task Completed");
		return taskInfo;

		
	}
	@Override
	public void postcheck() throws Exception {
		System.out.println("Custom Task postcheck");
		
	}
	@Override
	public void postLuanch() throws Exception {
		System.out.println("Custom Task postLuanch");
		
	}
	@Override
	public void getStatus() throws Exception {
		System.out.println("Custom Task getStatus");
		
	}
	@Override
	public void destroy() {
		System.out.println("Custom Task destroy");
		
	}
}
