/*
 * Copyright (c) 2012-2015 iWave Software LLC
 * All Rights Reserved
 */
package com.emc.sa.service.linux.file;

import java.net.URI;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.emc.sa.engine.ExecutionUtils;
import com.emc.sa.service.linux.LinuxUtils;
import com.emc.sa.service.linux.tasks.AddToFSTab;
import com.emc.sa.service.linux.tasks.CreateDirectory;
import com.emc.sa.service.linux.tasks.DeleteDirectory;
import com.emc.sa.service.linux.tasks.GetDirectoryContents;
import com.emc.sa.service.linux.tasks.LinuxExecutionTask;
import com.emc.sa.service.linux.tasks.ListMountPoints;
import com.emc.sa.service.linux.tasks.MountPath;
import com.emc.sa.service.linux.tasks.RemoveFromFSTab;
import com.emc.sa.service.linux.tasks.UnmountPath;
import com.emc.sa.service.linux.tasks.VerifyMountPoint;
import com.emc.sa.service.vipr.ViPRExecutionUtils;
import com.emc.sa.service.vipr.file.tasks.GetFileSystemTags;
import com.emc.sa.service.vipr.file.tasks.RemoveFileSystemTag;
import com.emc.sa.service.vipr.file.tasks.SetFileSystemTag;
import com.emc.storageos.model.block.BlockObjectRestRep;
import com.iwave.ext.linux.LinuxSystemCLI;
import com.iwave.ext.linux.model.MountPoint;

public class LinuxFileSupport {

    private final LinuxSystemCLI targetSystem;

    public LinuxFileSupport(LinuxSystemCLI targetSystem) {
        this.targetSystem = targetSystem;
    }

    public String getHostName() {
        return targetSystem.getHost();
    }

    protected void logDebug(String message, Object... args) {
        ExecutionUtils.currentContext().logDebug(message, args);
    }

    protected void logInfo(String message, Object... args) {
        ExecutionUtils.currentContext().logInfo(message, args);
    }

    protected void logWarn(String message, Object... args) {
        ExecutionUtils.currentContext().logWarn(message, args);
    }

    protected void logError(String message, Object... args) {
        ExecutionUtils.currentContext().logError(message, args);
    }

    protected void logError(Throwable cause, String message, Object... args) {
        ExecutionUtils.currentContext().logError(message, args);
    }

    public void verifyMountPoint(String path) {
        execute(new VerifyMountPoint(path));
    }

    public MountPoint findMountPoint(BlockObjectRestRep volume) {
        return LinuxUtils.getMountPoint(targetSystem.getHostId(), getMountPoints(), volume);
    }

    public Map<String, MountPoint> getMountPoints() {
        return execute(new ListMountPoints());
    }

    public void createDirectory(String path) {
        execute(new CreateDirectory(path));
        addRollback(new DeleteDirectory(path));
    }

    public void deleteDirectory(String path) {
        execute(new DeleteDirectory(path));
    }

    public boolean isDirectoryEmpty(String path) {
        return execute(new GetDirectoryContents(path)).isEmpty();
    }

    public void addToFSTab(String device, String path, String fsType, String options) {
        execute(new AddToFSTab(device, path, fsType, StringUtils.defaultIfEmpty(options, AddToFSTab.DEFAULT_OPTIONS)));
        addRollback(new RemoveFromFSTab(path));
    }

    public void removeFromFSTab(String path) {
        execute(new RemoveFromFSTab(path));
    }

    public void mountPath(String path) {
        execute(new MountPath(path));
        addRollback(new UnmountPath(path));
    }

    public void unmountPath(String path) {
        execute(new UnmountPath(path));
    }

    public Set<String> getFSTags(String fileSystemId) {
        return ExecutionUtils.execute(new GetFileSystemTags(fileSystemId));
    }

    public void setFSTag(String fileSystemId, String tag) {
        ExecutionUtils.execute(new SetFileSystemTag(fileSystemId, tag));
        ExecutionUtils.addRollback(new RemoveFileSystemTag(fileSystemId, tag));
    }

    public void removeFSTag(URI fileSystemId, String removeTag) {
        ExecutionUtils.execute(new RemoveFileSystemTag(fileSystemId, removeTag));
        ExecutionUtils.addRollback(new SetFileSystemTag(fileSystemId, removeTag));
    }

    protected <T> T execute(LinuxExecutionTask<T> task) {
        task.setTargetSystem(targetSystem);

        return ViPRExecutionUtils.execute(task);
    }

    public void addRollback(LinuxExecutionTask<?> rollbackTask) {
        rollbackTask.setTargetSystem(targetSystem);

        ExecutionUtils.addRollback(rollbackTask);
    }
}
