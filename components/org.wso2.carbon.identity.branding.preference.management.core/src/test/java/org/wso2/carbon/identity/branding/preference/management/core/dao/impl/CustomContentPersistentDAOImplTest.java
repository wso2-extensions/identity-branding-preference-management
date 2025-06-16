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

package org.wso2.carbon.identity.branding.preference.management.core.dao.impl;

import org.mockito.MockedStatic;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.branding.preference.management.core.dao.CustomContentPersistentDAO;
import org.wso2.carbon.identity.branding.preference.management.core.dao.cache.CustomContentCache;
import org.wso2.carbon.identity.branding.preference.management.core.exception.BrandingPreferenceMgtException;
import org.wso2.carbon.identity.branding.preference.management.core.model.CustomLayoutContent;
import org.wso2.carbon.identity.common.testng.WithCarbonHome;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;

import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertThrows;

/**
 * Test class for CustomContentPersistentDAOImpl.
 */
@WithCarbonHome
public class CustomContentPersistentDAOImplTest {

    private CustomContentPersistentDAO customContentPersistentDAO;

    private MockedStatic<OrgCustomContentDAOImpl> mockedOrgCustomContentDAO;
    private MockedStatic<AppCustomContentDAOImpl> mockedAppCustomContentDAO;
    private MockedStatic<IdentityTenantUtil> mockedIdentityTenantUtil;

    private OrgCustomContentDAOImpl orgCustomContentDAO;
    private AppCustomContentDAOImpl appCustomContentDAO;

    private static final String APPLICATION_UUID = "application-uuid";
    private static final String TENANT_DOMAIN_1 = "tenant-domain-1";
    private static final String TENANT_DOMAIN_2 = "tenant-domain-2";
    private static final int TENANT_ID_1 = 1;
    private static final int TENANT_ID_2 = 2;

    @BeforeClass
    public void init() {

        customContentPersistentDAO = CustomContentPersistentFactory.getCustomContentPersistentDAO();

        mockedAppCustomContentDAO = mockStatic(AppCustomContentDAOImpl.class);
        mockedOrgCustomContentDAO = mockStatic(OrgCustomContentDAOImpl.class);
        orgCustomContentDAO = mock(OrgCustomContentDAOImpl.class);
        mockedOrgCustomContentDAO.when(OrgCustomContentDAOImpl::getInstance).thenReturn(orgCustomContentDAO);
        appCustomContentDAO = mock(AppCustomContentDAOImpl.class);
        mockedAppCustomContentDAO.when(AppCustomContentDAOImpl::getInstance).thenReturn(appCustomContentDAO);
        mockedIdentityTenantUtil = mockStatic(IdentityTenantUtil.class);
        mockedIdentityTenantUtil.when(() -> IdentityTenantUtil.getTenantId(TENANT_DOMAIN_1)).thenReturn(TENANT_ID_1);
        mockedIdentityTenantUtil.when(() -> IdentityTenantUtil.getTenantId(TENANT_DOMAIN_2)).thenReturn(TENANT_ID_2);
        mockedIdentityTenantUtil.when(() -> IdentityTenantUtil.getTenantDomain(TENANT_ID_1))
                .thenReturn(TENANT_DOMAIN_1);
        mockedIdentityTenantUtil.when(() -> IdentityTenantUtil.getTenantDomain(TENANT_ID_2))
                .thenReturn(TENANT_DOMAIN_2);
    }

    @AfterClass
    public void tearDown() {

        mockedOrgCustomContentDAO.close();
        mockedAppCustomContentDAO.close();
        mockedIdentityTenantUtil.close();
    }

    @Test(description = "Test addCustomContent method")
    public void testAddCustomContent() throws Exception {

        CustomLayoutContent customLayoutContent =
                new CustomLayoutContent.CustomLayoutContentBuilder().setHtml("test-html").setCss("test-css")
                        .setJs("test-js").build();

        // Test adding custom content for organization.
        customContentPersistentDAO.addCustomContent(
                customLayoutContent, null, TENANT_DOMAIN_1);
        verify(orgCustomContentDAO).addOrgCustomContent(customLayoutContent, TENANT_ID_1);
        doThrow(BrandingPreferenceMgtException.class).when(orgCustomContentDAO)
                .addOrgCustomContent(customLayoutContent, TENANT_ID_1);
        assertThrows(BrandingPreferenceMgtException.class, () -> {
            customContentPersistentDAO.addCustomContent(customLayoutContent, null, TENANT_DOMAIN_1);
        });

        // Test adding custom content for application.
        customContentPersistentDAO.addCustomContent(
                customLayoutContent, APPLICATION_UUID, TENANT_DOMAIN_1);
        verify(appCustomContentDAO).addAppCustomContent(customLayoutContent, APPLICATION_UUID, TENANT_ID_1);
        doThrow(BrandingPreferenceMgtException.class).when(appCustomContentDAO)
                .addAppCustomContent(customLayoutContent, APPLICATION_UUID, TENANT_ID_1);
        assertThrows(BrandingPreferenceMgtException.class, () -> {
            customContentPersistentDAO.addCustomContent(customLayoutContent, APPLICATION_UUID, TENANT_DOMAIN_1);
        });
    }

    @Test(description = "Test getCustomContent method", dependsOnMethods = "testAddCustomContent")
    public void testGetCustomContent() throws Exception {

        CustomLayoutContent customLayoutContent =
                new CustomLayoutContent.CustomLayoutContentBuilder().setHtml("test-html").setCss("test-css")
                        .setJs("test-js").build();

        // Test getting custom content for organization.
        when(orgCustomContentDAO.getOrgCustomContent(TENANT_ID_1)).thenReturn(null);
        customContentPersistentDAO.getCustomContent(null, TENANT_DOMAIN_1);
        verify(orgCustomContentDAO).getOrgCustomContent(TENANT_ID_1);
        clearInvocations(orgCustomContentDAO);
        assertNull(customContentPersistentDAO.getCustomContent(null, TENANT_DOMAIN_1));
        verifyNoInteractions(orgCustomContentDAO);
        CustomContentCache.getInstance().clear(TENANT_ID_1);
        when(orgCustomContentDAO.getOrgCustomContent(TENANT_ID_1)).thenReturn(customLayoutContent);
        customContentPersistentDAO.getCustomContent(null, TENANT_DOMAIN_1);
        verify(orgCustomContentDAO).getOrgCustomContent(TENANT_ID_1);
        clearInvocations(orgCustomContentDAO);
        customContentPersistentDAO.getCustomContent(null, TENANT_DOMAIN_1);
        verifyNoInteractions(orgCustomContentDAO);
        when(orgCustomContentDAO.getOrgCustomContent(TENANT_ID_2)).thenThrow(BrandingPreferenceMgtException.class);
        assertThrows(BrandingPreferenceMgtException.class, () -> {
            customContentPersistentDAO.getCustomContent(null, TENANT_DOMAIN_2);
        });

        // Test getting custom content for application.
        when(appCustomContentDAO.getAppCustomContent(APPLICATION_UUID, TENANT_ID_1)).thenReturn(customLayoutContent);
        customContentPersistentDAO.getCustomContent(APPLICATION_UUID, TENANT_DOMAIN_1);
        verify(appCustomContentDAO).getAppCustomContent(APPLICATION_UUID, TENANT_ID_1);
        clearInvocations(appCustomContentDAO);
        customContentPersistentDAO.getCustomContent(APPLICATION_UUID, TENANT_DOMAIN_1);
        verifyNoInteractions(appCustomContentDAO);
        when(appCustomContentDAO.getAppCustomContent(APPLICATION_UUID, TENANT_ID_2)).thenThrow(
                BrandingPreferenceMgtException.class);
        assertThrows(BrandingPreferenceMgtException.class, () -> {
            customContentPersistentDAO.getCustomContent(APPLICATION_UUID, TENANT_DOMAIN_2);
        });
    }

    @Test(description = "Test updateCustomContent method", dependsOnMethods = "testGetCustomContent")
    public void testUpdateCustomContent() throws Exception {

        CustomLayoutContent customLayoutContent =
                new CustomLayoutContent.CustomLayoutContentBuilder().setHtml("updated-html").setCss("updated-css")
                        .setJs("updated-js").build();

        // Test updating custom content for organization.
        when(orgCustomContentDAO.getOrgCustomContent(TENANT_ID_1)).thenReturn(customLayoutContent);
        customContentPersistentDAO.updateCustomContent(customLayoutContent, null, TENANT_DOMAIN_1);
        verify(orgCustomContentDAO).updateOrgCustomContent(customLayoutContent, TENANT_ID_1);
        CustomLayoutContent updatedContent = customContentPersistentDAO.getCustomContent(null, TENANT_DOMAIN_1);
        assertEquals(updatedContent.getHtml(), "updated-html");
        assertEquals(updatedContent.getCss(), "updated-css");
        assertEquals(updatedContent.getJs(), "updated-js");
        doThrow(BrandingPreferenceMgtException.class).when(orgCustomContentDAO)
                .updateOrgCustomContent(customLayoutContent, TENANT_ID_1);
        assertThrows(BrandingPreferenceMgtException.class, () -> {
            customContentPersistentDAO.updateCustomContent(customLayoutContent, null, TENANT_DOMAIN_1);
        });

        // Test updating custom content for application.
        when(appCustomContentDAO.getAppCustomContent(APPLICATION_UUID, TENANT_ID_1)).thenReturn(customLayoutContent);
        customContentPersistentDAO.updateCustomContent(customLayoutContent, APPLICATION_UUID, TENANT_DOMAIN_1);
        verify(appCustomContentDAO).updateAppCustomContent(customLayoutContent, APPLICATION_UUID, TENANT_ID_1);
        updatedContent = customContentPersistentDAO.getCustomContent(APPLICATION_UUID, TENANT_DOMAIN_1);
        assertEquals(updatedContent.getHtml(), "updated-html");
        assertEquals(updatedContent.getCss(), "updated-css");
        assertEquals(updatedContent.getJs(), "updated-js");
        doThrow(BrandingPreferenceMgtException.class).when(appCustomContentDAO)
                .updateAppCustomContent(customLayoutContent, APPLICATION_UUID, TENANT_ID_1);
        assertThrows(BrandingPreferenceMgtException.class, () -> {
            customContentPersistentDAO.updateCustomContent(customLayoutContent, APPLICATION_UUID, TENANT_DOMAIN_1);
        });
    }

    @Test(description = "Test deleteCustomContent method", dependsOnMethods = "testUpdateCustomContent")
    public void testDeleteCustomContent() throws Exception {

        // Test deleting custom content for organization.
        when(orgCustomContentDAO.getOrgCustomContent(TENANT_ID_1)).thenReturn(null);
        customContentPersistentDAO.deleteCustomContent(null, TENANT_DOMAIN_1);
        verify(orgCustomContentDAO).deleteOrgCustomContent(TENANT_ID_1);
        CustomLayoutContent deletedContent = customContentPersistentDAO.getCustomContent(null, TENANT_DOMAIN_1);
        assertNull(deletedContent);
        doThrow(BrandingPreferenceMgtException.class).when(orgCustomContentDAO)
                .deleteOrgCustomContent(TENANT_ID_1);
        assertThrows(BrandingPreferenceMgtException.class, () -> {
            customContentPersistentDAO.deleteCustomContent(null, TENANT_DOMAIN_1);
        });

        // Test deleting custom content for application.
        when(appCustomContentDAO.getAppCustomContent(APPLICATION_UUID, TENANT_ID_1)).thenReturn(null);
        customContentPersistentDAO.deleteCustomContent(APPLICATION_UUID, TENANT_DOMAIN_1);
        verify(appCustomContentDAO).deleteAppCustomContent(APPLICATION_UUID, TENANT_ID_1);
        deletedContent = customContentPersistentDAO.getCustomContent(APPLICATION_UUID, TENANT_DOMAIN_1);
        assertNull(deletedContent);
        doThrow(BrandingPreferenceMgtException.class).when(appCustomContentDAO)
                .deleteAppCustomContent(APPLICATION_UUID, TENANT_ID_1);
        assertThrows(BrandingPreferenceMgtException.class, () -> {
            customContentPersistentDAO.deleteCustomContent(APPLICATION_UUID, TENANT_DOMAIN_1);
        });
    }
}
