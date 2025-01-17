/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.branding.preference.management.core.ai;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.wso2.carbon.identity.ai.service.mgt.exceptions.AIClientException;
import org.wso2.carbon.identity.ai.service.mgt.exceptions.AIServerException;
import org.wso2.carbon.identity.core.util.IdentityUtil;

import java.util.HashMap;
import java.util.Map;

import static org.wso2.carbon.identity.ai.service.mgt.util.AIHttpClientUtil.executeRequest;
import static org.wso2.carbon.identity.branding.preference.management.core.ai.constants.BrandingAIConstants.OPERATION_ID_PROPERTY;
import static org.wso2.carbon.identity.branding.preference.management.core.ai.constants.BrandingAIConstants.WEB_SITE_URL_PROPERTY;
import static org.wso2.carbon.registry.core.RegistryConstants.PATH_SEPARATOR;

/**
 * Implementation of the AI Branding Preference Manager.
 */
public class BrandingAIPreferenceManagerImpl implements BrandingAIPreferenceManager {

    private static final String BRANDING_AI_ENDPOINT = IdentityUtil.getProperty(
            "AIServices.BrandingAI.BrandingAIEndpoint");
    private static final String BRANDING_AI_GENERATE_ENDPOINT = "/api/server/v1/branding-preference/generate";
    private static final String BRANDING_AI_STATUS_ENDPOINT = "/api/server/v1/branding-preference/status";
    private static final String BRANDING_AI_RESULT_ENDPOINT = "/api/server/v1/branding-preference/result";

    private static final Log LOG = LogFactory.getLog(BrandingAIPreferenceManagerImpl.class);

    @Override
    public String generateBrandingPreference(String websiteURL) throws AIClientException, AIServerException {

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put(WEB_SITE_URL_PROPERTY, websiteURL);
        Map<String, Object> stringObjectMap = executeRequest(BRANDING_AI_ENDPOINT, BRANDING_AI_GENERATE_ENDPOINT,
                HttpPost.class, requestBody);
        return (String) stringObjectMap.get(OPERATION_ID_PROPERTY);
    }

    @Override
    public Object getBrandingPreferenceGenerationStatus(String operationId) throws AIClientException,
            AIServerException {

        return executeRequest(BRANDING_AI_ENDPOINT, BRANDING_AI_STATUS_ENDPOINT + PATH_SEPARATOR + operationId,
                HttpGet.class, null);
    }

    @Override
    public Object getBrandingPreferenceGenerationResult(String operationId) throws AIClientException,
            AIServerException {

        return executeRequest(BRANDING_AI_ENDPOINT, BRANDING_AI_RESULT_ENDPOINT + PATH_SEPARATOR + operationId,
                HttpGet.class, null);
    }
}
