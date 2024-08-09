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

package org.wso2.carbon.identity.branding.preference.management.core.util;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.branding.preference.management.core.model.BrandingPreference;

import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.DEFAULT_LOCALE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ORGANIZATION_TYPE;
import static org.wso2.carbon.identity.branding.preference.management.core.util.TestUtils.getPreferenceFromFile;

/**
 * Unit tests for BrandingPreferenceMgtUtils.
 */
public class BrandingPreferenceMgtUtilsTest {

    public static final String SUPER_TENANT_DOMAIN_NAME = "carbon.super";

    @DataProvider(name = "publishedBrandingPreferenceDataProvider")
    public Object[][] publishedBrandingPreferenceDataProvider() throws Exception {

        return new Object[][]{
                {"sample-preference-1.json"},
                {"sample-preference-without-isBrandingEnabled-config.json"},
                {"sample-preference-without-configs.json"},
        };
    }

    @Test(dataProvider = "publishedBrandingPreferenceDataProvider")
    public void testIsPublishedBranding(String preferenceFile) throws Exception {

        BrandingPreference brandingPreference = new BrandingPreference();
        brandingPreference.setType(ORGANIZATION_TYPE);
        brandingPreference.setName(SUPER_TENANT_DOMAIN_NAME);
        brandingPreference.setLocale(DEFAULT_LOCALE);
        brandingPreference.setPreference(getPreferenceFromFile(preferenceFile));

        boolean isBrandingPublished = BrandingPreferenceMgtUtils.isBrandingPublished(brandingPreference);
        Assert.assertTrue(isBrandingPublished);
    }

    @Test
    public void testIsPublishedBrandingWithUnpublishedPreferences() throws Exception {

        BrandingPreference brandingPreference = new BrandingPreference();
        brandingPreference.setType(ORGANIZATION_TYPE);
        brandingPreference.setName(SUPER_TENANT_DOMAIN_NAME);
        brandingPreference.setLocale(DEFAULT_LOCALE);
        brandingPreference.setPreference(getPreferenceFromFile("sample-unpublished-preference.json"));

        boolean isBrandingPublished = BrandingPreferenceMgtUtils.isBrandingPublished(brandingPreference);
        Assert.assertFalse(isBrandingPublished);
    }
}
