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
 * This Interface is to define CRUD operations for Application vise Custom Layout Content
 */
public interface AppCustomContentDAO {

    /**
     * Checks whether custom layout content exists for the given APP.
     *
     * @param applicationUuid   Application UUID.
     * @param tenantId          Tenant ID.
     * @return                  True if custom content exists for the tenant, false otherwise.
     * @throws BrandingPreferenceMgtException if an error occurs during the database access.
     */
    boolean isAppCustomContentAvailable(String applicationUuid, int tenantId)
            throws BrandingPreferenceMgtException;

    /**
     * Adds new custom layout content (HTML, CSS, JS) for a given APP.
     *
     * @param content The {@link CustomLayoutContent} object containing HTML, CSS, and JS content.
     * @param applicationUuid Application UUID.
     * @param tenantId Tenant ID.
     * @throws BrandingPreferenceMgtException if an error occurs during insertion of any content.
     */
    void addAppCustomContent(CustomLayoutContent content, String applicationUuid, int tenantId)
            throws BrandingPreferenceMgtException;

    /**
     * Updates the custom layout content (HTML, CSS, JS) for the given APP.
     *
     * @param content The {@link CustomLayoutContent} object containing updated content.
     * @param applicationUuid Application UUID.
     * @param tenantId Tenant ID.
     * @throws BrandingPreferenceMgtException if an error occurs during update.
     */
    void updateAppCustomContent(CustomLayoutContent content, String applicationUuid, int tenantId)
            throws BrandingPreferenceMgtException;

    /**
     * Retrieves the custom layout content (HTML, CSS, JS) for the specified APP.
     *
     * @param applicationUuid Application UUID.
     * @param tenantId Tenant ID.
     * @return A {@link CustomLayoutContent} object containing the APP's custom layout content.
     * @throws BrandingPreferenceMgtException if an error occurs while fetching the content.
     */
    CustomLayoutContent getAppCustomContent(String applicationUuid, int tenantId)
            throws BrandingPreferenceMgtException;

    /**
     * Deletes all custom layout content (HTML, CSS, JS) for the specified APP.
     *
     * @param applicationUuid Application UUID.
     * @param tenantId Tenant ID.
     * @throws BrandingPreferenceMgtException if an error occurs during deletion.
     */
    void deleteAppCustomContent(String applicationUuid, int tenantId) throws BrandingPreferenceMgtException;

}
