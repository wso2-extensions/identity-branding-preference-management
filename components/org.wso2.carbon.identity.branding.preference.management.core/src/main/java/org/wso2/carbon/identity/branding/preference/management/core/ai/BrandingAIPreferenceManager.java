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

import org.wso2.carbon.identity.ai.service.mgt.exceptions.AIClientException;
import org.wso2.carbon.identity.ai.service.mgt.exceptions.AIServerException;

/**
 * Interface for AI branding preference manager.
 */
public interface BrandingAIPreferenceManager {

    /**
     * This method will connect to the BrandingAI microservice and generate the branding preference for the given
     * website URL.
     *
     * @param websiteURL Website URL for which the branding preference should be generated.
     * @return Operation ID of the branding preference generation.
     * @throws AIServerException When errors occurs from the AI service.
     * @throws AIClientException When invalid request is sent to the AI service.
     */
    String generateBrandingPreference(String websiteURL) throws AIClientException, AIServerException;

    /**
     * This method will connect to the BrandingAI microservice and get the status of the branding preference generation.
     *
     * @param operationId Operation ID of the branding preference generation.
     * @return Status of the branding preference generation.
     * @throws AIServerException When errors occurs from the AI service.
     * @throws AIClientException When invalid request is sent to the AI service.
     */
    Object getBrandingPreferenceGenerationStatus(String operationId) throws AIClientException, AIServerException;

    /**
     * This method will connect to the BrandingAI microservice and get the result of the branding preference generation.
     *
     * @param operationId Operation ID of the branding preference generation.
     * @return Result of the branding preference generation.
     * @throws AIServerException When errors occurs from the AI service.
     * @throws AIClientException When invalid request is sent to the AI service.
     */
    Object getBrandingPreferenceGenerationResult(String operationId) throws AIClientException, AIServerException;
}
