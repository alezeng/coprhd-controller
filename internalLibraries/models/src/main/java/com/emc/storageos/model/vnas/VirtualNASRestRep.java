/*
 * Copyright (c) 2008-2013 EMC Corporation
 * All Rights Reserved
 */

package com.emc.storageos.model.vnas;

import java.net.URI;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import com.emc.storageos.model.NamedRelatedResourceRep;
import com.emc.storageos.model.varray.VirtualArrayResourceRestRep;

@XmlRootElement(name = "virtual_nas_server")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class VirtualNASRestRep extends VirtualArrayResourceRestRep {
    
    // NAS Server name
    private String nasName;
    
    
    // storageSystem, which it belongs
    private NamedRelatedResourceRep storageDeviceURI;
    private String maxFSID;
    private String maxExports;
    private String maxProvisionedCapacity;
    private Set<String> protocols;
    
    // Set of Authentication providers for the VNasServer - set values will of type AunthnProvider
    private Set<String> cifsServers;
    
    // List of Storage Ports associated with this Nas Server
    private Set<NamedRelatedResourceRep> storagePorts;
    
    // State of the NAS server
    private String nasState;
    
    
    // Place holder for hosting storageDomain's information
    private Set<NamedRelatedResourceRep> storageDomain;
    
    private String registrationStatus ;
    private String compatibilityStatus; 
    private String discoveryStatus ;
    
    // Place holder for Tag
    private Set<String> nasTag;
    
    
    // Project name which this VNAS belongs to
    private NamedRelatedResourceRep project;

    private String vNasType;

    // Base directory Path for the VNAS applicable in AccessZones & vFiler device types
    private String baseDirPath;

    // place holder for the Parent NAS server the Data Mover
    private NamedRelatedResourceRep parentNASURI;


    public VirtualNASRestRep() {
    }


    @XmlElement(name="nas_name")
    public String getNasName() {
        return nasName;
    }


    public void setNasName(String nasName) {
        this.nasName = nasName;
    }


    @XmlElement(name="storage_device")
    public NamedRelatedResourceRep getStorageDeviceURI() {
        return storageDeviceURI;
    }


    public void setStorageDeviceURI(NamedRelatedResourceRep storageDeviceURI) {
        this.storageDeviceURI = storageDeviceURI;
    }


    @XmlElement(name="max_fsid")
    public String getMaxFSID() {
        return maxFSID;
    }


    public void setMaxFSID(String maxFSID) {
        this.maxFSID = maxFSID;
    }


    @XmlElement(name="max_exports")
    public String getMaxExports() {
        return maxExports;
    }


    public void setMaxExports(String maxExports) {
        this.maxExports = maxExports;
    }


    @XmlElement(name="max_provisioned_capacity")
    public String getMaxProvisionedCapacity() {
        return maxProvisionedCapacity;
    }


    public void setMaxProvisionedCapacity(String maxProvisionedCapacity) {
        this.maxProvisionedCapacity = maxProvisionedCapacity;
    }


    @XmlElementWrapper(name = "protocols")
    @XmlElement(name="protocol")
    public Set<String> getProtocols() {
        return protocols;
    }


    public void setProtocols(Set<String> protocols) {
        this.protocols = protocols;
    }

    @XmlElementWrapper(name = "cifs_servers")
    @XmlElement(name="cifs_server")
    public Set<String> getCifsServers() {
        return cifsServers;
    }


    public void setCifsServers(Set<String> cifsServers) {
        this.cifsServers = cifsServers;
    }


    @XmlElementWrapper(name = "storage_ports")
    @XmlElement(name="storage_port")
    public Set<NamedRelatedResourceRep> getStoragePorts() {
        return storagePorts;
    }


    public void setStoragePorts(Set<NamedRelatedResourceRep> storagePorts) {
        this.storagePorts = storagePorts;
    }


    @XmlElement(name="nas_state")
    public String getNasState() {
        return nasState;
    }


    public void setNasState(String nasState) {
        this.nasState = nasState;
    }


    @XmlElementWrapper(name = "storage_domains")
    @XmlElement(name="storage_domain")
    public Set<NamedRelatedResourceRep> getStorageDomain() {
        return storageDomain;
    }


    public void setStorageDomain(Set<NamedRelatedResourceRep> storageDomain) {
        this.storageDomain = storageDomain;
    }


    @XmlElement(name="registration_status")
    public String getRegistrationStatus() {
        return registrationStatus;
    }


    public void setRegistrationStatus(String registrationStatus) {
        this.registrationStatus = registrationStatus;
    }


    @XmlElement(name="compatibility_status")
    public String getCompatibilityStatus() {
        return compatibilityStatus;
    }


    public void setCompatibilityStatus(String compatibilityStatus) {
        this.compatibilityStatus = compatibilityStatus;
    }


    @XmlElement(name="discovery_status")
    public String getDiscoveryStatus() {
        return discoveryStatus;
    }


    public void setDiscoveryStatus(String discoveryStatus) {
        this.discoveryStatus = discoveryStatus;
    }

    @XmlElementWrapper(name = "nas_tags")
    @XmlElement(name="nas_tag")
    public Set<String> getNasTag() {
        return nasTag;
    }


    public void setNasTag(Set<String> nasTag) {
        this.nasTag = nasTag;
    }


    @XmlElement(name="project")
    public NamedRelatedResourceRep getProject() {
        return project;
    }


    public void setProject(NamedRelatedResourceRep project) {
        this.project = project;
    }


    @XmlElement(name="vnas_type")
    public String getvNasType() {
        return vNasType;
    }


    public void setvNasType(String vNasType) {
        this.vNasType = vNasType;
    }


    @XmlElement(name="base_dir_path")
    public String getBaseDirPath() {
        return baseDirPath;
    }


    public void setBaseDirPath(String baseDirPath) {
        this.baseDirPath = baseDirPath;
    }


    @XmlElement(name="parent_nas")
    public NamedRelatedResourceRep getParentNASURI() {
        return parentNASURI;
    }


    public void setParentNASURI(NamedRelatedResourceRep parentNASURI) {
        this.parentNASURI = parentNASURI;
    }
    
}
