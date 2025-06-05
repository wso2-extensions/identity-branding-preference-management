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

package org.wso2.carbon.identity.branding.preference.management.core.dao;

import org.wso2.carbon.identity.branding.preference.management.core.exception.BrandingPreferenceMgtException;
import org.wso2.carbon.identity.branding.preference.management.core.model.CustomLayoutContent;

/**
 * Interface defining the operations for managing custom layout content persistence.
 * Provides methods to add, update, retrieve, and delete custom layout content.
 */
public interface CustomContentPersistentDAO {

    /**
     * Adds custom layout content (HTML, CSS, JS).
     *
     * @param customLayoutContent The {@link CustomLayoutContent} object containing HTML, CSS, and JS content.
     * @param applicationUuid     Application UUID.
     * @param tenantDomain        Tenant domain.
     * @throws BrandingPreferenceMgtException If an error occurs while adding the custom layout content.
     */
    void addCustomContent(CustomLayoutContent customLayoutContent, String applicationUuid,
                          String tenantDomain) throws BrandingPreferenceMgtException;

    /**
     * Updates the existing custom layout content.
     *
     * @param customLayoutContent The {@link CustomLayoutContent} object containing updated HTML, CSS, and JS content.
     * @param applicationUuid     Application UUID.
     * @param tenantDomain        Tenant domain.
     * @throws BrandingPreferenceMgtException If an error occurs while updating the custom layout content.
     */
    void updateCustomContent(CustomLayoutContent customLayoutContent, String applicationUuid,
                             String tenantDomain) throws BrandingPreferenceMgtException;

    /**
     * Get specified custom layout content.
     *
     * @param applicationUuid Application UUID.
     * @param tenantDomain    Tenant domain.
     * @return Custom Layout Content for a particular APP or ORG
     * @throws BrandingPreferenceMgtException If an error occurred while retrieving the content.
     */
    CustomLayoutContent getCustomContent(String applicationUuid, String tenantDomain)
            throws BrandingPreferenceMgtException;

    /**
     * Delete specified custom layout content.
     *
     * @param applicationUuid Application UUID.
     * @param tenantDomain    Tenant domain.
     * @throws BrandingPreferenceMgtException If an error occurred while deleting the content.
     */
    void deleteCustomContent(String applicationUuid, String tenantDomain) throws BrandingPreferenceMgtException;
}
