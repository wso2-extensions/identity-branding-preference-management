/*
 * Copyright (c) 2022-2024, WSO2 LLC. (http://www.wso2.com).
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

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.base.CarbonBaseConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.branding.preference.management.core.dao.CustomContentPersistentDAO;
import org.wso2.carbon.identity.branding.preference.management.core.dao.impl.CustomContentPersistentFactory;
import org.wso2.carbon.identity.branding.preference.management.core.exception.BrandingPreferenceMgtClientException;
import org.wso2.carbon.identity.branding.preference.management.core.exception.BrandingPreferenceMgtServerException;
import org.wso2.carbon.identity.branding.preference.management.core.internal.BrandingPreferenceManagerComponentDataHolder;
import org.wso2.carbon.identity.branding.preference.management.core.model.BrandingPreference;
import org.wso2.carbon.identity.branding.preference.management.core.model.CustomLayoutContent;
import org.wso2.carbon.identity.branding.preference.management.core.model.CustomText;
import org.wso2.carbon.identity.branding.preference.management.core.util.ConfigurationManagementUtils;
import org.wso2.carbon.identity.common.testng.WithH2Database;
import org.wso2.carbon.identity.common.testng.realm.InMemoryRealmService;
import org.wso2.carbon.identity.configuration.mgt.core.ConfigurationManager;
import org.wso2.carbon.identity.configuration.mgt.core.exception.ConfigurationManagementClientException;
import org.wso2.carbon.identity.configuration.mgt.core.exception.ConfigurationManagementException;
import org.wso2.carbon.identity.configuration.mgt.core.model.Resource;
import org.wso2.carbon.identity.configuration.mgt.core.model.ResourceFile;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.services.IdentityEventService;
import org.wso2.carbon.user.core.UserStoreException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;
import static org.wso2.carbon.base.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME;
import static org.wso2.carbon.base.MultitenantConstants.SUPER_TENANT_ID;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.APPLICATION_TYPE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.DEFAULT_LOCALE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ORGANIZATION_TYPE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.RESOURCE_ALREADY_EXISTS_ERROR_CODE;
import static org.wso2.carbon.identity.branding.preference.management.core.util.TestUtils.getPreferenceFromFile;

/**
 * Unit tests for BrandingPreferenceManagerImpl.
 */
@WithH2Database(files = {"dbscripts/config/h2.sql"})
public class BrandingPreferenceManagerImplTest {

    public static final int SAMPLE_TENANT_ID_ABC = 1;
    public static final String SAMPLE_TENANT_DOMAIN_NAME_ABC = "abc";
    public static final String SAMPLE_APPLICATION_ID_1 = "550e8400-e29b-41d4-a716-446655440000";
    public static final String SAMPLE_APPLICATION_ID_2 = "550e8400-e29b-41d4-a716-446655440001";
    public static final String SAMPLE_APPLICATION_ID_3 = "550e8400-e29b-41d4-a716-446655440002";
    public static final String LOGIN_SCREEN = "login";
    public static final String FRENCH_LOCALE = "fr-FR";

    @Mock
    IdentityEventService identityEventService;
    @Mock
    UIBrandingPreferenceResolver resolver;

    private BrandingPreferenceManagerImpl brandingPreferenceManagerImpl;

    @BeforeMethod
    public void setUp() throws Exception {

        initMocks(this);
        setCarbonHome();
        setCarbonContextForTenant(SUPER_TENANT_DOMAIN_NAME, SUPER_TENANT_ID);
        brandingPreferenceManagerImpl = new BrandingPreferenceManagerImpl();

        BrandingPreferenceManagerComponentDataHolder.getInstance().setUiBrandingPreferenceResolver(resolver);
        doNothing().when(resolver).clearBrandingResolverCacheHierarchy(any(), any(), any());
        doNothing().when(resolver).clearCustomTextResolverCacheHierarchy(any(), any(), any());

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

        BrandingPreference brandingPreference3 = new BrandingPreference();
        brandingPreference3.setType(APPLICATION_TYPE);
        brandingPreference3.setName(SAMPLE_APPLICATION_ID_1);
        brandingPreference3.setLocale(DEFAULT_LOCALE);
        brandingPreference3.setPreference(getPreferenceFromFile("sample-preference-2.json"));

        return new Object[][]{
                {brandingPreference1, SUPER_TENANT_DOMAIN_NAME, SUPER_TENANT_ID},
                {brandingPreference2, SAMPLE_TENANT_DOMAIN_NAME_ABC, SAMPLE_TENANT_ID_ABC},
                {brandingPreference3, SAMPLE_TENANT_DOMAIN_NAME_ABC, SAMPLE_TENANT_ID_ABC},
        };
    }

    @DataProvider(name = "applicationBrandingPreferenceDataProvider")
    public Object[][] applicationBrandingPreferenceDataProvider() throws Exception {

        BrandingPreference brandingPreference1 = new BrandingPreference();
        brandingPreference1.setType(APPLICATION_TYPE);
        brandingPreference1.setName(SAMPLE_APPLICATION_ID_1);
        brandingPreference1.setLocale(DEFAULT_LOCALE);
        brandingPreference1.setPreference(getPreferenceFromFile("sample-preference-1.json"));

        BrandingPreference brandingPreference2 = new BrandingPreference();
        brandingPreference2.setType(APPLICATION_TYPE);
        brandingPreference2.setName(SAMPLE_APPLICATION_ID_2);
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
        // Verify that clearBrandingResolverCacheHierarchy is called once and only once.
        verify(resolver, times(1)).clearBrandingResolverCacheHierarchy(any(), any(), any());

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
        // Verify that clearBrandingResolverCacheHierarchy is called once and only once.
        verify(resolver, times(1)).clearBrandingResolverCacheHierarchy(any(), any(), any());

        //  Adding conflicting branding preference.
        assertThrows(BrandingPreferenceMgtClientException.class, () -> brandingPreferenceManagerImpl
                .addBrandingPreference(inputBP));
        // Verify that clearBrandingResolverCacheHierarchy is not called after the conflicting addition.
        verify(resolver, times(1)).clearBrandingResolverCacheHierarchy(any(), any(), any());

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

        BrandingPreference brandingPreference4 = new BrandingPreference();
        brandingPreference3.setType(ORGANIZATION_TYPE);
        brandingPreference3.setName(SUPER_TENANT_DOMAIN_NAME);
        brandingPreference3.setLocale(DEFAULT_LOCALE);
        JSONObject preference = new JSONObject();
        JSONObject layout = new JSONObject();
        layout.put("activeLayout", "custom");
        layout.put("content", new JSONObject());
        preference.put("layout", layout);
        brandingPreference3.setPreference(preference);

        return new Object[][]{
                {brandingPreference1},
                {brandingPreference2},
                {brandingPreference3},
                {brandingPreference4},
        };
    }

    @Test(dataProvider = "invalidBrandingPreferenceDataProvider")
    public void testAddInvalidBrandingPreference(Object brandingPreference) throws Exception {

        BrandingPreference inputBP = (BrandingPreference) brandingPreference;

        // Adding new branding preference.
        assertThrows(BrandingPreferenceMgtClientException.class, () -> brandingPreferenceManagerImpl
                .addBrandingPreference(inputBP));
        // Verify that clearBrandingResolverCacheHierarchy is never called.
        verify(resolver, never()).clearBrandingResolverCacheHierarchy(any(), any(), any());
    }

    @Test(description = "Test the transaction errors while adding branding preference.")
    public void testTransactionErrorsWhileAddingBrandingPreference() throws Exception {

        BrandingPreference brandingPreference = new BrandingPreference();
        brandingPreference.setType(APPLICATION_TYPE);
        brandingPreference.setName(SAMPLE_APPLICATION_ID_3);
        brandingPreference.setLocale(DEFAULT_LOCALE);
        brandingPreference.setPreference(getPreferenceFromFile("sample-preference-2.json"));

        // Test the failure of adding custom layout content.
        try (MockedStatic<CustomContentPersistentFactory> mockedCustomContentPersistentFactory = mockStatic(
                CustomContentPersistentFactory.class)) {
            CustomContentPersistentDAO customContentPersistentDAO = mock(CustomContentPersistentDAO.class);
            mockedCustomContentPersistentFactory.when(CustomContentPersistentFactory::getCustomContentPersistentDAO)
                    .thenReturn(customContentPersistentDAO);
            doThrow(BrandingPreferenceMgtServerException.class).when(customContentPersistentDAO)
                    .addCustomContent(any(CustomLayoutContent.class), anyString(), anyString());
            assertThrows(BrandingPreferenceMgtServerException.class,
                    () -> brandingPreferenceManagerImpl.addBrandingPreference(brandingPreference));
            // Verify transaction rollback.
            try {
                brandingPreferenceManagerImpl.getBrandingPreference(APPLICATION_TYPE, SAMPLE_APPLICATION_ID_3,
                        DEFAULT_LOCALE);
            } catch (BrandingPreferenceMgtClientException e) {
                assertEquals(e.getMessage(), "Branding preferences are not configured for type: APP, name: " +
                        "550e8400-e29b-41d4-a716-446655440002 in tenant: carbon.super.");
            }
        }

        // Test the failure of adding branding config into configuration store.
        ConfigurationManager configurationManager = mock(ConfigurationManager.class);
        BrandingPreferenceManagerComponentDataHolder.getInstance().setConfigurationManager(configurationManager);
        doThrow(ConfigurationManagementException.class).when(configurationManager)
                .addResource(anyString(), any(Resource.class));
        assertThrows(BrandingPreferenceMgtServerException.class,
                () -> brandingPreferenceManagerImpl.addBrandingPreference(brandingPreference));
        doThrow(new ConfigurationManagementClientException("error", RESOURCE_ALREADY_EXISTS_ERROR_CODE)).when(
                configurationManager).addResource(anyString(), any(Resource.class));
        assertThrows(BrandingPreferenceMgtClientException.class,
                () -> brandingPreferenceManagerImpl.addBrandingPreference(brandingPreference));
        doThrow(RuntimeException.class).when(configurationManager).addResource(anyString(), any(Resource.class));
        assertThrows(BrandingPreferenceMgtServerException.class,
                () -> brandingPreferenceManagerImpl.addBrandingPreference(brandingPreference));
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
        when(resolver.resolveBranding(inputBP.getType(), inputBP.getName(), inputBP.getLocale(), false))
                .thenReturn(inputBP);

        BrandingPreference retrievedBP =
                brandingPreferenceManagerImpl.resolveBrandingPreference(inputBP.getType(), inputBP.getName(),
                        inputBP.getLocale(), false);
        Assert.assertEquals(retrievedBP.getPreference(), inputBP.getPreference());
        Assert.assertEquals(retrievedBP.getName(), inputBP.getName());
        Assert.assertEquals(retrievedBP.getType(), inputBP.getType());
        Assert.assertEquals(retrievedBP.getLocale(), inputBP.getLocale());
    }

    @Test(dataProvider = "applicationBrandingPreferenceDataProvider")
    public void testResolveApplicationBrandingPreference(Object brandingPreference, String tenantDomain, int tenantId)
            throws Exception {

        setCarbonContextForTenant(tenantDomain, tenantId);
        BrandingPreference inputBP = (BrandingPreference) brandingPreference;
        when(resolver.resolveBranding(inputBP.getType(), inputBP.getName(), inputBP.getLocale(), false))
                .thenReturn(inputBP);

        //  Retrieving added branding preference.
        BrandingPreference retrievedBP = brandingPreferenceManagerImpl.resolveBrandingPreference
                (inputBP.getType(), inputBP.getName(), inputBP.getLocale(), false);
        Assert.assertEquals(retrievedBP.getPreference(), inputBP.getPreference());
        Assert.assertEquals(retrievedBP.getName(), inputBP.getName());
        Assert.assertEquals(retrievedBP.getType(), inputBP.getType());
        Assert.assertEquals(retrievedBP.getLocale(), inputBP.getLocale());
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

    @Test(description = "Test the transaction errors while getting branding preference.")
    public void testTransactionErrorsWhileGettingBrandingPreference() throws Exception {

        BrandingPreference brandingPreference = new BrandingPreference();
        brandingPreference.setType(APPLICATION_TYPE);
        brandingPreference.setName(SAMPLE_APPLICATION_ID_3);
        brandingPreference.setLocale(DEFAULT_LOCALE);
        brandingPreference.setPreference(getPreferenceFromFile("sample-preference-2.json"));

        brandingPreferenceManagerImpl.addBrandingPreference(brandingPreference);

        // Test the failure of getting custom layout content.
        try (MockedStatic<CustomContentPersistentFactory> mockedCustomContentPersistentFactory = mockStatic(
                CustomContentPersistentFactory.class)) {
            CustomContentPersistentDAO customContentPersistentDAO = mock(CustomContentPersistentDAO.class);
            mockedCustomContentPersistentFactory.when(CustomContentPersistentFactory::getCustomContentPersistentDAO)
                    .thenReturn(customContentPersistentDAO);
            when(customContentPersistentDAO.getCustomContent(anyString(), anyString()))
                    .thenThrow(BrandingPreferenceMgtServerException.class);
            assertThrows(BrandingPreferenceMgtServerException.class,
                    () -> brandingPreferenceManagerImpl.getBrandingPreference(APPLICATION_TYPE,
                            SAMPLE_APPLICATION_ID_3, DEFAULT_LOCALE));
        }

        // Test the failure of getting branding config from configuration store.
        ConfigurationManager configurationManager = mock(ConfigurationManager.class);
        BrandingPreferenceManagerComponentDataHolder.getInstance().setConfigurationManager(configurationManager);
        when(configurationManager.getFiles(anyString(), anyString())).thenReturn(new ArrayList<>());
        assertThrows(BrandingPreferenceMgtClientException.class,
                () -> brandingPreferenceManagerImpl.getBrandingPreference(APPLICATION_TYPE, SAMPLE_APPLICATION_ID_3,
                        DEFAULT_LOCALE));
        ResourceFile resourceFile = mock(ResourceFile.class);
        when(resourceFile.getId()).thenReturn(null);
        when(configurationManager.getFiles(anyString(), anyString())).thenReturn(new ArrayList<ResourceFile>() {{
            add(resourceFile);
        }});
        assertThrows(BrandingPreferenceMgtClientException.class,
                () -> brandingPreferenceManagerImpl.getBrandingPreference(APPLICATION_TYPE, SAMPLE_APPLICATION_ID_3,
                        DEFAULT_LOCALE));
        when(resourceFile.getId()).thenReturn("file-id");
        when(configurationManager.getFileById(anyString(), anyString(), anyString())).thenReturn(null);
        assertThrows(BrandingPreferenceMgtClientException.class,
                () -> brandingPreferenceManagerImpl.getBrandingPreference(APPLICATION_TYPE, SAMPLE_APPLICATION_ID_3,
                        DEFAULT_LOCALE));
        when(configurationManager.getFiles(anyString(), anyString())).thenThrow(
                ConfigurationManagementException.class);
        assertThrows(BrandingPreferenceMgtServerException.class,
                () -> brandingPreferenceManagerImpl.getBrandingPreference(APPLICATION_TYPE, SAMPLE_APPLICATION_ID_3,
                        DEFAULT_LOCALE));
        doThrow(RuntimeException.class).when(configurationManager).getFiles(anyString(), anyString());
        assertThrows(BrandingPreferenceMgtServerException.class,
                () -> brandingPreferenceManagerImpl.getBrandingPreference(APPLICATION_TYPE, SAMPLE_APPLICATION_ID_3,
                        DEFAULT_LOCALE));

        BrandingPreferenceManagerComponentDataHolder.getInstance()
                .setConfigurationManager(ConfigurationManagementUtils.getConfigurationManager());
        // Test the IO exception while building the branding preference.
        try (MockedStatic<IOUtils> mockedIOUtils = mockStatic(IOUtils.class)) {
            mockedIOUtils.when(() -> IOUtils.toString(any(InputStream.class), anyString()))
                    .thenThrow(IOException.class);
            assertThrows(BrandingPreferenceMgtServerException.class,
                    () -> brandingPreferenceManagerImpl.getBrandingPreference(APPLICATION_TYPE,
                            SAMPLE_APPLICATION_ID_3, DEFAULT_LOCALE));
        }

        brandingPreferenceManagerImpl.deleteBrandingPreference(APPLICATION_TYPE, SAMPLE_APPLICATION_ID_3,
                DEFAULT_LOCALE);
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
        newBrandingPreference1.setPreference(getPreferenceFromFile("sample-preference-2.json"));

        BrandingPreference brandingPreference2 = new BrandingPreference();
        brandingPreference2.setType(ORGANIZATION_TYPE);
        brandingPreference2.setName(SAMPLE_TENANT_DOMAIN_NAME_ABC);
        brandingPreference2.setLocale(DEFAULT_LOCALE);
        brandingPreference2.setPreference(getPreferenceFromFile("sample-preference-1.json"));

        BrandingPreference newBrandingPreference2 = new BrandingPreference();
        newBrandingPreference2.setType(ORGANIZATION_TYPE);
        newBrandingPreference2.setName(SAMPLE_TENANT_DOMAIN_NAME_ABC);
        newBrandingPreference2.setLocale(DEFAULT_LOCALE);
        newBrandingPreference2.setPreference(getPreferenceFromFile("sample-preference-2.json"));

        BrandingPreference brandingPreference3 = new BrandingPreference();
        brandingPreference3.setType(APPLICATION_TYPE);
        brandingPreference3.setName(SAMPLE_APPLICATION_ID_3);
        brandingPreference3.setLocale(DEFAULT_LOCALE);
        brandingPreference3.setPreference(getPreferenceFromFile("sample-preference-1.json"));

        BrandingPreference newBrandingPreference3 = new BrandingPreference();
        newBrandingPreference3.setType(APPLICATION_TYPE);
        newBrandingPreference3.setName(SAMPLE_APPLICATION_ID_3);
        newBrandingPreference3.setLocale(DEFAULT_LOCALE);
        newBrandingPreference3.setPreference(getPreferenceFromFile("sample-preference-2.json"));

        return new Object[][]{
                {brandingPreference1, newBrandingPreference1, SUPER_TENANT_DOMAIN_NAME, SUPER_TENANT_ID},
                {brandingPreference2, newBrandingPreference2, SAMPLE_TENANT_DOMAIN_NAME_ABC, SAMPLE_TENANT_ID_ABC},
                {brandingPreference3, newBrandingPreference3, SAMPLE_TENANT_DOMAIN_NAME_ABC, SAMPLE_TENANT_ID_ABC}
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
        // Verify that clearBrandingResolverCacheHierarchy is called once and only once.
        verify(resolver, times(1)).clearBrandingResolverCacheHierarchy(any(), any(), any());

        BrandingPreference updatedBP = brandingPreferenceManagerImpl.replaceBrandingPreference(newBP);
        Assert.assertEquals(updatedBP.getPreference(), newBP.getPreference());
         /* Since published state is not changed, verify that clearBrandingResolverCacheHierarchy is not called again
           after the update. */
        verify(resolver, times(1)).clearBrandingResolverCacheHierarchy(any(), any(), any());

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

    @DataProvider(name = "updateBrandingPreferencePublishedStateDataProvider")
    public Object[][] updateBrandingPreferencePublishedStateDataProvider() throws Exception {

        BrandingPreference brandingPreference1 = new BrandingPreference();
        brandingPreference1.setType(ORGANIZATION_TYPE);
        brandingPreference1.setName(SUPER_TENANT_DOMAIN_NAME);
        brandingPreference1.setLocale(DEFAULT_LOCALE);
        brandingPreference1.setPreference(getPreferenceFromFile("sample-preference-1.json"));

        BrandingPreference brandingPreference2 = new BrandingPreference();
        brandingPreference2.setType(ORGANIZATION_TYPE);
        brandingPreference2.setName(SAMPLE_TENANT_DOMAIN_NAME_ABC);
        brandingPreference2.setLocale(DEFAULT_LOCALE);
        brandingPreference2.setPreference(getPreferenceFromFile("sample-preference-1.json"));

        BrandingPreference unpublishedBrandingPreference1 = new BrandingPreference();
        unpublishedBrandingPreference1.setType(ORGANIZATION_TYPE);
        unpublishedBrandingPreference1.setName(SUPER_TENANT_DOMAIN_NAME);
        unpublishedBrandingPreference1.setLocale(DEFAULT_LOCALE);
        unpublishedBrandingPreference1.setPreference(getPreferenceFromFile("sample-unpublished-preference.json"));

        BrandingPreference unpublishedBrandingPreference2 = new BrandingPreference();
        unpublishedBrandingPreference2.setType(ORGANIZATION_TYPE);
        unpublishedBrandingPreference2.setName(SAMPLE_TENANT_DOMAIN_NAME_ABC);
        unpublishedBrandingPreference2.setLocale(DEFAULT_LOCALE);
        unpublishedBrandingPreference2.setPreference(getPreferenceFromFile("sample-unpublished-preference.json"));

        return new Object[][]{
                {brandingPreference1, unpublishedBrandingPreference1, SUPER_TENANT_DOMAIN_NAME, SUPER_TENANT_ID},
                {brandingPreference2, unpublishedBrandingPreference2, SAMPLE_TENANT_DOMAIN_NAME_ABC,
                        SAMPLE_TENANT_ID_ABC},
                {unpublishedBrandingPreference1, brandingPreference1, SUPER_TENANT_DOMAIN_NAME, SUPER_TENANT_ID},
                {unpublishedBrandingPreference2, brandingPreference2, SAMPLE_TENANT_DOMAIN_NAME_ABC,
                        SAMPLE_TENANT_ID_ABC},
        };
    }

    @Test(dataProvider = "updateBrandingPreferencePublishedStateDataProvider")
    public void testUpdateBrandingPreferencePublishedState(Object brandingPreference, Object newBrandingPreference,
                                              String tenantDomain, int tenantId) throws Exception {

        setCarbonContextForTenant(tenantDomain, tenantId);
        BrandingPreference inputBP = (BrandingPreference) brandingPreference;
        BrandingPreference newBP = (BrandingPreference) newBrandingPreference;

        // Adding new branding preference.
        BrandingPreference addedBP = brandingPreferenceManagerImpl.addBrandingPreference(inputBP);
        Assert.assertEquals(addedBP.getPreference(), inputBP.getPreference());
        // Verify that clearBrandingResolverCacheHierarchy is called once and only once.
        verify(resolver, times(1)).clearBrandingResolverCacheHierarchy(any(), any(), any());

        BrandingPreference updatedBP = brandingPreferenceManagerImpl.replaceBrandingPreference(newBP);
        Assert.assertEquals(updatedBP.getPreference(), newBP.getPreference());
         /* Since published state is changed, verify that clearBrandingResolverCacheHierarchy is called again
           after the update. */
        verify(resolver, times(2)).clearBrandingResolverCacheHierarchy(any(), any(), any());

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

    @Test(description = "Test the transaction errors while replacing branding preference.")
    public void testTransactionErrorsWhileReplacingBrandingPreference() throws Exception {

        BrandingPreference brandingPreference = new BrandingPreference();
        brandingPreference.setType(APPLICATION_TYPE);
        brandingPreference.setName(SAMPLE_APPLICATION_ID_3);
        brandingPreference.setLocale(DEFAULT_LOCALE);
        brandingPreference.setPreference(getPreferenceFromFile("sample-preference-1.json"));

        BrandingPreference brandingPreferenceNew = new BrandingPreference();
        brandingPreferenceNew.setType(APPLICATION_TYPE);
        brandingPreferenceNew.setName(SAMPLE_APPLICATION_ID_3);
        brandingPreferenceNew.setLocale(DEFAULT_LOCALE);
        brandingPreferenceNew.setPreference(getPreferenceFromFile("sample-preference-2.json"));

        brandingPreferenceManagerImpl.addBrandingPreference(brandingPreference);

        // Test the failure of updating custom layout content.
        try (MockedStatic<CustomContentPersistentFactory> mockedCustomContentPersistentFactory = mockStatic(
                CustomContentPersistentFactory.class)) {
            CustomContentPersistentDAO customContentPersistentDAO = mock(CustomContentPersistentDAO.class);
            mockedCustomContentPersistentFactory.when(CustomContentPersistentFactory::getCustomContentPersistentDAO)
                    .thenReturn(customContentPersistentDAO);
            doThrow(BrandingPreferenceMgtServerException.class).when(customContentPersistentDAO)
                    .updateCustomContent(any(CustomLayoutContent.class), anyString(), anyString());
            assertThrows(BrandingPreferenceMgtServerException.class,
                    () -> brandingPreferenceManagerImpl.replaceBrandingPreference(brandingPreferenceNew));
            // Verify transaction rollback.
            BrandingPreference currentBP = brandingPreferenceManagerImpl
                    .getBrandingPreference(APPLICATION_TYPE, SAMPLE_APPLICATION_ID_3, DEFAULT_LOCALE);
            assertEquals(currentBP.getPreference(), brandingPreference.getPreference());
        }

        // Test the failure of replacing branding config in configuration store.
        ConfigurationManager configurationManager = BrandingPreferenceManagerComponentDataHolder.getInstance()
                .getConfigurationManager();
        configurationManager = spy(configurationManager);
        BrandingPreferenceManagerComponentDataHolder.getInstance().setConfigurationManager(configurationManager);
        doThrow(ConfigurationManagementException.class).when(configurationManager)
                .replaceResource(anyString(), any(Resource.class));
        assertThrows(BrandingPreferenceMgtServerException.class,
                () -> brandingPreferenceManagerImpl.replaceBrandingPreference(brandingPreferenceNew));

        BrandingPreferenceManagerComponentDataHolder.getInstance()
                .setConfigurationManager(ConfigurationManagementUtils.getConfigurationManager());
        // Test the IO exception while building the branding preference.
        try (MockedConstruction<ByteArrayInputStream> mockedByteArrayInputStream = mockConstruction(
                ByteArrayInputStream.class, (mock, context) -> {
                    throw new IOException("error");
                })) {
            assertThrows(BrandingPreferenceMgtServerException.class,
                    () -> brandingPreferenceManagerImpl.replaceBrandingPreference(brandingPreferenceNew));
        }

        brandingPreferenceManagerImpl.deleteBrandingPreference(APPLICATION_TYPE, SAMPLE_APPLICATION_ID_3,
                DEFAULT_LOCALE);
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
        // Verify that clearBrandingResolverCacheHierarchy is called once and only once.
        verify(resolver, times(1)).clearBrandingResolverCacheHierarchy(any(), any(), any());

        // Deleting added branding preference.
        brandingPreferenceManagerImpl.deleteBrandingPreference
                (inputBP.getType(), inputBP.getName(), inputBP.getLocale());
        // Verify that clearBrandingResolverCacheHierarchy is called again after the deletion.
        verify(resolver, times(2)).clearBrandingResolverCacheHierarchy(any(), any(), any());

        // Retrieving deleted branding preference.
        assertThrows(BrandingPreferenceMgtClientException.class, () -> brandingPreferenceManagerImpl
                .getBrandingPreference(inputBP.getType(), inputBP.getName(), inputBP.getLocale()));
    }

    @Test(dataProvider = "notExistingBrandingPreferenceDataProvider")
    public void testDeleteNotExistingBrandingPreference(String tenantDomain, int tenantId) throws Exception {

        setCarbonContextForTenant(tenantDomain, tenantId);
        assertThrows(BrandingPreferenceMgtClientException.class, () -> brandingPreferenceManagerImpl
                .deleteBrandingPreference(ORGANIZATION_TYPE, SUPER_TENANT_DOMAIN_NAME, DEFAULT_LOCALE));
        // Verify that clearBrandingResolverCacheHierarchy is never called.
        verify(resolver, never()).clearBrandingResolverCacheHierarchy(any(), any(), any());
    }

    @Test(description = "Test the transaction errors while deleting branding preference.")
    public void testTransactionErrorsWhileDeletingBrandingPreference() throws Exception {

        BrandingPreference brandingPreference = new BrandingPreference();
        brandingPreference.setType(APPLICATION_TYPE);
        brandingPreference.setName(SAMPLE_APPLICATION_ID_3);
        brandingPreference.setLocale(DEFAULT_LOCALE);
        brandingPreference.setPreference(getPreferenceFromFile("sample-preference-2.json"));

        brandingPreferenceManagerImpl.addBrandingPreference(brandingPreference);

        // Test the failure of deleting custom layout content.
//        try (MockedStatic<CustomContentPersistentFactory> mockedCustomContentPersistentFactory = mockStatic(
//                CustomContentPersistentFactory.class)) {
//            CustomContentPersistentDAO customContentPersistentDAO = mock(CustomContentPersistentDAO.class);
//            mockedCustomContentPersistentFactory.when(CustomContentPersistentFactory::getCustomContentPersistentDAO)
//                    .thenReturn(customContentPersistentDAO);
//            doThrow(BrandingPreferenceMgtServerException.class).when(customContentPersistentDAO)
//                    .deleteCustomContent(anyString(), anyString());
//            assertThrows(BrandingPreferenceMgtServerException.class,
//                    () -> brandingPreferenceManagerImpl.deleteBrandingPreference(APPLICATION_TYPE,
//                            SAMPLE_APPLICATION_ID_3, DEFAULT_LOCALE));
//            // Verify transaction rollback.
//            BrandingPreference currentBP = brandingPreferenceManagerImpl
//                    .getBrandingPreference(APPLICATION_TYPE, SAMPLE_APPLICATION_ID_3, DEFAULT_LOCALE);
//            assertEquals(currentBP.getPreference(), brandingPreference.getPreference());
//        }

        // Test the failure of replacing branding config in configuration store.
        ConfigurationManager configurationManager = BrandingPreferenceManagerComponentDataHolder.getInstance()
                .getConfigurationManager();
        configurationManager = spy(configurationManager);
        BrandingPreferenceManagerComponentDataHolder.getInstance().setConfigurationManager(configurationManager);
        doThrow(ConfigurationManagementException.class).when(configurationManager)
                .deleteResource(anyString(), anyString());
        assertThrows(BrandingPreferenceMgtServerException.class,
                () -> brandingPreferenceManagerImpl.deleteBrandingPreference(APPLICATION_TYPE, SAMPLE_APPLICATION_ID_3,
                        DEFAULT_LOCALE));

        BrandingPreferenceManagerComponentDataHolder.getInstance()
                .setConfigurationManager(ConfigurationManagementUtils.getConfigurationManager());
        brandingPreferenceManagerImpl.deleteBrandingPreference(APPLICATION_TYPE, SAMPLE_APPLICATION_ID_3,
                DEFAULT_LOCALE);
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
        // Verify that clearCustomTextResolverCacheHierarchy is called once and only once.
        verify(resolver, times(1)).clearCustomTextResolverCacheHierarchy(any(), any(), any());

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
    public void testAddInvaliCustomTextPreference(Object customText) throws Exception {

        CustomText inputCT = (CustomText) customText;

        // Adding new custom text preference.
        assertThrows(BrandingPreferenceMgtClientException.class, () -> brandingPreferenceManagerImpl
                .addCustomText(inputCT));
        // Verify that clearCustomTextResolverCacheHierarchy is never called.
        verify(resolver, never()).clearCustomTextResolverCacheHierarchy(any(), any(), any());
    }

    @Test(dataProvider = "customTextPreferenceDataProvider")
    public void testResolveCustomText(Object customText, String tenantDomain, int tenantId)
            throws Exception {

        setCarbonContextForTenant(tenantDomain, tenantId);
        CustomText inputCT = (CustomText) customText;
        when(resolver.resolveCustomText(inputCT.getType(), inputCT.getName(), inputCT.getScreen(),
                inputCT.getLocale())).thenReturn(inputCT);

        CustomText retrievedCT =
                brandingPreferenceManagerImpl.resolveCustomText(inputCT.getType(), inputCT.getName(),
                        inputCT.getScreen(), inputCT.getLocale());
        Assert.assertEquals(retrievedCT.getPreference(), inputCT.getPreference());
        Assert.assertEquals(retrievedCT.getName(), inputCT.getName());
        Assert.assertEquals(retrievedCT.getType(), inputCT.getType());
        Assert.assertEquals(retrievedCT.getScreen(), inputCT.getScreen());
        Assert.assertEquals(retrievedCT.getLocale(), inputCT.getLocale());
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
        // Verify that clearCustomTextResolverCacheHierarchy is never called.
        verify(resolver, never()).clearCustomTextResolverCacheHierarchy(any(), any(), any());
    }

    @DataProvider(name = "multipleCustomTextDataProvider")
    public Object[][] multipleCustomTextDataProvider() throws Exception {

        CustomText customText1 = new CustomText();
        customText1.setType(ORGANIZATION_TYPE);
        customText1.setName(SAMPLE_TENANT_DOMAIN_NAME_ABC);
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
                {customText1, customText2, SAMPLE_TENANT_DOMAIN_NAME_ABC, SUPER_TENANT_ID},
        };
    }

    @Test(dataProvider = "multipleCustomTextDataProvider")
    public void testDeleteAllCustomText(Object customText1, Object customText2, String tenantDomain, int tenantId)
            throws Exception {

        setCarbonContextForTenant(tenantDomain, tenantId);
        CustomText inputCT1 = (CustomText) customText1;
        CustomText inputCT2 = (CustomText) customText2;

        // Adding new custom text preference.
        brandingPreferenceManagerImpl.addCustomText(inputCT1);
        brandingPreferenceManagerImpl.addCustomText(inputCT2);
        // Verify that clearCustomTextResolverCacheHierarchy is called twice.
        verify(resolver, times(2)).clearCustomTextResolverCacheHierarchy(any(), any(), any());

        // Retrieving added custom text preference.
        CustomText retrievedCT1 = brandingPreferenceManagerImpl.getCustomText
                (inputCT1.getType(), inputCT1.getName(), inputCT1.getScreen(), inputCT1.getLocale());
        Assert.assertEquals(retrievedCT1.getPreference(), inputCT1.getPreference());
        Assert.assertEquals(retrievedCT1.getName(), inputCT1.getName());
        Assert.assertEquals(retrievedCT1.getType(), inputCT1.getType());
        Assert.assertEquals(retrievedCT1.getLocale(), inputCT1.getLocale());

        CustomText retrievedCT2 = brandingPreferenceManagerImpl.getCustomText
                (inputCT2.getType(), inputCT2.getName(), inputCT2.getScreen(), inputCT2.getLocale());
        Assert.assertEquals(retrievedCT2.getPreference(), inputCT2.getPreference());
        Assert.assertEquals(retrievedCT2.getName(), inputCT2.getName());
        Assert.assertEquals(retrievedCT2.getType(), inputCT2.getType());
        Assert.assertEquals(retrievedCT2.getLocale(), inputCT2.getLocale());

        // Bulk Deleting added custom text preferences.
        brandingPreferenceManagerImpl.deleteAllCustomText();
        // Verify that clearCustomTextResolverCacheHierarchy is called again once after deletion.
        verify(resolver, times(3)).clearCustomTextResolverCacheHierarchy(any(), any(), any());

        assertThrows(BrandingPreferenceMgtClientException.class, () -> brandingPreferenceManagerImpl
                .getCustomText(inputCT1.getType(), inputCT1.getName(), inputCT1.getScreen(), inputCT1.getLocale()));
        assertThrows(BrandingPreferenceMgtClientException.class, () -> brandingPreferenceManagerImpl
                .getCustomText(inputCT2.getType(), inputCT2.getName(), inputCT2.getScreen(), inputCT2.getLocale()));
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
