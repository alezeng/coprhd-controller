/*
 * Copyright (c) 2017 Dell EMC
 * All Rights Reserved
 */
package com.emc.storageos.vmax.restapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.emc.storageos.services.util.EnvConfig;
import com.emc.storageos.vmax.restapi.errorhandling.VMAXException;
import com.emc.storageos.vmax.restapi.model.AsyncJob;
import com.emc.storageos.vmax.restapi.model.response.migration.CreateMigrationEnvironmentResponse;
import com.emc.storageos.vmax.restapi.model.response.migration.MigrationEnvironmentListResponse;
import com.emc.storageos.vmax.restapi.model.response.migration.MigrationEnvironmentResponse;
import com.emc.storageos.vmax.restapi.model.response.migration.MigrationStorageGroupListResponse;
import com.emc.storageos.vmax.restapi.model.response.migration.MigrationStorageGroupResponse;

/**
 * 
 * Update unisphereIp, userName and password attributes before executing this test class
 *
 */
public class VMAXApiClientTest {
    private static Logger log = LoggerFactory.getLogger(VMAXApiClientTest.class);
    private static String SG_MIGRATION = "AlexNDM_SG_8";
    private static String SOURCE_SG_NON_MIGRATION = "AlexNDM_SG_1";
    private static String NON_EXISTING_SG = "NON_EXISTING_DG";
    private static String SG_CANNOT_FOUND = "Storage Group [%s] on Symmetrix [%s] cannot be found";

    private static VMAXApiClient apiClient;
    private static final String unisphereIp = EnvConfig.get("sanity", "vmax.host");
    private static String userName = EnvConfig.get("sanity", "vmax.username");
    private static String password = EnvConfig.get("sanity", "vmax.password");
    private static int portNumber = 8443;

    private static final String sourceArraySerialNumber = "000195702161";
    private static final String targetArraySerialNumber = "000196800794";
    private static final String SG_NAME = "test_mig_161";
    private static final String version = "8.4.0.10";
    private static final Set<String> localSystems = new HashSet<>(Arrays.asList("000196701343", "000196801612", "000197000197",
            "000197000143", "000196800794", "000196801468"));

    @BeforeClass
    public static void setup() throws Exception {
        VMAXApiClientFactory apiClientFactory = VMAXApiClientFactory.getInstance();
        apiClientFactory.setConnectionTimeoutMs(30000);
        apiClientFactory.setConnManagerTimeout(60000);
        apiClientFactory.setMaxConnections(300);
        apiClientFactory.setMaxConnectionsPerHost(100);
        apiClientFactory.setNeedCertificateManager(true);
        apiClientFactory.setSocketConnectionTimeoutMs(3600000);

        apiClientFactory.init();
        apiClient = apiClientFactory.getClient(unisphereIp, portNumber, true, userName, password);
        assertNotNull("Api Client object is null", apiClient);
    }

    @Test
    public void getApiVersionTest() throws Exception {
        assertEquals(version, apiClient.getApiVersion());
    }

    @Test
    public void getLocalSystemsTest() throws Exception {
        assertEquals(localSystems, apiClient.getLocalSystems());
    }

    @Test
    public void craeteMigrationEnvironmentTest() throws Exception {
        assertNotNull("Api Client object is null", apiClient);
        CreateMigrationEnvironmentResponse response = apiClient.createMigrationEnvironment(sourceArraySerialNumber,
                targetArraySerialNumber);
        assertNotNull("Response object is null", response);
        assertEquals("ArrayId is not correct", targetArraySerialNumber, response.getArrayId());
        assertFalse("Invalid status", !response.isLocal());
        assertEquals("Invalid Migration session count", 1, response.getMigrationSessionCount());
    }

    @Test
    public void getMigrationEnvironmentTest() throws Exception {
        assertNotNull("Api Client object is null", apiClient);

        MigrationEnvironmentResponse response = apiClient.getMigrationEnvironment(sourceArraySerialNumber, targetArraySerialNumber);
        assertEquals("Not the correct state", "OK", response.getState());
        assertEquals("Not the correct symm id", sourceArraySerialNumber, response.getSymmetrixId());
        assertEquals("Not the correct other symm id", targetArraySerialNumber, response.getOtherSymmetrixId());
        assertFalse("Not the correct status", response.isInvalid());
        // fail("Not yet implemented");
    }

    @Test(expected = VMAXException.class)
    public void getMigrationEnvironmentNegativeTest() throws Exception {
        assertNotNull("Api Client object is null", apiClient);
        MigrationEnvironmentResponse response = apiClient.getMigrationEnvironment("xyz", "abc");
    }

    @Test
    public void getMigrationEnvironmentListTest() throws Exception {
        assertNotNull("Api Client object is null", apiClient);
        MigrationEnvironmentListResponse response = apiClient.getMigrationEnvironmentList(sourceArraySerialNumber);
        assertNotNull("Response object is null", response);
        assertNotNull("ArrayIdList object is null", response.getArrayIdList());
        assertEquals("Invalid size ", 2, response.getArrayIdList().size());
    }

    @Test
    public void deleteMigrationEnvironmentTest() throws Exception {
        assertNotNull("Api Client object is null", apiClient);
        MigrationEnvironmentListResponse response = apiClient.getMigrationEnvironmentList(sourceArraySerialNumber);
        assertNotNull("Response object is null", response);
        assertNotNull("ArrayIdList object is null", response.getArrayIdList());
        assertEquals("Invalid size ", 1, response.getArrayIdList().size());
        apiClient.deleteMigrationEnvironment(sourceArraySerialNumber, targetArraySerialNumber);

        response = apiClient.getMigrationEnvironmentList(sourceArraySerialNumber);
        assertNotNull("Response object is null", response);
        assertNotNull("ArrayIdList object is null", response.getArrayIdList());
        assertEquals("Invalid size ", 0, response.getArrayIdList().size());
    }

    @Test
    public void getAllMigrationStorageGroupsTest() throws Exception {
        assertNotNull("Api Client object is null", apiClient);
        MigrationStorageGroupListResponse getMigrationStorageGroupListResponse = apiClient
                .getMigrationStorageGroups(sourceArraySerialNumber);
        assertNotNull("Response object is null", getMigrationStorageGroupListResponse);
        assertNotNull("getNameList object is null", getMigrationStorageGroupListResponse.getNameList());
        assertEquals("Name List size should be greater than zero", true, getMigrationStorageGroupListResponse.getNameList().size() > 0);
    }

    @Test
    public void getMigrationStorageGroupTest() throws Exception {
        assertNotNull("Api Client object is null", apiClient);
        MigrationStorageGroupResponse getMigrationStorageGroupResponse = apiClient.getMigrationStorageGroup(sourceArraySerialNumber,
                targetArraySerialNumber, SG_NAME);
        assertNotNull("Response object is null", getMigrationStorageGroupResponse);
        assertEquals("Invalid sourceArray response", sourceArraySerialNumber, getMigrationStorageGroupResponse.getSourceArray());
        assertEquals("Invalid targetArray response", targetArraySerialNumber, getMigrationStorageGroupResponse.getTargetArray());
        assertEquals("Invalid storageGroup response", SG_NAME, getMigrationStorageGroupResponse.getStorageGroup());
        assertEquals("Invalid state response", "CutoverReady", getMigrationStorageGroupResponse.getState());
        assertEquals("Invalid totalCapacity response", 1.0, getMigrationStorageGroupResponse.getTotalCapacity(), 1);
        assertNotNull("Device Pair object is null", getMigrationStorageGroupResponse.getDevicePairs());
        assertEquals("Device Pair List size should be greater than zero", true,
                getMigrationStorageGroupResponse.getDevicePairs().size() > 0);

        assertNotNull("Source Masking View list object is null", getMigrationStorageGroupResponse.getSourceMaskingViewList());
        assertEquals("Source Masking View List size should be greater than zero", true,
                getMigrationStorageGroupResponse.getSourceMaskingViewList().size() > 0);
        assertNotNull("Target Masking View list object is null", getMigrationStorageGroupResponse.getTargetMaskingViewList());
        assertEquals("Target Masking View List size should be greater than zero", true,
                getMigrationStorageGroupResponse.getTargetMaskingViewList().size() > 0);

    }

    /**
     * Test getMigrationStorageGroup with existing and non existing SGs
     */
    @Test
    public void getMigrationStorageGroupTests() throws Exception {
        log.info("Starting getMigrationStorageGroup test");
        String source = "000195701351";
        String target = "000197000143";

        // test non exsting SG
        testGetMigrationStorageGroup(source, target, NON_EXISTING_SG, String.format(SG_CANNOT_FOUND, NON_EXISTING_SG, source));

        // test existing non migration SG
        testGetMigrationStorageGroup(source, target, SOURCE_SG_NON_MIGRATION, String.format(SG_CANNOT_FOUND, SOURCE_SG_NON_MIGRATION, target));

        // test migration SG
        MigrationStorageGroupResponse response = apiClient.getMigrationStorageGroup(source, target, SG_MIGRATION);
        assertNotNull("Response object is null", response);

        // switch source and target
        source = "000197000143";
        target = "000195701351";

        testGetMigrationStorageGroup(source, target, NON_EXISTING_SG, String.format(SG_CANNOT_FOUND, NON_EXISTING_SG, source));
        testGetMigrationStorageGroup(source, target, SOURCE_SG_NON_MIGRATION, String.format(SG_CANNOT_FOUND, SOURCE_SG_NON_MIGRATION, source));
        response = apiClient.getMigrationStorageGroup(source, target, SG_MIGRATION);
        assertNotNull("Response object is null", response);

        log.info("Finished getMigrationStorageGroup test");
    }

    private void testGetMigrationStorageGroup(String source, String target, String sg, String expectedErr) {
        try {
            apiClient.getMigrationStorageGroup(source, target, sg);
        } catch (Exception e) {
            String msg = e.getMessage();
            log.info("Error - " + msg);
            log.info("Expected error - " + expectedErr);
            assertTrue(StringUtils.contains(msg, expectedErr));
        }
    }

    @Test
    public void getAsyncJobStatus() throws Exception {
        assertNotNull("Api Client object is null", apiClient);
        AsyncJob asyncJob = apiClient.getAsyncJob("1502920747322");
        assertNotNull("asyncJob object is null", asyncJob);
        assertNotNull("jobId is null", asyncJob.getJobId());
    }

}
