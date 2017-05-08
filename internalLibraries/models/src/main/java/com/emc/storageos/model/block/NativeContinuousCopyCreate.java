/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.storageos.model.block;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Native continuous copy creation parameters. The
 * name and count values are used to generate volume
 * labels for the mirrors. If count is 1, name is used
 * as the label. If count is greater than 1, name and
 * count are combined to generate labels for the multiple
 * mirrors.
 */
@XmlRootElement(name = "native_continuous_copy_create")
public class NativeContinuousCopyCreate {

    private String name;
    private Integer count;
    private BlockPerformanceParamsMap performanceParams;    

    public NativeContinuousCopyCreate() {
    }

    public NativeContinuousCopyCreate(String name, Integer count, BlockPerformanceParamsMap performanceParams) {
        this.name = name;
        this.count = count;
        this.performanceParams = performanceParams;
    }

    /**
     * User provided name.
     * 
     */
    @XmlElement(required = true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * User provided number of copies.
     *
     */ 
    @XmlElement(required = false)
    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }
    
    /**
     * The performance parameters to use when the newly created mirror is 
     * provisioned.
     * 
     * @return The performance parameters.
     */
    @XmlElement(name = "performance_params")
    public BlockPerformanceParamsMap getPerformanceParams() {
        return performanceParams;
    }

    public void setPerformanceParams(BlockPerformanceParamsMap performanceParams) {
        this.performanceParams = performanceParams;
    }    
}
