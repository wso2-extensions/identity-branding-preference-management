/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.branding.preference.management.core;

import org.wso2.carbon.identity.branding.preference.management.core.exception.BrandingPreferenceMgtException;
import org.wso2.carbon.identity.branding.preference.management.core.exception.NotImplementedException;
import org.wso2.carbon.identity.branding.preference.management.core.model.BrandingPreference;
import org.wso2.carbon.identity.branding.preference.management.core.model.CustomText;

/**
 * UI Branding Preference Resolver.
 */
public interface UIBrandingPreferenceResolver {

    /**
     * This method is used to retrieve a resolved branding preference.
     *
     * @param type   Type of the branding preference.
     * @param name   Name of the tenant/application.
     * @param locale Language preference of the branding.
     * @return The requested branding preference. If not exists return the default branding preference.
     * @throws BrandingPreferenceMgtException if any error occurred.
     */
    BrandingPreference resolveBranding(String type, String name, String locale) throws BrandingPreferenceMgtException;

    /**
     * This method is used to clear the branding preference resolver caches of
     * the organization and all its children down the tree.
     *
     * @param currentTenantDomain   Tenant domain where the cache needs to be cleared.
     * @throws BrandingPreferenceMgtException if any error occurred.
     */
    void clearBrandingResolverCacheHierarchy(String currentTenantDomain) throws BrandingPreferenceMgtException;

    /**
     * This method is used to retrieve a resolved custom text preference.
     *
     * @param type   Type of the custom text preference.
     * @param name   Name of the tenant/application.
     * @param screen Screen param of the custom text preference.
     * @param locale Language preference of the custom text preference.
     * @return The resolved custom text preference.
     * @throws BrandingPreferenceMgtException if any error occurred.
     */
    default CustomText resolveCustomText(String type, String name, String screen, String locale)
            throws BrandingPreferenceMgtException {

        throw new NotImplementedException("This functionality is not implemented.");
    }

    /**
     * This method is used to clear the custom text preference resolver caches of
     * the organization and all its children down the tree.
     *
     * @param currentTenantDomain   Tenant domain where the cache needs to be cleared.
     * @param screen                Screen param of the custom text.
     * @param locale                Locale param of the custom text.
     * @throws BrandingPreferenceMgtException if any error occurred.
     */
    default void clearCustomTextResolverCacheHierarchy(String currentTenantDomain, String screen, String locale)
            throws BrandingPreferenceMgtException {

        throw new NotImplementedException("This functionality is not implemented.");
    }
}
