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

import org.mockito.MockedStatic;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.branding.preference.management.core.dao.CustomContentPersistentDAO;
import org.wso2.carbon.identity.branding.preference.management.core.dao.impl.CustomContentPersistentFactory;
import org.wso2.carbon.identity.branding.preference.management.core.exception.BrandingPreferenceMgtException;
import org.wso2.carbon.identity.branding.preference.management.core.model.BrandingPreference;
import org.wso2.carbon.identity.branding.preference.management.core.model.CustomLayoutContent;
import org.wso2.carbon.identity.common.testng.WithCarbonHome;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.DEFAULT_LOCALE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ORGANIZATION_TYPE;
import static org.wso2.carbon.identity.branding.preference.management.core.util.TestUtils.getPreferenceFromFile;

/**
 * Unit tests for BrandingPreferenceMgtUtils.
 */
@WithCarbonHome
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
        assertFalse(isBrandingPublished);
    }

    @DataProvider(name = "brandingPreferenceValidationDataProvider")
    public Object[][] brandingPreferenceValidationDataProvider() {

        return new Object[][] {
                {"invalid-preference", false},
                {"{\"theme\": \"{}\"}", true},
                {"{\"layout\": \"  \"}", false},
                {"{\"layout\": {}}", true},
                {"{\"layout\": {\"activeLayout\": \"centered\"}}", true},
                {"{\"layout\": {\"activeLayout\": \"custom\"}}", true},
                {"{\"layout\": {\"activeLayout\": \"custom\", \"content\": {}}}", false},
                {"{\"layout\": {\"activeLayout\": \"custom\", \"content\": {\"html\":\"  \"}}}", false},
                {"{\"layout\": {\"activeLayout\": \"custom\", \"content\": {\"html\":\"sample-html-content\"}}}",
                        false},
                {"{\"layout\": {\"activeLayout\": \"custom\", \"content\": {\"html\":\"{{{MainSection}}}\"}}}",
                        true},
                {"{\"layout\": {\"activeLayout\": \"custom\", \"content\": {\"html\":\"{{{   MainSection    }}}\"}}}",
                        true},
                {"{\"layout\": {\"activeLayout\": \"custom\", \"content\": {\"html\":\"{{{ \nMainSection}}}\"}}}",
                        false}
        };
    }

    @Test(description = "Test isValidBrandingPreference method",
            dataProvider = "brandingPreferenceValidationDataProvider")
    public void testIsValidBrandingPreference(String preference, boolean isValid) throws Exception {

        try {
            BrandingPreferenceMgtUtils.isValidBrandingPreference(preference, SUPER_TENANT_DOMAIN_NAME);
            assertTrue(isValid, "Valid preference should not throw an exception.");
        } catch (BrandingPreferenceMgtException e) {
            assertFalse(isValid, "Invalid preference should throw an exception.");
        }
    }

    @DataProvider(name = "resolveContentTypesDataProvider")
    public Object[][] resolveContentTypesDataProvider() {

        return new Object[][] {
            {"", "", "", null},
            {"sample-html", "", "", new String[]{"sample-html", null, null}},
            {"sample-html", "sample-css", "", new String[]{"sample-html", "sample-css", null}},
            {"sample-html", null, "sample-js", new String[]{"sample-html", null, "sample-js"}},
            {"sample-html", "sample-css", "sample-js", new String[]{"sample-html", "sample-css", "sample-js"}}
        };
    }

    @Test(description = "Test resolveContentTypes method", dataProvider = "resolveContentTypesDataProvider")
    public void testResolveContentTypes(String html, String css, String js, String[] expectedEntries) {

        CustomLayoutContent content = new CustomLayoutContent.CustomLayoutContentBuilder()
                .setHtml(html)
                .setCss(css)
                .setJs(js)
                .build();

        Map<String, String> result = BrandingPreferenceMgtUtils.resolveContentTypes(content);

        if (expectedEntries == null) {
            assertTrue(result.isEmpty(), "Expected result map should be empty.");
        } else {
            assertNotNull(result, "Result should not be null for valid content.");
            assertEquals(result.get("html"), expectedEntries[0], "HTML content mismatch.");
            assertEquals(result.get("css"), expectedEntries[1], "CSS content mismatch.");
            assertEquals(result.get("js"), expectedEntries[2], "JS content mismatch.");
        }
    }

    @DataProvider(name = "extractCustomLayoutContentDataProvider")
    public Object[][] extractCustomLayoutContentDataProvider() {

        Map<String, Object> preference1 = new LinkedHashMap<>();
        preference1.put("layout", null);

        Map<String, Object> preference2 = new LinkedHashMap<>();
        Map<String, Object> layout1 = new LinkedHashMap<>();
        layout1.put("activeLayout", "centered");
        preference2.put("layout", layout1);

        Map<String, Object> preference3 = new LinkedHashMap<>();
        Map<String, Object> layout2 = new LinkedHashMap<>();
        layout2.put("activeLayout", "custom");
        preference3.put("layout", layout2);

        Map<String, Object> preference4 = new LinkedHashMap<>();
        Map<String, Object> layout3 = new LinkedHashMap<>();
        layout3.put("activeLayout", "custom");
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("html", "{{{MainSection}}}");
        content.put("css", "body { color: red; }");
        content.put("js", "console.log('test');");
        layout3.put("content", content);
        preference4.put("layout", layout3);

        Map<String, Object> preference5 = new LinkedHashMap<>();
        Map<String, Object> layout4 = new LinkedHashMap<>();
        layout4.put("activeLayout", null);
        preference5.put("layout", layout4);

        return new Object[][] {
                { null, false },
                { preference1, false },
                { preference2, false },
                { preference3, false },
                { preference4, true },
                { preference5, false }
        };
    }

    @Test(description = "Test extractCustomLayoutContent method",
            dataProvider = "extractCustomLayoutContentDataProvider")
    public void testExtractCustomLayoutContent(Object preference, boolean isValid) throws Exception {

        CustomLayoutContent content = BrandingPreferenceMgtUtils.extractCustomLayoutContent(preference);
        if (isValid) {
            assertNotNull(content, "Content should not be null for valid preference.");
            assertEquals(content.getHtml(), "{{{MainSection}}}", "HTML content mismatch.");
            assertEquals(content.getCss(), "body { color: red; }", "CSS content mismatch.");
            assertEquals(content.getJs(), "console.log('test');", "JS content mismatch.");
        } else {
            assertNull(content, "Content should be null for invalid preference.");
        }
    }

    @DataProvider(name = "addCustomLayoutContentToPreferencesProvider")
    public Object[][] addCustomLayoutContentToPreferencesProvider() {

        Map<String, Object> preference1 = new LinkedHashMap<>();
        preference1.put("layout", null);

        Map<String, Object> preference2 = new LinkedHashMap<>();
        Map<String, Object> layout1 = new LinkedHashMap<>();
        layout1.put("activeLayout", "centered");
        preference2.put("layout", layout1);

        Map<String, Object> preference3 = new LinkedHashMap<>();
        Map<String, Object> layout2 = new LinkedHashMap<>();
        layout2.put("activeLayout", "custom");
        preference3.put("layout", layout2);

        Map<String, Object> preference4 = new LinkedHashMap<>();
        Map<String, Object> layout3 = new LinkedHashMap<>();
        layout3.put("activeLayout", null);
        preference4.put("layout", layout3);

        CustomLayoutContent customLayoutContent1 =
                new CustomLayoutContent.CustomLayoutContentBuilder().setHtml("test-html").setCss("test-css")
                        .setJs("test-js").build();
        CustomLayoutContent customLayoutContent2 =
                new CustomLayoutContent.CustomLayoutContentBuilder().setHtml("test-html").build();

        return new Object[][] {
                { null, false, null },
                { preference1, false, null },
                { preference2, false, null },
                { preference3, true, customLayoutContent1 },
                { preference3, true, customLayoutContent2 },
                { preference3, false, null },
                { preference4, false, null }
        };
    }

    @Test(description = "Test addCustomLayoutContentToPreferences method",
            dataProvider = "addCustomLayoutContentToPreferencesProvider")
    public void testAddCustomLayoutContentToPreferences(Object preference, boolean isValid,
                                                        CustomLayoutContent customLayoutContent) throws Exception {

        try (MockedStatic<CustomContentPersistentFactory> customContentPersistentFactoryMockedStatic = mockStatic(
                CustomContentPersistentFactory.class)) {

            Map<?, ?> preferenceMap = preference == null ? null : new LinkedHashMap<>((Map<?, ?>) preference);
            CustomContentPersistentDAO customContentPersistentDAO = mock(CustomContentPersistentDAO.class);
            when(customContentPersistentDAO.getCustomContent(anyString(), anyString())).thenReturn(customLayoutContent);
            customContentPersistentFactoryMockedStatic.when(
                            CustomContentPersistentFactory::getCustomContentPersistentDAO)
                    .thenReturn(customContentPersistentDAO);
            BrandingPreferenceMgtUtils.addCustomLayoutContentToPreferences(preferenceMap, "test-application-uuid",
                    SUPER_TENANT_DOMAIN_NAME);
            if (isValid) {
                Map<?, ?> content = (Map<?, ?>) ((Map<?, ?>) preferenceMap.get("layout")).get("content");
                assertEquals(content.get("html"), customLayoutContent.getHtml(), "HTML content mismatch.");
                assertEquals(content.get("css"), customLayoutContent.getCss(), "CSS content mismatch.");
                assertEquals(content.get("js"), customLayoutContent.getJs(), "JS content mismatch.");
            } else {
                assertEquals(preference, preferenceMap, "Preference map should not be modified.");
            }
        }
    }
}
