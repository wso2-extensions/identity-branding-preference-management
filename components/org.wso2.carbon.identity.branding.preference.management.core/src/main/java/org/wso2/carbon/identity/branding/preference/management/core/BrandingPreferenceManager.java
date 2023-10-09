/*
 * Copyright (c) 2022-2023, WSO2 LLC. (http://www.wso2.com).
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
 * Branding preference management service interface.
 */
public interface BrandingPreferenceManager {

    /**
     * This API is used to create a branding preference.
     *
     * @param brandingPreference Branding preference.
     * @return the created branding preference.
     * @throws BrandingPreferenceMgtException if any error occurred.
     */
    BrandingPreference addBrandingPreference(BrandingPreference brandingPreference)
            throws BrandingPreferenceMgtException;

    /**
     * This API is used to retrieve a branding preference.
     *
     * @param type   Type of the branding preference.
     * @param name   Name of the tenant/application.
     * @param locale Language preference of the branding.
     * @return The requested branding preference. If not exists return the default branding preference.
     * @throws BrandingPreferenceMgtException if any error occurred.
     */
    BrandingPreference getBrandingPreference(String type, String name, String locale)
            throws BrandingPreferenceMgtException;

    /**
     * This API is used to retrieve a resolved branding preference.
     *
     * @param type   Type of the branding preference.
     * @param name   Name of the tenant/application.
     * @param locale Language preference of the branding.
     * @return The resolved branding preference. If not exists return the default branding preference.
     * @throws BrandingPreferenceMgtException if any error occurred.
     */
    BrandingPreference resolveBrandingPreference(String type, String name, String locale)
            throws BrandingPreferenceMgtException;

    /**
     * This API is used to replace a given branding preference.
     *
     * @param brandingPreference Branding preference to be added.
     * @return Updated branding preference.
     * @throws BrandingPreferenceMgtException if any error occurred.
     */
    BrandingPreference replaceBrandingPreference(BrandingPreference brandingPreference)
            throws BrandingPreferenceMgtException;

    /**
     * This API is used to delete a branding preference.
     *
     * @param type   Type of the branding preference.
     * @param name   Name of the tenant/application.
     * @param locale language preference of the branding.
     * @throws BrandingPreferenceMgtException if any error occurred.
     */
    void deleteBrandingPreference(String type, String name, String locale)
            throws BrandingPreferenceMgtException;

    /**
     * This API is used to create a custom text preference.
     *
     * @param customText Custom Text preference.
     * @return The created custom text preference.
     * @throws BrandingPreferenceMgtException if any error occurred.
     */
    default CustomText addCustomText(CustomText customText) throws BrandingPreferenceMgtException {

        throw new NotImplementedException("This functionality is not implemented.");
    }

    /**
     * This API is used to retrieve a custom text preference.
     *
     * @param type   Type of the custom text preference.
     * @param name   Name of the tenant/application where custom text belongs.
     * @param screen Screen where the custom text needs to be applied.
     * @param locale Language preference of the custom text.
     * @return The requested custom text preference.
     * @throws BrandingPreferenceMgtException if any error occurred.
     */
    default CustomText getCustomText(String type, String name, String screen, String locale)
            throws BrandingPreferenceMgtException {

        throw new NotImplementedException("This functionality is not implemented.");
    }

    /**
     * This API is used to retrieve a resolved custom text preference.
     *
     * @param type   Type of the custom text preference.
     * @param screen Screen where the custom text needs to be applied.
     * @param name   Name of the tenant/application where custom text belongs.
     * @param locale Language preference of the custom text.
     * @return The resolved custom text preference. If not exists return the default custom text preference.
     * @throws BrandingPreferenceMgtException if any error occurred.
     */
    default CustomText resolveCustomText(String type, String name, String screen, String locale)
            throws BrandingPreferenceMgtException {

        throw new NotImplementedException("This functionality is not implemented.");
    }

    /**
     * This API is used to replace a given custom text preference.
     *
     * @param customText Custom Text preference to be added.
     * @return Updated custom text preference.
     * @throws BrandingPreferenceMgtException if any error occurred.
     */
    default CustomText replaceCustomText(CustomText customText) throws BrandingPreferenceMgtException {

        throw new NotImplementedException("This functionality is not implemented.");
    }

    /**
     * This API is used to delete a custom text preference.
     *
     * @param type   Type of the custom text preference.
     * @param name   Name of the tenant/application where custom text belongs.
     * @param screen Screen of the custom text.
     * @param locale Language preference of the custom text.
     * @throws BrandingPreferenceMgtException if any error occurred.
     */
    default void deleteCustomText(String type, String name, String screen, String locale)
            throws BrandingPreferenceMgtException {

        throw new NotImplementedException("This functionality is not implemented.");
    }

}
