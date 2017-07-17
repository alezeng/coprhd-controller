/*
 * Copyright (c) 2017 EMC Corporation
 * All Rights Reserved
 */
package com.emc.storageos.driver.vmax3.restengine;

import static com.google.json.JsonSanitizer.sanitize;

import java.lang.reflect.Type;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.emc.storageos.driver.vmax3.smc.basetype.AuthenticationInfo;
import com.emc.storageos.driver.vmax3.smc.basetype.IParameter;
import com.emc.storageos.driver.vmax3.smc.basetype.IResponse;
import com.emc.storageos.driver.vmax3.smc.basetype.ResponseWrapper;
import com.emc.storageos.driver.vmax3.smc.symmetrix.resource.IteratorType;
import com.emc.storageos.driver.vmax3.utils.JsonParser;
import com.google.gson.Gson;
import com.sun.jersey.api.client.ClientResponse;

public class RestEngine {
    private static final Logger LOG = LoggerFactory.getLogger(RestClient.class);

    private AuthenticationInfo authenticationInfo;
    private boolean isVerifyCertificate = true;
    private RestClient restClient;

    /**
     * @param authenticationInfo
     */
    public RestEngine(AuthenticationInfo authenticationInfo) {
        super();
        this.authenticationInfo = authenticationInfo;
        this.restClient = new RestClient(this.authenticationInfo, isVerifyCertificate);
    }

    /**
     * Process get request.
     * 
     * @param url
     * @param Class<T> clazz, response bean
     * @return ResponseWrapper<T>
     */
    public <T extends IResponse> ResponseWrapper<T> get(String url, Class<T> clazz) {
        ClientResponse response = null;
        ResponseWrapper<T> responseWrapper = new ResponseWrapper<T>();
        try {
            response = restClient.get(url);
            processResponse(response, clazz, responseWrapper);

        } catch (Exception e) {
            // TODO: translate this exception to cust exception
            LOG.error("");
            responseWrapper.setException(e);
        } finally {
            if (response != null) {
                response.close();
            }
        }

        return responseWrapper;

    }

    public <T extends IResponse> ResponseWrapper<T> list(String url, Class<T> clazz, Type responseClazzType) {
        ClientResponse response = null;
        ResponseWrapper<T> responseWrapper = new ResponseWrapper<T>();
        try {
            response = restClient.get(url);
            processIteratorResponse(response, clazz, responseClazzType, responseWrapper);

        } catch (Exception e) {
            // TODO: translate this exception to cust exception
            LOG.error("");
            responseWrapper.setException(e);
        } finally {
            if (response != null) {
                response.close();
            }
        }

        return responseWrapper;

    }

    /**
     * Process post request.
     * 
     * @param url
     * @param IParameter params, request content
     * @param Class<T> clazz, response bean
     * @return ResponseWrapper<T>
     */
    public <T extends IResponse> ResponseWrapper<T> post(String url, IParameter params, Class<T> clazz) {
        ClientResponse response = null;
        ResponseWrapper<T> responseWrapper = new ResponseWrapper<T>();
        try {
            response = restClient.post(url, params.bean2Json());
            processResponse(response, clazz, responseWrapper);

        } catch (Exception e) {
            // TODO: translate this exception to cust exception
            LOG.error("{}", e);
            responseWrapper.setException(e);
        } finally {
            if (response != null) {
                response.close();
            }
        }

        return responseWrapper;

    }

    /**
     * Process put request.
     * 
     * @param url
     * @param IParameter params, request content
     * @param Class<T> clazz, response bean
     * @return ResponseWrapper<T>
     */
    public <T extends IResponse> ResponseWrapper<T> put(String url, IParameter params, Class<T> clazz) {
        ClientResponse response = null;
        ResponseWrapper<T> responseWrapper = new ResponseWrapper<T>();
        try {
            response = restClient.put(url, params.bean2Json());
            processResponse(response, clazz, responseWrapper);

        } catch (Exception e) {
            // TODO: translate this exception to cust exception
            LOG.error("");
            responseWrapper.setException(e);
        } finally {
            if (response != null) {
                response.close();
            }
        }

        return responseWrapper;

    }

    /**
     * Process delete request.
     * 
     * @param url
     * @param Class<T> clazz, response bean
     * @return ResponseWrapper<T>
     */
    public <T extends IResponse> ResponseWrapper<T> delete(String url, Class<T> clazz) {
        ClientResponse response = null;
        ResponseWrapper<T> responseWrapper = new ResponseWrapper<T>();
        try {
            response = restClient.delete(url);
            processResponse(response, clazz, responseWrapper);

        } catch (Exception e) {
            // TODO: translate this exception to cust exception
            LOG.error("");
            responseWrapper.setException(e);
        } finally {
            if (response != null) {
                response.close();
            }
        }

        return responseWrapper;

    }

    private <T extends IResponse> void processResponse(ClientResponse response, Class<T> clazz, ResponseWrapper<T> responseWrapper) {
        if (response == null) {
            // TODO: define cust Exception and use it here
            responseWrapper.setException(new NullPointerException(""));
            return;
        }
        String respnseString = response.getEntity(String.class);
        int status = response.getStatus();
        // if (responseWrapper.isSuccessfulStatus()) {
        T bean = JsonParser.parseJson2Bean((respnseString), clazz);
        bean.setStatus(status);
        responseWrapper.setResponseBean(bean);
        // } else {
        // responseWrapper.setMessage(respnseString);
        // }
    }

    private <T extends IResponse> void processIteratorResponse(ClientResponse response, Class<T> clazz, Type responseClazzType,
            ResponseWrapper<T> responseWrapper) {
        if (response == null) {
            // TODO: define cust Exception and use it here
            responseWrapper.setException(new NullPointerException(""));
            return;
        }
        String respnseString = response.getEntity(String.class);
        int status = response.getStatus();
        // if (responseWrapper.isSuccessfulStatus()) {

        // add the polymorphic specialization

        IteratorType<T> beanIterator = new Gson().fromJson(sanitize(respnseString), responseClazzType);
        beanIterator.setStatus(status);
        responseWrapper.setResponseBeanIterator(beanIterator);
        // } else {
        // responseWrapper.setMessage(respnseString);
        // }
    }

}