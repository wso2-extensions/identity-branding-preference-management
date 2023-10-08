/*
 * Copyright (c) (2022-2023), WSO2 LLC. (http://www.wso2.com).
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

import org.json.JSONObject;
import org.mockito.Mock;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.base.CarbonBaseConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.branding.preference.management.core.exception.BrandingPreferenceMgtClientException;
import org.wso2.carbon.identity.branding.preference.management.core.internal.BrandingPreferenceManagerComponentDataHolder;
import org.wso2.carbon.identity.branding.preference.management.core.model.BrandingPreference;
import org.wso2.carbon.identity.branding.preference.management.core.model.CustomText;
import org.wso2.carbon.identity.branding.preference.management.core.util.ConfigurationManagementUtils;
import org.wso2.carbon.identity.branding.preference.management.core.util.MockUIBrandingPreferenceResolver;
import org.wso2.carbon.identity.common.testng.WithH2Database;
import org.wso2.carbon.identity.common.testng.realm.InMemoryRealmService;
import org.wso2.carbon.identity.configuration.mgt.core.ConfigurationManager;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.services.IdentityEventService;
import org.wso2.carbon.user.core.UserStoreException;

import java.io.IOException;
import java.nio.file.Paths;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.testng.Assert.assertThrows;
import static org.wso2.carbon.base.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME;
import static org.wso2.carbon.base.MultitenantConstants.SUPER_TENANT_ID;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.DEFAULT_LOCALE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ORGANIZATION_TYPE;
import static org.wso2.carbon.identity.branding.preference.management.core.util.TestUtils.getPreferenceFromFile;

/**
 * Unit tests for BrandingPreferenceManagerImpl.
 */
@WithH2Database(files = {"dbscripts/config/h2.sql"})
public class BrandingPreferenceManagerImplTest {

    public static final int SAMPLE_TENANT_ID_ABC = 1;
    public static final String SAMPLE_TENANT_DOMAIN_NAME_ABC = "abc";
    public static final String LOGIN_SCREEN = "login";
    public static final String FRENCH_LOCALE = "fr-FR";

    @Mock
    IdentityEventService identityEventService;
    private BrandingPreferenceManagerImpl brandingPreferenceManagerImpl;

    @BeforeMethod
    public void setUp() throws Exception {

        initMocks(this);
        setCarbonHome();
        setCarbonContextForTenant(SUPER_TENANT_DOMAIN_NAME, SUPER_TENANT_ID);
        brandingPreferenceManagerImpl = new BrandingPreferenceManagerImpl();

        ConfigurationManager configurationManager = ConfigurationManagementUtils.getConfigurationManager();
        BrandingPreferenceManagerComponentDataHolder.getInstance().setConfigurationManager(configurationManager);

        BrandingPreferenceManagerComponentDataHolder.getInstance().setIdentityEventService(identityEventService);
        doNothing().when(identityEventService).handleEvent(any(Event.class));
    }

    @DataProvider(name = "brandingPreferenceDataProvider")
    public Object[][] brandingPreferenceDataProvider() throws Exception {

        BrandingPreference brandingPreference1 = new BrandingPreference();
        brandingPreference1.setType(ORGANIZATION_TYPE);
        brandingPreference1.setName(SUPER_TENANT_DOMAIN_NAME);
        brandingPreference1.setLocale(DEFAULT_LOCALE);
        brandingPreference1.setPreference(getPreferenceFromFile("sample-preference-1.json"));

        BrandingPreference brandingPreference2 = new BrandingPreference();
        brandingPreference2.setType(ORGANIZATION_TYPE);
        brandingPreference2.setName(SAMPLE_TENANT_DOMAIN_NAME_ABC);
        brandingPreference2.setLocale(DEFAULT_LOCALE);
        brandingPreference2.setPreference(getPreferenceFromFile("sample-preference-2.json"));

        return new Object[][]{
                {brandingPreference1, SUPER_TENANT_DOMAIN_NAME, SUPER_TENANT_ID},
                {brandingPreference2, SAMPLE_TENANT_DOMAIN_NAME_ABC, SAMPLE_TENANT_ID_ABC},
        };
    }

    @Test(dataProvider = "brandingPreferenceDataProvider")
    public void testAddBrandingPreference(Object brandingPreference, String tenantDomain, int tenantId)
            throws Exception {

        setCarbonContextForTenant(tenantDomain, tenantId);
        BrandingPreference inputBP = (BrandingPreference) brandingPreference;

        // Adding new branding preference.
        BrandingPreference addedBP = brandingPreferenceManagerImpl.addBrandingPreference(inputBP);
        Assert.assertEquals(addedBP.getPreference(), inputBP.getPreference());
        Assert.assertEquals(addedBP.getName(), inputBP.getName());

        //  Retrieving added branding preference.
        BrandingPreference retrievedBP = brandingPreferenceManagerImpl.getBrandingPreference
                (inputBP.getType(), inputBP.getName(), inputBP.getLocale());
        Assert.assertEquals(retrievedBP.getPreference(), inputBP.getPreference());
        Assert.assertEquals(retrievedBP.getName(), inputBP.getName());

        // Deleting added branding preference.
        brandingPreferenceManagerImpl.deleteBrandingPreference(inputBP.getType(), inputBP.getName(),
                inputBP.getLocale());
    }

    @Test(dataProvider = "brandingPreferenceDataProvider")
    public void testAddConflictBrandingPreference(Object brandingPreference, String tenantDomain, int tenantId)
            throws Exception {

        setCarbonContextForTenant(tenantDomain, tenantId);
        BrandingPreference inputBP = (BrandingPreference) brandingPreference;

        // Adding new branding preference.
        BrandingPreference addedBP = brandingPreferenceManagerImpl.addBrandingPreference(inputBP);
        Assert.assertEquals(addedBP.getPreference(), inputBP.getPreference());
        Assert.assertEquals(addedBP.getName(), inputBP.getName());

        //  Adding conflicting branding preference.
        assertThrows(BrandingPreferenceMgtClientException.class, () -> brandingPreferenceManagerImpl
                .addBrandingPreference(inputBP));

        // Deleting added branding preference.
        brandingPreferenceManagerImpl.deleteBrandingPreference(inputBP.getType(), inputBP.getName(),
                inputBP.getLocale());
    }

    @DataProvider(name = "invalidBrandingPreferenceDataProvider")
    public Object[][] invalidBrandingPreferenceDataProvider() {

        BrandingPreference brandingPreference1 = new BrandingPreference();
        brandingPreference1.setType(ORGANIZATION_TYPE);
        brandingPreference1.setName(SUPER_TENANT_DOMAIN_NAME);
        brandingPreference1.setLocale(DEFAULT_LOCALE);
        brandingPreference1.setPreference(1234);

        BrandingPreference brandingPreference2 = new BrandingPreference();
        brandingPreference2.setType(ORGANIZATION_TYPE);
        brandingPreference2.setName(SUPER_TENANT_DOMAIN_NAME);
        brandingPreference2.setLocale(DEFAULT_LOCALE);
        brandingPreference2.setPreference("Branding Preference");

        BrandingPreference brandingPreference3 = new BrandingPreference();
        brandingPreference3.setType(ORGANIZATION_TYPE);
        brandingPreference3.setName(SUPER_TENANT_DOMAIN_NAME);
        brandingPreference3.setLocale(DEFAULT_LOCALE);
        brandingPreference3.setPreference(new JSONObject());

        return new Object[][]{
                {brandingPreference1},
                {brandingPreference2},
                {brandingPreference3},
        };
    }

    @Test(dataProvider = "invalidBrandingPreferenceDataProvider")
    public void testAddInvalidBrandingPreference(Object brandingPreference) {

        BrandingPreference inputBP = (BrandingPreference) brandingPreference;

        // Adding new branding preference.
        assertThrows(BrandingPreferenceMgtClientException.class, () -> brandingPreferenceManagerImpl
                .addBrandingPreference(inputBP));
    }

    @Test(dataProvider = "brandingPreferenceDataProvider")
    public void testGetBrandingPreference(Object brandingPreference, String tenantDomain, int tenantId)
            throws Exception {

        setCarbonContextForTenant(tenantDomain, tenantId);
        BrandingPreference inputBP = (BrandingPreference) brandingPreference;

        // Adding new branding preference.
        BrandingPreference addedBP = brandingPreferenceManagerImpl.addBrandingPreference(inputBP);

        //  Retrieving added branding preference.
        BrandingPreference retrievedBP = brandingPreferenceManagerImpl.getBrandingPreference
                (inputBP.getType(), inputBP.getName(), inputBP.getLocale());
        Assert.assertEquals(retrievedBP.getPreference(), inputBP.getPreference());
        Assert.assertEquals(retrievedBP.getName(), inputBP.getName());
        Assert.assertEquals(retrievedBP.getType(), inputBP.getType());
        Assert.assertEquals(retrievedBP.getLocale(), inputBP.getLocale());

        // Deleting added branding preference.
        brandingPreferenceManagerImpl.deleteBrandingPreference
                (inputBP.getType(), inputBP.getName(), inputBP.getLocale());
    }

    @Test(dataProvider = "brandingPreferenceDataProvider")
    public void testResolveBrandingPreference(Object brandingPreference, String tenantDomain, int tenantId)
            throws Exception {

        setCarbonContextForTenant(tenantDomain, tenantId);
        BrandingPreference inputBP = (BrandingPreference) brandingPreference;

        // Adding new branding preference.
        brandingPreferenceManagerImpl.addBrandingPreference(inputBP);

        //  Retrieving added branding preference.
        BrandingPreference retrievedBP =
                brandingPreferenceManagerImpl.resolveBrandingPreference(inputBP.getType(), inputBP.getName(),
                        inputBP.getLocale());
        Assert.assertEquals(retrievedBP.getPreference(), inputBP.getPreference());
        Assert.assertEquals(retrievedBP.getName(), inputBP.getName());
        Assert.assertEquals(retrievedBP.getType(), inputBP.getType());
        Assert.assertEquals(retrievedBP.getLocale(), inputBP.getLocale());

        // Deleting added branding preference.
        brandingPreferenceManagerImpl.deleteBrandingPreference
                (inputBP.getType(), inputBP.getName(), inputBP.getLocale());
    }

    @Test(dataProvider = "brandingPreferenceDataProvider")
    public void testResolveBrandingPreferenceWithResolver(Object brandingPreference, String tenantDomain, int tenantId)
            throws Exception {

        setCarbonContextForTenant(tenantDomain, tenantId);
        BrandingPreference inputBP = (BrandingPreference) brandingPreference;

        MockUIBrandingPreferenceResolver resolver = new MockUIBrandingPreferenceResolver();
        resolver.setBranding(inputBP);
        BrandingPreferenceManagerComponentDataHolder.getInstance().setUiBrandingPreferenceResolver(resolver);

        BrandingPreference retrievedBP =
                brandingPreferenceManagerImpl.resolveBrandingPreference(inputBP.getType(), inputBP.getName(),
                        inputBP.getLocale());
        Assert.assertEquals(retrievedBP.getPreference(), inputBP.getPreference());
        Assert.assertEquals(retrievedBP.getName(), inputBP.getName());
        Assert.assertEquals(retrievedBP.getType(), inputBP.getType());
        Assert.assertEquals(retrievedBP.getLocale(), inputBP.getLocale());

        BrandingPreferenceManagerComponentDataHolder.getInstance().setUiBrandingPreferenceResolver(null);
    }

    @DataProvider(name = "notExistingBrandingPreferenceDataProvider")
    public Object[][] notExistingBrandingPreferenceDataProvider() {

        return new Object[][]{
                {SUPER_TENANT_DOMAIN_NAME, SUPER_TENANT_ID},
                {SAMPLE_TENANT_DOMAIN_NAME_ABC, SAMPLE_TENANT_ID_ABC},
        };
    }

    @Test(dataProvider = "notExistingBrandingPreferenceDataProvider")
    public void testGetNotExistingBrandingPreference(String tenantDomain, int tenantId) throws Exception {

        setCarbonContextForTenant(tenantDomain, tenantId);
        assertThrows(BrandingPreferenceMgtClientException.class, () -> brandingPreferenceManagerImpl
                .getBrandingPreference(ORGANIZATION_TYPE, SUPER_TENANT_DOMAIN_NAME, DEFAULT_LOCALE));
    }

    @DataProvider(name = "replaceBrandingPreferenceDataProvider")
    public Object[][] replaceBrandingPreferenceDataProvider() throws Exception {

        BrandingPreference brandingPreference1 = new BrandingPreference();
        brandingPreference1.setType(ORGANIZATION_TYPE);
        brandingPreference1.setName(SUPER_TENANT_DOMAIN_NAME);
        brandingPreference1.setLocale(DEFAULT_LOCALE);
        brandingPreference1.setPreference(getPreferenceFromFile("sample-preference-1.json"));

        BrandingPreference newBrandingPreference1 = new BrandingPreference();
        newBrandingPreference1.setType(ORGANIZATION_TYPE);
        newBrandingPreference1.setName(SUPER_TENANT_DOMAIN_NAME);
        newBrandingPreference1.setLocale(DEFAULT_LOCALE);
        newBrandingPreference1.setPreference(getPreferenceFromFile("sample-preference-1.json"));

        BrandingPreference brandingPreference2 = new BrandingPreference();
        brandingPreference2.setType(ORGANIZATION_TYPE);
        brandingPreference2.setName(SAMPLE_TENANT_DOMAIN_NAME_ABC);
        brandingPreference2.setLocale(DEFAULT_LOCALE);
        brandingPreference2.setPreference(getPreferenceFromFile("sample-preference-2.json"));

        BrandingPreference newBrandingPreference2 = new BrandingPreference();
        newBrandingPreference2.setType(ORGANIZATION_TYPE);
        newBrandingPreference2.setName(SAMPLE_TENANT_DOMAIN_NAME_ABC);
        newBrandingPreference2.setLocale(DEFAULT_LOCALE);
        newBrandingPreference2.setPreference(getPreferenceFromFile("sample-preference-1.json"));

        return new Object[][]{
                {brandingPreference1, newBrandingPreference1, SUPER_TENANT_DOMAIN_NAME, SUPER_TENANT_ID},
                {brandingPreference2, newBrandingPreference2, SAMPLE_TENANT_DOMAIN_NAME_ABC, SAMPLE_TENANT_ID_ABC},
        };
    }

    @Test(dataProvider = "replaceBrandingPreferenceDataProvider")
    public void testReplaceBrandingPreference(Object brandingPreference, Object newBrandingPreference,
                                              String tenantDomain, int tenantId) throws Exception {

        setCarbonContextForTenant(tenantDomain, tenantId);
        BrandingPreference inputBP = (BrandingPreference) brandingPreference;
        BrandingPreference newBP = (BrandingPreference) newBrandingPreference;

        // Adding new branding preference.
        BrandingPreference addedBP = brandingPreferenceManagerImpl.addBrandingPreference(inputBP);
        Assert.assertEquals(addedBP.getPreference(), inputBP.getPreference());

        BrandingPreference updatedBP = brandingPreferenceManagerImpl.replaceBrandingPreference(newBP);
        Assert.assertEquals(updatedBP.getPreference(), newBP.getPreference());

        //  Retrieving updated branding preference.
        BrandingPreference retrievedBP = brandingPreferenceManagerImpl.getBrandingPreference
                (newBP.getType(), newBP.getName(), newBP.getLocale());
        Assert.assertEquals(retrievedBP.getPreference(), newBP.getPreference());
        Assert.assertEquals(retrievedBP.getName(), newBP.getName());
        Assert.assertEquals(retrievedBP.getType(), newBP.getType());
        Assert.assertEquals(retrievedBP.getLocale(), newBP.getLocale());

        // Deleting added branding preference.
        brandingPreferenceManagerImpl.deleteBrandingPreference
                (newBP.getType(), newBP.getName(), newBP.getLocale());
    }

    @Test(dataProvider = "brandingPreferenceDataProvider")
    public void testReplaceNotExistingBrandingPreference(Object brandingPreference, String tenantDomain, int tenantId)
            throws Exception {

        setCarbonContextForTenant(tenantDomain, tenantId);
        BrandingPreference newBP = (BrandingPreference) brandingPreference;

        assertThrows(BrandingPreferenceMgtClientException.class, () -> brandingPreferenceManagerImpl
                .replaceBrandingPreference(newBP));
    }

    @Test(dataProvider = "brandingPreferenceDataProvider")
    public void testDeleteBrandingPreference(Object brandingPreference, String tenantDomain, int tenantId)
            throws Exception {

        setCarbonContextForTenant(tenantDomain, tenantId);
        BrandingPreference inputBP = (BrandingPreference) brandingPreference;

        // Adding new branding preference.
        BrandingPreference addedBP = brandingPreferenceManagerImpl.addBrandingPreference(inputBP);
        Assert.assertEquals(addedBP.getPreference(), inputBP.getPreference());
        Assert.assertEquals(addedBP.getName(), inputBP.getName());

        // Deleting added branding preference.
        brandingPreferenceManagerImpl.deleteBrandingPreference
                (inputBP.getType(), inputBP.getName(), inputBP.getLocale());

        // Retrieving deleted branding preference.
        assertThrows(BrandingPreferenceMgtClientException.class, () -> brandingPreferenceManagerImpl
                .getBrandingPreference(inputBP.getType(), inputBP.getName(), inputBP.getLocale()));
    }

    @Test(dataProvider = "notExistingBrandingPreferenceDataProvider")
    public void testDeleteNotExistingBrandingPreference(String tenantDomain, int tenantId) throws Exception {

        setCarbonContextForTenant(tenantDomain, tenantId);
        assertThrows(BrandingPreferenceMgtClientException.class, () -> brandingPreferenceManagerImpl
                .deleteBrandingPreference(ORGANIZATION_TYPE, SUPER_TENANT_DOMAIN_NAME, DEFAULT_LOCALE));
    }

    @DataProvider(name = "customTextPreferenceDataProvider")
    public Object[][] customTextPreferenceDataProvider() throws Exception {

        CustomText customText1 = new CustomText();
        customText1.setType(ORGANIZATION_TYPE);
        customText1.setName(SUPER_TENANT_DOMAIN_NAME);
        customText1.setScreen(LOGIN_SCREEN);
        customText1.setLocale(DEFAULT_LOCALE);
        customText1.setPreference(getPreferenceFromFile("sample-text-customization-1.json"));

        CustomText customText2 = new CustomText();
        customText2.setType(ORGANIZATION_TYPE);
        customText2.setName(SAMPLE_TENANT_DOMAIN_NAME_ABC);
        customText2.setScreen(LOGIN_SCREEN);
        customText2.setLocale(FRENCH_LOCALE);
        customText2.setPreference(getPreferenceFromFile("sample-text-customization-2.json"));

        return new Object[][]{
                {customText1, SUPER_TENANT_DOMAIN_NAME, SUPER_TENANT_ID},
                {customText2, SAMPLE_TENANT_DOMAIN_NAME_ABC, SAMPLE_TENANT_ID_ABC},
        };
    }

    @Test(dataProvider = "customTextPreferenceDataProvider")
    public void testAddCustomText(Object customText, String tenantDomain, int tenantId)
            throws Exception {

        setCarbonContextForTenant(tenantDomain, tenantId);
        CustomText inputCT = (CustomText) customText;

        // Adding new custom text preference.
        CustomText addedCT = brandingPreferenceManagerImpl.addCustomText(inputCT);
        Assert.assertEquals(addedCT.getPreference(), inputCT.getPreference());
        Assert.assertEquals(addedCT.getName(), inputCT.getName());
        Assert.assertEquals(addedCT.getLocale(), inputCT.getLocale());
        Assert.assertEquals(addedCT.getScreen(), inputCT.getScreen());

        // Retrieving added custom text preference.
        CustomText retrievedCT = brandingPreferenceManagerImpl.getCustomText
                (inputCT.getType(), inputCT.getName(), inputCT.getScreen(), inputCT.getLocale());
        Assert.assertEquals(retrievedCT.getPreference(), inputCT.getPreference());
        Assert.assertEquals(retrievedCT.getName(), inputCT.getName());
        Assert.assertEquals(retrievedCT.getType(), inputCT.getType());
        Assert.assertEquals(retrievedCT.getScreen(), inputCT.getScreen());
        Assert.assertEquals(retrievedCT.getLocale(), inputCT.getLocale());

        // Deleting added custom text preference.
        brandingPreferenceManagerImpl.deleteCustomText(inputCT.getType(), inputCT.getName(), inputCT.getScreen(),
                inputCT.getLocale());

        // Retrieving deleted custom text preference.
        assertThrows(BrandingPreferenceMgtClientException.class, () -> brandingPreferenceManagerImpl
                .getCustomText(inputCT.getType(), inputCT.getName(), inputCT.getScreen(), inputCT.getLocale()));
    }

    @DataProvider(name = "invalidCustomTextPreferenceDataProvider")
    public Object[][] invalidCustomTextPreferenceDataProvider() throws IOException {

        CustomText customText1 = new CustomText();
        customText1.setType(ORGANIZATION_TYPE);
        customText1.setName(SUPER_TENANT_DOMAIN_NAME);
        customText1.setScreen(LOGIN_SCREEN);
        customText1.setLocale(DEFAULT_LOCALE);
        customText1.setPreference(1234);

        CustomText customText2 = new CustomText();
        customText2.setType(ORGANIZATION_TYPE);
        customText2.setName(SUPER_TENANT_DOMAIN_NAME);
        customText2.setScreen(LOGIN_SCREEN);
        customText2.setLocale(DEFAULT_LOCALE);
        customText2.setPreference("Branding Preference");

        CustomText customText3 = new CustomText();
        customText3.setType(ORGANIZATION_TYPE);
        customText3.setName(SUPER_TENANT_DOMAIN_NAME);
        customText3.setScreen(LOGIN_SCREEN);
        customText3.setLocale(DEFAULT_LOCALE);
        customText3.setPreference(new JSONObject());

        return new Object[][]{
                {customText1},
                {customText2},
                {customText3},
        };
    }

    @Test(dataProvider = "invalidCustomTextPreferenceDataProvider")
    public void testAddInvaliCustomTextPreference(Object customText) {

        CustomText inputCT = (CustomText) customText;

        // Adding new custom text preference.
        assertThrows(BrandingPreferenceMgtClientException.class, () -> brandingPreferenceManagerImpl
                .addCustomText(inputCT));
    }

    @Test(dataProvider = "customTextPreferenceDataProvider")
    public void testResolveCustomTextPreference(Object customText, String tenantDomain, int tenantId)
            throws Exception {

        setCarbonContextForTenant(tenantDomain, tenantId);
        CustomText inputCT = (CustomText) customText;

        // Adding new custom text preference.
        brandingPreferenceManagerImpl.addCustomText(inputCT);

        // Retrieving added custom text preference.
        CustomText retrievedCT =
                brandingPreferenceManagerImpl.resolveCustomText(inputCT.getType(), inputCT.getName(),
                        inputCT.getScreen(), inputCT.getLocale());
        Assert.assertEquals(retrievedCT.getPreference(), inputCT.getPreference());
        Assert.assertEquals(retrievedCT.getName(), inputCT.getName());
        Assert.assertEquals(retrievedCT.getType(), inputCT.getType());
        Assert.assertEquals(retrievedCT.getScreen(), inputCT.getScreen());
        Assert.assertEquals(retrievedCT.getLocale(), inputCT.getLocale());

        // Deleting added custom text preference.
        brandingPreferenceManagerImpl.deleteCustomText
                (inputCT.getType(), inputCT.getName(), inputCT.getScreen(), inputCT.getLocale());
    }

    @Test(dataProvider = "customTextPreferenceDataProvider")
    public void testResolveCustomTextWithResolver(Object customText, String tenantDomain, int tenantId)
            throws Exception {

        setCarbonContextForTenant(tenantDomain, tenantId);
        CustomText inputCT = (CustomText) customText;

        MockUIBrandingPreferenceResolver resolver = new MockUIBrandingPreferenceResolver();
        resolver.setCustomText(inputCT);
        BrandingPreferenceManagerComponentDataHolder.getInstance().setUiBrandingPreferenceResolver(resolver);

        CustomText retrievedCT =
                brandingPreferenceManagerImpl.resolveCustomText(inputCT.getType(), inputCT.getName(),
                        inputCT.getScreen(), inputCT.getLocale());
        Assert.assertEquals(retrievedCT.getPreference(), inputCT.getPreference());
        Assert.assertEquals(retrievedCT.getName(), inputCT.getName());
        Assert.assertEquals(retrievedCT.getType(), inputCT.getType());
        Assert.assertEquals(retrievedCT.getScreen(), inputCT.getScreen());
        Assert.assertEquals(retrievedCT.getLocale(), inputCT.getLocale());

        BrandingPreferenceManagerComponentDataHolder.getInstance().setUiBrandingPreferenceResolver(null);
    }

    @DataProvider(name = "notExistingCustomTextDataProvider")
    public Object[][] notExistingCustomTextDataProvider() {

        return new Object[][]{
                {SUPER_TENANT_DOMAIN_NAME, SUPER_TENANT_ID},
                {SAMPLE_TENANT_DOMAIN_NAME_ABC, SAMPLE_TENANT_ID_ABC},
        };
    }

    @Test(dataProvider = "notExistingCustomTextDataProvider")
    public void testGetNotExistingCustomText(String tenantDomain, int tenantId) throws Exception {

        setCarbonContextForTenant(tenantDomain, tenantId);
        assertThrows(BrandingPreferenceMgtClientException.class, () -> brandingPreferenceManagerImpl
                .getCustomText(ORGANIZATION_TYPE, SUPER_TENANT_DOMAIN_NAME, LOGIN_SCREEN, DEFAULT_LOCALE));
    }

    @DataProvider(name = "replaceCustomTextDataProvider")
    public Object[][] replaceCustomTextDataProvider() throws Exception {

        CustomText customText1 = new CustomText();
        customText1.setType(ORGANIZATION_TYPE);
        customText1.setName(SUPER_TENANT_DOMAIN_NAME);
        customText1.setScreen(LOGIN_SCREEN);
        customText1.setLocale(DEFAULT_LOCALE);
        customText1.setPreference(getPreferenceFromFile("sample-preference-1.json"));

        CustomText newCustomText1 = new CustomText();
        newCustomText1.setType(ORGANIZATION_TYPE);
        newCustomText1.setName(SUPER_TENANT_DOMAIN_NAME);
        newCustomText1.setScreen(LOGIN_SCREEN);
        newCustomText1.setLocale(DEFAULT_LOCALE);
        newCustomText1.setPreference(getPreferenceFromFile("sample-preference-2.json"));

        CustomText customText2 = new CustomText();
        customText2.setType(ORGANIZATION_TYPE);
        customText2.setName(SUPER_TENANT_DOMAIN_NAME);
        customText2.setScreen(LOGIN_SCREEN);
        customText2.setLocale(DEFAULT_LOCALE);
        customText2.setPreference(getPreferenceFromFile("sample-preference-2.json"));

        CustomText newCustomText2 = new CustomText();
        newCustomText2.setType(ORGANIZATION_TYPE);
        newCustomText2.setName(SUPER_TENANT_DOMAIN_NAME);
        newCustomText2.setScreen(LOGIN_SCREEN);
        newCustomText2.setLocale(DEFAULT_LOCALE);
        newCustomText2.setPreference(getPreferenceFromFile("sample-preference-1.json"));

        return new Object[][]{
                {customText1, newCustomText1, SUPER_TENANT_DOMAIN_NAME, SUPER_TENANT_ID},
                {customText1, newCustomText2, SAMPLE_TENANT_DOMAIN_NAME_ABC, SAMPLE_TENANT_ID_ABC},
        };
    }

    @Test(dataProvider = "replaceCustomTextDataProvider")
    public void testReplaceCustomText(Object customText, Object newCustomText, String tenantDomain, int tenantId)
            throws Exception {

        setCarbonContextForTenant(tenantDomain, tenantId);
        CustomText inputCT = (CustomText) customText;
        CustomText newCT = (CustomText) newCustomText;

        // Adding new custom text preference.
        CustomText addedCT = brandingPreferenceManagerImpl.addCustomText(inputCT);
        Assert.assertEquals(addedCT.getPreference(), inputCT.getPreference());

        CustomText updatedCT = brandingPreferenceManagerImpl.replaceCustomText(newCT);
        Assert.assertEquals(updatedCT.getPreference(), newCT.getPreference());

        // Retrieving updated custom text preference.
        CustomText retrievedBP = brandingPreferenceManagerImpl.getCustomText
                (newCT.getType(), newCT.getName(), newCT.getScreen(), newCT.getLocale());
        Assert.assertEquals(retrievedBP.getPreference(), newCT.getPreference());
        Assert.assertEquals(retrievedBP.getName(), newCT.getName());
        Assert.assertEquals(retrievedBP.getType(), newCT.getType());
        Assert.assertEquals(retrievedBP.getLocale(), newCT.getLocale());

        // Deleting added custom text preference.
        brandingPreferenceManagerImpl.deleteCustomText
                (newCT.getType(), newCT.getName(), newCT.getScreen(), newCT.getLocale());
    }

    @Test(dataProvider = "customTextPreferenceDataProvider")
    public void testReplaceNotExistingCustomText(Object customText, String tenantDomain, int tenantId)
            throws Exception {

        setCarbonContextForTenant(tenantDomain, tenantId);
        CustomText newCT = (CustomText) customText;

        assertThrows(BrandingPreferenceMgtClientException.class, () -> brandingPreferenceManagerImpl
                .replaceCustomText(newCT));
    }

    @Test(dataProvider = "notExistingCustomTextDataProvider")
    public void testDeleteNotExistingCustomData(String tenantDomain, int tenantId) throws Exception {

        setCarbonContextForTenant(tenantDomain, tenantId);
        assertThrows(BrandingPreferenceMgtClientException.class, () -> brandingPreferenceManagerImpl
                .deleteCustomText(ORGANIZATION_TYPE, SUPER_TENANT_DOMAIN_NAME, LOGIN_SCREEN, DEFAULT_LOCALE));
    }

    private void setCarbonContextForTenant(String tenantDomain, int tenantId) throws UserStoreException {

        PrivilegedCarbonContext.startTenantFlow();
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain);
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(tenantId);
        InMemoryRealmService testSessionRealmService = new InMemoryRealmService(tenantId);
        IdentityTenantUtil.setRealmService(testSessionRealmService);
    }

    private void setCarbonHome() {

        String carbonHome = Paths.get(System.getProperty("user.dir"), "target", "test-classes", "repository").
                toString();
        System.setProperty(CarbonBaseConstants.CARBON_HOME, carbonHome);
        System.setProperty(CarbonBaseConstants.CARBON_CONFIG_DIR_PATH, Paths.get(carbonHome, "conf").toString());
    }
}
