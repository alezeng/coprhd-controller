/*
 * Copyright (c) 2008-2015 EMC Corporation
 * All Rights Reserved
 */

package com.emc.storageos.db.client.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbCheckerFileWriter {
    static final String WRITER_STORAGEOS = "StorageOS";
    static final String WRITER_GEOSTORAGEOS = "GeoStorageOS";
    static final String WRITER_REBUILD_INDEX = "RebuildIndex";
    private final static String FILE_PATH = "/tmp/";
    static final String CLEANUP_FILE_STORAGEOS = FILE_PATH + "cleanup-StorageOS.cql";
    static final String CLEANUP_FILE_GEOSTORAGEOS = FILE_PATH + "cleanup-GeoStorageOS.cql";
    static final String CLEANUP_FILE_REBUILD_INDEX = FILE_PATH + "cleanup-RebuildIndex.file";

    public static final String COMMENT_CHAR = "--";
    private static final String USAGE_COMMON_LINE1 = "This file is inconsistent data generated by Database Consistency Checker, please be much cautious playing with it.";
    private static final String USAGE_COMMON_LINE2 = "You can use the following command to clean up these inconsistent data. [Generated Date: %s]";
    private static final String USAGE_COMMON_LINE2_WITH_DATE = String.format(USAGE_COMMON_LINE2, new Date());
    private static final String USAGE_STORAGEOS = String.format("%s %s\n%s %s\n%s /opt/storageos/bin/cqlsh -k StorageOS -f %s\n",
            COMMENT_CHAR, USAGE_COMMON_LINE1, COMMENT_CHAR, USAGE_COMMON_LINE2_WITH_DATE, COMMENT_CHAR, CLEANUP_FILE_STORAGEOS);
    private static final String USAGE_GEOSTORAGEOS = String.format(
            "%s %s\n%s %s\n%s /opt/storageos/bin/cqlsh -k GeoStorageOS -f %s localhost 9260\n",
            COMMENT_CHAR, USAGE_COMMON_LINE1, COMMENT_CHAR, USAGE_COMMON_LINE2_WITH_DATE, COMMENT_CHAR, CLEANUP_FILE_GEOSTORAGEOS);
    private static final String USAGE_REBUILDINDEX = String.format("%s %s\n%s %s\n%s /opt/storageos/bin/dbutils rebuild_index %s\n",
            COMMENT_CHAR, USAGE_COMMON_LINE1, COMMENT_CHAR, USAGE_COMMON_LINE2_WITH_DATE, COMMENT_CHAR, CLEANUP_FILE_REBUILD_INDEX);

    private static final String STORAGEOS_NAME = "storageos";
    private static String owner = STORAGEOS_NAME;
    private static String group = STORAGEOS_NAME;
    private static final Set<String> cleanupFiles = new HashSet<String>();
    private static final Map<String, BufferedWriter> writers = new HashMap<String, BufferedWriter>();
    private static final Logger log = LoggerFactory.getLogger(DbCheckerFileWriter.class);

    private DbCheckerFileWriter() {
    }

    static void writeTo(String name, String lineStr) {
        try {
            BufferedWriter writer = getWriter(name);
            writeln(writer, lineStr);
        } catch (IOException e) {
            System.out.println("Exception: " + e);
            log.error("Caught Exception: ", e);
        }
    }

    private static BufferedWriter getWriter(String name) throws IOException {
        WriteType type = WriteType.getByType(name);
        BufferedWriter writer = writers.get(type.filename);
        if (writer == null) {
            writer = init(type.filename, type.usage);
            writers.put(type.filename, writer);
            cleanupFiles.add(type.filename);
        }
        return writer;
    }

    private static BufferedWriter init(String fileName, String usage) throws IOException {
        deleteIfExists(fileName);
        final Path filePath = FileSystems.getDefault().getPath(fileName);
        BufferedWriter writer = Files.newBufferedWriter(filePath, Charset.defaultCharset());
        Set<PosixFilePermission> perms = EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
        setFilePermissions(filePath, owner, group, perms);

        writeln(writer, usage);
        return writer;
    }

    private static void writeln(BufferedWriter writer, String str) throws IOException {
        writer.write(str);
        writer.newLine();
        writer.flush();
    }

    public static void close() {
        for (Writer writer : writers.values()) {
            try {
                if (writer != null) {
                    writer.close();
                    writer = null;
                }
            } catch (IOException e) {
                log.error("Exception happens when closing file, e=", e);
            }
        }


    }

    public static String getGeneratedFileNames() {
        return StringUtils.join(cleanupFiles, " ");
    }

    private static void deleteIfExists(String fileName) {
        File file = new File(fileName);
        if (file.exists()) {
            file.delete();
        }
    }

    private static void setFilePermissions(Path path, String owner, String groupName,
            Set<PosixFilePermission> permissions) throws IOException {
        UserPrincipalLookupService lookupService = FileSystems.getDefault().getUserPrincipalLookupService();
        UserPrincipal user = lookupService.lookupPrincipalByName(owner);
        GroupPrincipal group = lookupService.lookupPrincipalByGroupName(groupName);
        PosixFileAttributeView attributeView = Files.getFileAttributeView(path, PosixFileAttributeView.class);
        attributeView.setGroup(group);
        attributeView.setOwner(user);
        Files.setPosixFilePermissions(path, permissions);
    }
    
    enum WriteType {
        STORAGEOS(WRITER_STORAGEOS, CLEANUP_FILE_STORAGEOS, USAGE_STORAGEOS),
        GEOSTORAGEOS(WRITER_GEOSTORAGEOS, CLEANUP_FILE_GEOSTORAGEOS, USAGE_GEOSTORAGEOS),
        REBUILD_INDEX(WRITER_REBUILD_INDEX, CLEANUP_FILE_REBUILD_INDEX, USAGE_REBUILDINDEX);
        String type;
        String filename;
        String usage;

        WriteType(String type, String filename, String usage) {
            this.type = type;
            this.filename = filename;
            this.usage = usage;
        }
        
        public static WriteType getByType(String type) {
            for (WriteType writeType : values()) {
                if (writeType.type.equals(type)) {
                    return writeType;
                }
            }
            throw new RuntimeException("invalid type:" + type);
        }
    }
}