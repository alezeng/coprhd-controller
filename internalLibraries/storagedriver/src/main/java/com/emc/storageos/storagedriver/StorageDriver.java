/*
 * Copyright (c) 2016 EMC Corporation
 * All Rights Reserved
 */
package com.emc.storageos.storagedriver;

import java.util.List;

import org.apache.commons.lang.mutable.MutableInt;

import com.emc.storageos.storagedriver.model.StorageObject;
import com.emc.storageos.storagedriver.model.StorageVolume;

public interface StorageDriver {


    public static final String SDK_VERSION_NUMBER = "3.2.0.0";
    public static final int DEFAULT_OBJECT_IDS_COUNT = 100;

    /**
     *  Get driver registration data.
     */
    public RegistrationData getRegistrationData();

    /**
     * Return driver task with a given id.
     *
     * @param taskId
     * @return
     */
    public DriverTask getTask(String taskId);

    /**
     * Get storage object with a given type with specified native ID which belongs to specified storage system
     *
     * @param storageSystemId storage system native id
     * @param objectId object native id
     * @param type  class instance
     * @param <T> storage object type
     * @return storage object or null if does not exist
     *
     * Example of usage:
     *    StorageVolume volume = StorageDriver.getStorageObject("vmax-12345", "volume-1234", StorageVolume.class);
     */
    public <T extends StorageObject> T getStorageObject(String storageSystemId, String objectId, Class<T> type);

    /**
     * Get list of storage objects with a given type with specified native IDs which belongs to specified storage system
     *
     * @param storageSystemId storage system native id
     * @param objectIds object native ids. Currently the maximum number of objectsIDs that will be supplied in a single call is 100
     *            (DEFAULT_OBJECT_IDS_COUNT)
     * @param type class instance
     * @param token used for paging. Input 0 indicates that the first page should be returned. Output 0 indicates
     *            that last page was returned. Type: Input/Output.
     * @param <T> storage object type
     * @return corresponding storage objects list or empty list if non are discovered
     *
     *         Example of usage:
     *         List<StorageVolume> volumes = StorageDriver.getStorageObjects("vmax-12345", {"volume-1234","volume-5678"},
     *         StorageVolume.class, new
     *         MutableInt(0));
     */
    public <T extends StorageObject> List<T> getStorageObjects(String storageSystemId, List<String> objectIds, Class<T> type,
            MutableInt token);
}


