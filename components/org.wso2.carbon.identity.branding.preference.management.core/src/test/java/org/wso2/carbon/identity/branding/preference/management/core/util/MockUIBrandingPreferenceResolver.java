/*
 * Copyright (c) 2023-2024, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.branding.preference.management.core.util;

import org.wso2.carbon.identity.branding.preference.management.core.UIBrandingPreferenceResolver;
import org.wso2.carbon.identity.branding.preference.management.core.exception.BrandingPreferenceMgtException;
import org.wso2.carbon.identity.branding.preference.management.core.model.BrandingPreference;
import org.wso2.carbon.identity.branding.preference.management.core.model.CustomText;

public class MockUIBrandingPreferenceResolver implements UIBrandingPreferenceResolver {

    BrandingPreference brandingPreference;

    CustomText customText;

    @Override
    public BrandingPreference resolveBranding(String type, String name, String locale)
            throws BrandingPreferenceMgtException {

        return brandingPreference;
    }

    @Override
    public void clearBrandingResolverCacheHierarchy(String type, String name, String currentTenantDomain)
            throws BrandingPreferenceMgtException {

        setBranding(null);
    }

    @Override
    public void clearBrandingResolverCacheHierarchy(String currentTenantDomain) throws BrandingPreferenceMgtException {
        
        setBranding(null);
    }

    @Override
    public CustomText resolveCustomText(String type, String name, String screen, String locale)
            throws BrandingPreferenceMgtException {

        return customText;
    }

    @Override
    public void clearCustomTextResolverCacheHierarchy(String currentTenantDomain, String screen, String locale)
            throws BrandingPreferenceMgtException {

        setCustomText(null);
    }

    public void setBranding(BrandingPreference brandingPreference) {

        this.brandingPreference = brandingPreference;
    }

    public void setCustomText(CustomText customText) {

        this.customText = customText;
    }
}
