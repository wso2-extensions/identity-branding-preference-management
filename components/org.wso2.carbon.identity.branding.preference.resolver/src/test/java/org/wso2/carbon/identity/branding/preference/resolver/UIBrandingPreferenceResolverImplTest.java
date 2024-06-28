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

package org.wso2.carbon.identity.branding.preference.resolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.base.CarbonBaseConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.context.internal.OSGiDataHolder;
import org.wso2.carbon.identity.branding.preference.management.core.UIBrandingPreferenceResolver;
import org.wso2.carbon.identity.branding.preference.management.core.exception.BrandingPreferenceMgtClientException;
import org.wso2.carbon.identity.branding.preference.management.core.internal.BrandingPreferenceManagerComponentDataHolder;
import org.wso2.carbon.identity.branding.preference.management.core.model.BrandingPreference;
import org.wso2.carbon.identity.branding.preference.resolver.cache.BrandedAppCache;
import org.wso2.carbon.identity.branding.preference.resolver.cache.BrandedAppCacheEntry;
import org.wso2.carbon.identity.branding.preference.resolver.cache.BrandedAppCacheKey;
import org.wso2.carbon.identity.branding.preference.resolver.cache.BrandedOrgCache;
import org.wso2.carbon.identity.branding.preference.resolver.cache.TextCustomizedOrgCache;
import org.wso2.carbon.identity.branding.preference.resolver.internal.BrandingResolverComponentDataHolder;
import org.wso2.carbon.identity.common.testng.realm.InMemoryRealmService;
import org.wso2.carbon.identity.configuration.mgt.core.ConfigurationManager;
import org.wso2.carbon.identity.configuration.mgt.core.exception.ConfigurationManagementException;
import org.wso2.carbon.identity.configuration.mgt.core.model.ResourceFile;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.services.IdentityEventService;
import org.wso2.carbon.identity.organization.management.application.OrgApplicationManager;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.identity.organization.management.service.model.ParentOrganizationDO;
import org.wso2.carbon.user.api.TenantManager;
import org.wso2.carbon.user.api.UserRealmService;
import org.wso2.carbon.user.core.UserStoreException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.testng.Assert.assertThrows;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.APPLICATION_BRANDING_RESOURCE_TYPE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.APPLICATION_TYPE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.BRANDING_RESOURCE_TYPE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.DEFAULT_LOCALE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ORGANIZATION_TYPE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.RESOURCE_NAME_SEPARATOR;

/**
 * Unit tests for UIBrandingPreferenceResolverImpl.
 */
public class UIBrandingPreferenceResolverImplTest {

    @Mock
    private OrganizationManager organizationManager;
    @Mock
    private IdentityEventService identityEventService;
    @Mock
    private ConfigurationManager configurationManager;
    @Mock
    private OrgApplicationManager orgApplicationManager;
    @Mock
    private BrandedOrgCache brandedOrgCache;
    @Mock
    private BrandedAppCache brandedAppCache;
    @Mock
    private TextCustomizedOrgCache textCustomizedOrgCache;

    private UIBrandingPreferenceResolver brandingPreferenceResolver;

    private static final String ROOT_APP_ID = "fa9b9ac5-a429-49e2-9c51-4259c7ebe45e";
    private static final String ROOT_ORG_ID = "72b81cba-51c7-4dc1-91be-b267e177c17a";
    private static final String ROOT_TENANT_DOMAIN = "root-organization";
    private static final int ROOT_TENANT_ID = 1;
    private static final String PARENT_APP_ID = "1e2ef3df-e670-4339-9833-9df41dda7c96";
    private static final String PARENT_ORG_ID = "93d996f9-a5ba-4275-a52b-adaad9eba869";
    private static final int PARENT_TENANT_ID = 2;
    private static final String CHILD_APP_ID = "42ef1d92-add6-449b-8a3c-fc308d2a4eac";
    private static final String CHILD_ORG_ID = "30b701c6-e309-4241-b047-0c299c45d1a0";
    private static final int CHILD_TENANT_ID = 3;

    @BeforeMethod
    public void setUp() throws Exception {

        openMocks(this);
        setCarbonHome();

        BrandingResolverComponentDataHolder.getInstance().setConfigurationManager(configurationManager);
        BrandingResolverComponentDataHolder.getInstance().setOrganizationManager(organizationManager);
        BrandingResolverComponentDataHolder.getInstance().setOrgApplicationManager(orgApplicationManager);

        BrandingPreferenceManagerComponentDataHolder.getInstance().setIdentityEventService(identityEventService);
        doNothing().when(identityEventService).handleEvent(any(Event.class));

        brandingPreferenceResolver =
                new UIBrandingPreferenceResolverImpl(brandedOrgCache, brandedAppCache, textCustomizedOrgCache);
    }

    @Test
    public void testResolveAppBrandingFromCurrentAppBranding() throws Exception {

        try (MockedStatic<OSGiDataHolder> mockedOSGiDataHolder = mockStatic(OSGiDataHolder.class)) {
            mockOSGiDataHolder(mockedOSGiDataHolder);
            setCarbonContextForTenant(CHILD_ORG_ID, CHILD_TENANT_ID, CHILD_ORG_ID);

            String resourceName = CHILD_APP_ID.toLowerCase() + RESOURCE_NAME_SEPARATOR + DEFAULT_LOCALE;
            String resourceId = "51356f5e-e10b-49f2-87a6-f7f48e164374";
            String resourceFileName = "sample-child-app-branding-preference.json";

            mockBrandingPreferenceRetrieval(resourceName, resourceId, APPLICATION_BRANDING_RESOURCE_TYPE,
                    resourceFileName);

            BrandingPreference resolvedBrandingPreference =
                    brandingPreferenceResolver.resolveBranding(APPLICATION_TYPE, CHILD_APP_ID, DEFAULT_LOCALE);

            Assert.assertEquals(resolvedBrandingPreference.getName(), CHILD_APP_ID);
            Assert.assertEquals(resolvedBrandingPreference.getLocale(), DEFAULT_LOCALE);
            Assert.assertEquals(resolvedBrandingPreference.getType(), APPLICATION_TYPE);
            Assert.assertEquals(resolvedBrandingPreference.getPreference(), getPreferenceFromFile(resourceFileName));
        }
    }

    @Test
    public void testResolveAppBrandingFromCurrentOrgBranding() throws Exception {

        try (MockedStatic<OSGiDataHolder> mockedOSGiDataHolder = mockStatic(OSGiDataHolder.class)) {
            mockOSGiDataHolder(mockedOSGiDataHolder);
            setCarbonContextForTenant(CHILD_ORG_ID, CHILD_TENANT_ID, CHILD_ORG_ID);

            String resourceName =
                    String.valueOf(CHILD_TENANT_ID).toLowerCase() + RESOURCE_NAME_SEPARATOR + DEFAULT_LOCALE;
            String resourceId = "61356f5e-e10b-49f2-87a6-f7f48e164374";
            String resourceFileName = "sample-child-org-branding-preference.json";

            mockBrandingPreferenceRetrieval(resourceName, resourceId, BRANDING_RESOURCE_TYPE, resourceFileName);

            BrandingPreference resolvedBrandingPreference =
                    brandingPreferenceResolver.resolveBranding(APPLICATION_TYPE, CHILD_APP_ID, DEFAULT_LOCALE);

            Assert.assertEquals(resolvedBrandingPreference.getName(), CHILD_ORG_ID);
            Assert.assertEquals(resolvedBrandingPreference.getLocale(), DEFAULT_LOCALE);
            Assert.assertEquals(resolvedBrandingPreference.getType(), ORGANIZATION_TYPE);
            Assert.assertEquals(resolvedBrandingPreference.getPreference(), getPreferenceFromFile(resourceFileName));
        }
    }

    @Test
    public void testResolveAppBrandingFromParentAppBranding() throws Exception {

        try (MockedStatic<OSGiDataHolder> mockedOSGiDataHolder = mockStatic(OSGiDataHolder.class)) {
            mockOSGiDataHolder(mockedOSGiDataHolder);
            setCarbonContextForTenant(CHILD_ORG_ID, CHILD_TENANT_ID, CHILD_ORG_ID);

            String resourceName = PARENT_APP_ID.toLowerCase() + RESOURCE_NAME_SEPARATOR + DEFAULT_LOCALE;
            String resourceId = "71356f5e-e10b-49f2-87a6-f7f48e164374";
            String resourceFileName = "sample-parent-app-branding-preference.json";

            Organization childOrganization = new Organization();
            childOrganization.setId(CHILD_ORG_ID);
            ParentOrganizationDO parentOrganizationDO = new ParentOrganizationDO();
            parentOrganizationDO.setId(PARENT_ORG_ID);
            childOrganization.setParent(parentOrganizationDO);

            when(organizationManager.getOrganization(CHILD_ORG_ID, false, false)).thenReturn(childOrganization);

            when(orgApplicationManager.getParentAppId(CHILD_APP_ID, CHILD_ORG_ID)).thenReturn(
                    PARENT_APP_ID);
            when(organizationManager.resolveTenantDomain(PARENT_ORG_ID)).thenReturn(PARENT_ORG_ID);
            when(organizationManager.getOrganizationDepthInHierarchy(PARENT_ORG_ID)).thenReturn(1);

            mockBrandingPreferenceRetrieval(resourceName, resourceId, APPLICATION_BRANDING_RESOURCE_TYPE,
                    resourceFileName);

            BrandingPreference resolvedBrandingPreference =
                    brandingPreferenceResolver.resolveBranding(APPLICATION_TYPE, CHILD_APP_ID, DEFAULT_LOCALE);

            Assert.assertEquals(resolvedBrandingPreference.getName(), PARENT_APP_ID);
            Assert.assertEquals(resolvedBrandingPreference.getLocale(), DEFAULT_LOCALE);
            Assert.assertEquals(resolvedBrandingPreference.getType(), APPLICATION_TYPE);
            Assert.assertEquals(resolvedBrandingPreference.getPreference(),
                    getPreferenceFromFile("sample-parent-app-branding-preference-without-display-name.json"));
        }
    }

    @Test
    public void testResolveAppBrandingFromParentOrgBranding() throws Exception {

        try (MockedStatic<OSGiDataHolder> mockedOSGiDataHolder = mockStatic(OSGiDataHolder.class)) {
            mockOSGiDataHolder(mockedOSGiDataHolder);
            setCarbonContextForTenant(CHILD_ORG_ID, CHILD_TENANT_ID, CHILD_ORG_ID);
            String resourceName =
                    String.valueOf(PARENT_TENANT_ID).toLowerCase() + RESOURCE_NAME_SEPARATOR + DEFAULT_LOCALE;
            String resourceId = "81356f5e-e10b-49f2-87a6-f7f48e164374";
            String resourceFileName = "sample-parent-org-branding-preference.json";

            Organization childOrganization = new Organization();
            childOrganization.setId(CHILD_ORG_ID);
            ParentOrganizationDO parentOrganizationDO = new ParentOrganizationDO();
            parentOrganizationDO.setId(PARENT_ORG_ID);
            childOrganization.setParent(parentOrganizationDO);

            when(organizationManager.getOrganization(CHILD_ORG_ID, false, false)).thenReturn(childOrganization);

            when(orgApplicationManager.getParentAppId(CHILD_APP_ID, CHILD_ORG_ID)).thenReturn(
                    PARENT_APP_ID);
            when(organizationManager.resolveTenantDomain(PARENT_ORG_ID)).thenReturn(PARENT_ORG_ID);
            when(organizationManager.getOrganizationDepthInHierarchy(PARENT_ORG_ID)).thenReturn(1);

            mockBrandingPreferenceRetrieval(resourceName, resourceId, BRANDING_RESOURCE_TYPE, resourceFileName);

            BrandingPreference resolvedBrandingPreference =
                    brandingPreferenceResolver.resolveBranding(APPLICATION_TYPE, CHILD_APP_ID, DEFAULT_LOCALE);

            Assert.assertEquals(resolvedBrandingPreference.getName(), PARENT_ORG_ID);
            Assert.assertEquals(resolvedBrandingPreference.getLocale(), DEFAULT_LOCALE);
            Assert.assertEquals(resolvedBrandingPreference.getType(), ORGANIZATION_TYPE);
            Assert.assertEquals(resolvedBrandingPreference.getPreference(),
                    getPreferenceFromFile("sample-parent-org-branding-preference-without-display-name.json"));
        }
    }

    @Test
    public void testResolveAppBrandingFromRootAppBranding() throws Exception {

        try (MockedStatic<OSGiDataHolder> mockedOSGiDataHolder = mockStatic(OSGiDataHolder.class)) {
            mockOSGiDataHolder(mockedOSGiDataHolder);
            setCarbonContextForTenant(CHILD_ORG_ID, CHILD_TENANT_ID, CHILD_ORG_ID);
            String resourceName = ROOT_APP_ID.toLowerCase() + RESOURCE_NAME_SEPARATOR + DEFAULT_LOCALE;
            String resourceId = "91356f5e-e10b-49f2-87a6-f7f48e164374";
            String resourceFileName = "sample-root-app-branding-preference.json";

            Organization childOrganization = new Organization();
            childOrganization.setId(CHILD_ORG_ID);
            ParentOrganizationDO parentOrganizationDO = new ParentOrganizationDO();
            parentOrganizationDO.setId(PARENT_ORG_ID);
            childOrganization.setParent(parentOrganizationDO);

            when(organizationManager.getOrganization(CHILD_ORG_ID, false, false)).thenReturn(childOrganization);

            when(orgApplicationManager.getParentAppId(PARENT_APP_ID, PARENT_ORG_ID)).thenReturn(
                    ROOT_APP_ID);
            when(organizationManager.resolveTenantDomain(ROOT_ORG_ID)).thenReturn(ROOT_TENANT_DOMAIN);
            when(organizationManager.getOrganizationDepthInHierarchy(ROOT_ORG_ID)).thenReturn(0);

            when(orgApplicationManager.getParentAppId(CHILD_APP_ID, CHILD_ORG_ID)).thenReturn(
                    PARENT_APP_ID);
            when(organizationManager.resolveTenantDomain(PARENT_ORG_ID)).thenReturn(PARENT_ORG_ID);
            when(organizationManager.getOrganizationDepthInHierarchy(PARENT_ORG_ID)).thenReturn(1);

            List<String> parentAncestorOrganizationIds = new ArrayList<>();
            parentAncestorOrganizationIds.add(PARENT_ORG_ID);
            parentAncestorOrganizationIds.add(ROOT_ORG_ID);
            when(organizationManager.getAncestorOrganizationIds(PARENT_ORG_ID)).thenReturn(
                    parentAncestorOrganizationIds);

            mockBrandingPreferenceRetrieval(resourceName, resourceId, APPLICATION_BRANDING_RESOURCE_TYPE,
                    resourceFileName);

            BrandingPreference resolvedBrandingPreference =
                    brandingPreferenceResolver.resolveBranding(APPLICATION_TYPE, CHILD_APP_ID, DEFAULT_LOCALE);

            Assert.assertEquals(resolvedBrandingPreference.getName(), ROOT_APP_ID);
            Assert.assertEquals(resolvedBrandingPreference.getLocale(), DEFAULT_LOCALE);
            Assert.assertEquals(resolvedBrandingPreference.getType(), APPLICATION_TYPE);
            Assert.assertEquals(resolvedBrandingPreference.getPreference(),
                    getPreferenceFromFile("sample-root-app-branding-preference-without-display-name.json"));
        }
    }

    @Test
    public void testResolveAppBrandingFromRootOrgBranding() throws Exception {

        try (MockedStatic<OSGiDataHolder> mockedOSGiDataHolder = mockStatic(OSGiDataHolder.class)) {
            mockOSGiDataHolder(mockedOSGiDataHolder);
            setCarbonContextForTenant(CHILD_ORG_ID, CHILD_TENANT_ID, CHILD_ORG_ID);
            String resourceName =
                    String.valueOf(ROOT_TENANT_ID).toLowerCase() + RESOURCE_NAME_SEPARATOR + DEFAULT_LOCALE;
            String resourceId = "11356f5e-e10b-49f2-87a6-f7f48e164374";
            String resourceFileName = "sample-root-org-branding-preference.json";

            Organization childOrganization = new Organization();
            childOrganization.setId(CHILD_ORG_ID);
            ParentOrganizationDO parentOrganizationDO = new ParentOrganizationDO();
            parentOrganizationDO.setId(PARENT_ORG_ID);
            childOrganization.setParent(parentOrganizationDO);

            when(organizationManager.getOrganization(CHILD_ORG_ID, false, false)).thenReturn(childOrganization);

            when(orgApplicationManager.getParentAppId(PARENT_APP_ID, PARENT_ORG_ID)).thenReturn(
                    ROOT_APP_ID);
            when(organizationManager.resolveTenantDomain(ROOT_ORG_ID)).thenReturn(ROOT_TENANT_DOMAIN);
            when(organizationManager.getOrganizationDepthInHierarchy(ROOT_ORG_ID)).thenReturn(0);

            when(orgApplicationManager.getParentAppId(CHILD_APP_ID, CHILD_ORG_ID)).thenReturn(
                    PARENT_APP_ID);
            when(organizationManager.resolveTenantDomain(PARENT_ORG_ID)).thenReturn(PARENT_ORG_ID);
            when(organizationManager.getOrganizationDepthInHierarchy(PARENT_ORG_ID)).thenReturn(1);

            List<String> parentAncestorOrganizationIds = new ArrayList<>();
            parentAncestorOrganizationIds.add(PARENT_ORG_ID);
            parentAncestorOrganizationIds.add(ROOT_ORG_ID);
            when(organizationManager.getAncestorOrganizationIds(PARENT_ORG_ID)).thenReturn(
                    parentAncestorOrganizationIds);

            mockBrandingPreferenceRetrieval(resourceName, resourceId, BRANDING_RESOURCE_TYPE, resourceFileName);

            BrandingPreference resolvedBrandingPreference =
                    brandingPreferenceResolver.resolveBranding(APPLICATION_TYPE, CHILD_APP_ID, DEFAULT_LOCALE);

            Assert.assertEquals(resolvedBrandingPreference.getName(), ROOT_TENANT_DOMAIN);
            Assert.assertEquals(resolvedBrandingPreference.getLocale(), DEFAULT_LOCALE);
            Assert.assertEquals(resolvedBrandingPreference.getType(), ORGANIZATION_TYPE);
            Assert.assertEquals(resolvedBrandingPreference.getPreference(),
                    getPreferenceFromFile("sample-root-org-branding-preference-without-display-name.json"));
        }
    }

    @Test
    public void testResolveAppBrandingWhenNoBrandingAvailable() throws Exception {

        try (MockedStatic<OSGiDataHolder> mockedOSGiDataHolder = mockStatic(OSGiDataHolder.class)) {
            mockOSGiDataHolder(mockedOSGiDataHolder);
            setCarbonContextForTenant(CHILD_ORG_ID, CHILD_TENANT_ID, CHILD_ORG_ID);

            Organization childOrganization = new Organization();
            childOrganization.setId(CHILD_ORG_ID);
            ParentOrganizationDO parentOrganizationDO = new ParentOrganizationDO();
            parentOrganizationDO.setId(PARENT_ORG_ID);
            childOrganization.setParent(parentOrganizationDO);

            when(organizationManager.getOrganization(CHILD_ORG_ID, false, false)).thenReturn(childOrganization);

            when(orgApplicationManager.getParentAppId(PARENT_APP_ID, PARENT_ORG_ID)).thenReturn(
                    ROOT_APP_ID);
            when(organizationManager.resolveTenantDomain(ROOT_ORG_ID)).thenReturn(ROOT_TENANT_DOMAIN);
            when(organizationManager.getOrganizationDepthInHierarchy(ROOT_ORG_ID)).thenReturn(0);

            when(orgApplicationManager.getParentAppId(CHILD_APP_ID, CHILD_ORG_ID)).thenReturn(
                    PARENT_APP_ID);
            when(organizationManager.resolveTenantDomain(PARENT_ORG_ID)).thenReturn(PARENT_ORG_ID);
            when(organizationManager.getOrganizationDepthInHierarchy(PARENT_ORG_ID)).thenReturn(1);

            List<String> parentAncestorOrganizationIds = new ArrayList<>();
            parentAncestorOrganizationIds.add(PARENT_ORG_ID);
            parentAncestorOrganizationIds.add(ROOT_ORG_ID);
            when(organizationManager.getAncestorOrganizationIds(PARENT_ORG_ID)).thenReturn(
                    parentAncestorOrganizationIds);

            assertThrows(BrandingPreferenceMgtClientException.class, () -> {
                BrandingPreference resolvedBrandingPreference =
                        brandingPreferenceResolver.resolveBranding(APPLICATION_TYPE, CHILD_APP_ID, DEFAULT_LOCALE);
            });
        }
    }

    @Test
    public void testResolveAppBrandingFromCacheWithAppLevelBranding() throws Exception {

        try (MockedStatic<OSGiDataHolder> mockedOSGiDataHolder = mockStatic(OSGiDataHolder.class)) {
            mockOSGiDataHolder(mockedOSGiDataHolder);
            setCarbonContextForTenant(CHILD_ORG_ID, CHILD_TENANT_ID, CHILD_ORG_ID);

            String resourceName = PARENT_APP_ID.toLowerCase() + RESOURCE_NAME_SEPARATOR + DEFAULT_LOCALE;
            String resourceId = "12356f5e-e10b-49f2-87a6-f7f48e164374";
            String resourceFileName = "sample-parent-app-branding-preference.json";

            BrandedAppCacheEntry brandedAppCacheEntry =
                    new BrandedAppCacheEntry(PARENT_ORG_ID, PARENT_APP_ID, APPLICATION_TYPE);
            when(brandedAppCache.getValueFromCache(any(BrandedAppCacheKey.class), eq(CHILD_ORG_ID))).thenReturn(
                    brandedAppCacheEntry);
            mockBrandingPreferenceRetrieval(resourceName, resourceId, APPLICATION_BRANDING_RESOURCE_TYPE,
                    resourceFileName);

            BrandingPreference resolvedBrandingPreference =
                    brandingPreferenceResolver.resolveBranding(APPLICATION_TYPE, CHILD_APP_ID, DEFAULT_LOCALE);

            Assert.assertEquals(resolvedBrandingPreference.getName(), PARENT_APP_ID);
            Assert.assertEquals(resolvedBrandingPreference.getLocale(), DEFAULT_LOCALE);
            Assert.assertEquals(resolvedBrandingPreference.getType(), APPLICATION_TYPE);
            Assert.assertEquals(resolvedBrandingPreference.getPreference(),
                    getPreferenceFromFile("sample-parent-app-branding-preference-without-display-name.json"));
        }
    }

    @Test
    public void testResolveAppBrandingFromCacheWithOrgLevelBranding() throws Exception {

        try (MockedStatic<OSGiDataHolder> mockedOSGiDataHolder = mockStatic(OSGiDataHolder.class)) {
            mockOSGiDataHolder(mockedOSGiDataHolder);
            setCarbonContextForTenant(CHILD_ORG_ID, CHILD_TENANT_ID, CHILD_ORG_ID);

            String resourceName =
                    String.valueOf(PARENT_TENANT_ID).toLowerCase() + RESOURCE_NAME_SEPARATOR + DEFAULT_LOCALE;
            String resourceId = "13356f5e-e10b-49f2-87a6-f7f48e164374";
            String resourceFileName = "sample-parent-org-branding-preference.json";

            BrandedAppCacheEntry brandedAppCacheEntry =
                    new BrandedAppCacheEntry(PARENT_ORG_ID, null, ORGANIZATION_TYPE);
            when(brandedAppCache.getValueFromCache(any(BrandedAppCacheKey.class), eq(CHILD_ORG_ID))).thenReturn(
                    brandedAppCacheEntry);
            mockBrandingPreferenceRetrieval(resourceName, resourceId, BRANDING_RESOURCE_TYPE, resourceFileName);

            BrandingPreference resolvedBrandingPreference =
                    brandingPreferenceResolver.resolveBranding(APPLICATION_TYPE, CHILD_APP_ID, DEFAULT_LOCALE);

            Assert.assertEquals(resolvedBrandingPreference.getName(), PARENT_ORG_ID);
            Assert.assertEquals(resolvedBrandingPreference.getLocale(), DEFAULT_LOCALE);
            Assert.assertEquals(resolvedBrandingPreference.getType(), ORGANIZATION_TYPE);
            Assert.assertEquals(resolvedBrandingPreference.getPreference(),
                    getPreferenceFromFile("sample-parent-org-branding-preference-without-display-name.json"));
        }
    }

    private void mockOSGiDataHolder(MockedStatic<OSGiDataHolder> mockedOSGiDataHolder)
            throws Exception {

        TenantManager tenantManager = mock(TenantManager.class);
        when(tenantManager.getTenantId(ROOT_TENANT_DOMAIN)).thenReturn(ROOT_TENANT_ID);
        when(tenantManager.getTenantId(PARENT_ORG_ID)).thenReturn(PARENT_TENANT_ID);
        when(tenantManager.getTenantId(CHILD_ORG_ID)).thenReturn(CHILD_TENANT_ID);

        UserRealmService userRealmService = mock(UserRealmService.class);
        when(userRealmService.getTenantManager()).thenReturn(tenantManager);

        OSGiDataHolder dataHolder = mock(OSGiDataHolder.class);
        when(dataHolder.getUserRealmService()).thenReturn(userRealmService);
        mockedOSGiDataHolder.when(OSGiDataHolder::getInstance).thenReturn(dataHolder);
    }

    private void mockBrandingPreferenceRetrieval(String resourceName, String resourceId, String resourceType,
                                                 String resourceFileName)
            throws ConfigurationManagementException, IOException {

        List<ResourceFile> resourceFiles =
                getResourceFiles(resourceName, resourceId, resourceType);
        when(configurationManager.getFiles(resourceType, resourceName)).thenReturn(resourceFiles);

        File sampleResourceFile = new File(getSamplesPath(resourceFileName));
        InputStream inputStream = FileUtils.openInputStream(sampleResourceFile);
        when(configurationManager.getFileById(resourceType, resourceName, resourceId)).thenReturn(inputStream);
    }

    private void setCarbonContextForTenant(String tenantDomain, int tenantId, String organizationId)
            throws UserStoreException {

        PrivilegedCarbonContext.startTenantFlow();
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain);
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(tenantId);
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setOrganizationId(organizationId);
        InMemoryRealmService testSessionRealmService = new InMemoryRealmService(tenantId);
        IdentityTenantUtil.setRealmService(testSessionRealmService);
    }

    private void setCarbonHome() {

        String carbonHome = Paths.get(System.getProperty("user.dir"), "target", "test-classes", "repository").
                toString();
        System.setProperty(CarbonBaseConstants.CARBON_HOME, carbonHome);
        System.setProperty(CarbonBaseConstants.CARBON_CONFIG_DIR_PATH, Paths.get(carbonHome, "conf").toString());
    }

    private Object getPreferenceFromFile(String filename) throws IOException {

        File sampleResourceFile = new File(getSamplesPath(filename));
        InputStream fileStream = FileUtils.openInputStream(sampleResourceFile);
        String preferencesJSON = convertInputStreamToString(fileStream);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(preferencesJSON, Object.class);
    }

    private String getSamplesPath(String sampleName) {

        if (StringUtils.isNotBlank(sampleName)) {
            return Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "samples",
                    sampleName).toString();
        }
        throw new IllegalArgumentException("Sample name cannot be empty.");
    }

    private String convertInputStreamToString(InputStream inputStream) throws IOException {

        StringBuilder stringBuilder = new StringBuilder();
        String line;

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
        }

        return stringBuilder.toString();
    }

    private static List<ResourceFile> getResourceFiles(String resourceName, String resourceId, String resourceType) {

        List<ResourceFile> resourceFiles = new ArrayList<>();
        ResourceFile resourceFile = new ResourceFile();
        resourceFile.setName(resourceName);
        resourceFile.setId(resourceId);
        resourceFile.setPath("/resource/" + resourceType + "/" + resourceName + "/file/" + resourceId);
        resourceFiles.add(resourceFile);
        return resourceFiles;
    }
}
