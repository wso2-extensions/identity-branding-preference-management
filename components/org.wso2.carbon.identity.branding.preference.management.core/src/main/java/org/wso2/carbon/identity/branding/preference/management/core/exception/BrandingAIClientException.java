/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.branding.preference.management.core.exception;

import org.wso2.carbon.identity.branding.preference.management.core.ai.BrandingAIPreferenceManagerImpl;

/**
 * Client Exception class for BrandingAI service.
 */
public class BrandingAIClientException extends Exception {

    private String errorCode;
    private BrandingAIPreferenceManagerImpl.HttpResponseWrapper brandingAIResponse;

    public BrandingAIClientException(String message, String errorCode) {

        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param httpResponseWrapper Here we add the response wrapper to the exception to get the response details. This
     *                            contains the error response that return from the Branding AI microservice.
     * @param message             The detail message (which is saved for later retrieval by the getMessage() method).
     * @param errorCode           The error code.
     */
    public BrandingAIClientException(BrandingAIPreferenceManagerImpl.HttpResponseWrapper httpResponseWrapper,
                                     String message, String errorCode) {

        super(message);
        this.errorCode = errorCode;
        this.brandingAIResponse = httpResponseWrapper;
    }

    public BrandingAIClientException(String message, Throwable cause) {

        super(cause);
    }

    public BrandingAIClientException(String message, String errorCode, Throwable cause) {

        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {

        return errorCode;
    }

    public BrandingAIPreferenceManagerImpl.HttpResponseWrapper getBrandingAIResponse() {

        return brandingAIResponse;
    }
}
