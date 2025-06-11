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

import org.mockito.MockedConstruction;
import org.testng.annotations.Test;
import org.wso2.carbon.database.utils.jdbc.NamedQueryFilter;
import org.wso2.carbon.database.utils.jdbc.NamedTemplate;
import org.wso2.carbon.database.utils.jdbc.RowMapper;
import org.wso2.carbon.database.utils.jdbc.exceptions.DataAccessException;
import org.wso2.carbon.identity.branding.preference.management.core.dao.OrgCustomContentDAO;
import org.wso2.carbon.identity.branding.preference.management.core.exception.BrandingPreferenceMgtException;
import org.wso2.carbon.identity.branding.preference.management.core.model.CustomLayoutContent;
import org.wso2.carbon.identity.common.testng.WithCarbonHome;
import org.wso2.carbon.identity.common.testng.WithH2Database;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertThrows;

/**
 * Test class for OrgCustomContentDAOImpl.
 */
@WithCarbonHome
@WithH2Database(files = {"dbscripts/config/h2.sql"})
public class OrgCustomContentDAOImplTest {

    private final OrgCustomContentDAO orgCustomContentDAO = OrgCustomContentDAOImpl.getInstance();
    private static final int TEST_TENANT_ID_1 = 1;
    private static final int TEST_TENANT_ID_2 = 2;

    @Test(description = "Test for adding org custom content")
    public void testAddOrgCustomContent() throws Exception {

        // Test successful addition of org custom content.
        CustomLayoutContent customLayoutContent =
                new CustomLayoutContent.CustomLayoutContentBuilder().setHtml("test-html").setCss("test-css")
                        .setJs("test-js").build();
        orgCustomContentDAO.addOrgCustomContent(customLayoutContent, -1234);

        // Test the unique constraint violation when adding the same org custom content again.
        assertThrows(BrandingPreferenceMgtException.class, () -> {
            orgCustomContentDAO.addOrgCustomContent(customLayoutContent, -1234);
        });

        // Try to add null custom layout content.
        orgCustomContentDAO.addOrgCustomContent(null, TEST_TENANT_ID_1);

        // Test adding app custom content only with HTML.
        CustomLayoutContent customLayoutContent2 =
                new CustomLayoutContent.CustomLayoutContentBuilder().setHtml("test-html").build();
        orgCustomContentDAO.addOrgCustomContent(customLayoutContent2, TEST_TENANT_ID_1);

        // Test adding app custom content when the database query fails.
        try (MockedConstruction<NamedTemplate> mockedNamedTemplate = mockConstruction(NamedTemplate.class,
                (mock, context) -> {
                    when(mock.executeInsert(any(String.class), any(NamedQueryFilter.class), isNull(), anyBoolean()))
                            .thenThrow(DataAccessException.class);
                })) {
            assertThrows(BrandingPreferenceMgtException.class, () -> {
                orgCustomContentDAO.addOrgCustomContent(customLayoutContent2, TEST_TENANT_ID_2);
            });
        }
    }

    @Test(description = "Test for getting org custom content", dependsOnMethods = { "testAddOrgCustomContent" })
    public void testGetOrgCustomContent() throws Exception {

        // Test successful retrieval of org custom content.
        CustomLayoutContent customLayoutContent = orgCustomContentDAO.getOrgCustomContent(-1234);
        assertEquals(customLayoutContent.getHtml(), "test-html");
        assertEquals(customLayoutContent.getCss(), "test-css");
        assertEquals(customLayoutContent.getJs(), "test-js");
        customLayoutContent = orgCustomContentDAO.getOrgCustomContent(TEST_TENANT_ID_1);
        assertEquals(customLayoutContent.getHtml(), "test-html");
        assertNull(customLayoutContent.getCss());
        assertNull(customLayoutContent.getJs());

        // Test failure retrieval of org custom content with correct tenant id.
        customLayoutContent = orgCustomContentDAO.getOrgCustomContent(TEST_TENANT_ID_2);
        assertNull(customLayoutContent);

        // Test retrieval of org custom content when the database query fails.
        try (MockedConstruction<NamedTemplate> mockedNamedTemplate = mockConstruction(NamedTemplate.class,
                (mock, context) -> {
                    when(mock.executeQuery(any(String.class), any(RowMapper.class), any(NamedQueryFilter.class)))
                            .thenThrow(DataAccessException.class);
                })) {
            assertThrows(BrandingPreferenceMgtException.class, () -> {
                orgCustomContentDAO.getOrgCustomContent(-1234);
            });
        }
    }

    @Test(description = "Test for updating org custom content", dependsOnMethods = { "testGetOrgCustomContent" })
    public void testUpdateOrgCustomContent() throws Exception {

        // Test successful update of org custom content.
        CustomLayoutContent customLayoutContent =
                new CustomLayoutContent.CustomLayoutContentBuilder().setHtml("updated-html").setCss("updated-css")
                        .setJs("updated-js").build();
        orgCustomContentDAO.updateOrgCustomContent(customLayoutContent, -1234);
        CustomLayoutContent updatedContent = orgCustomContentDAO.getOrgCustomContent(-1234);
        assertEquals(updatedContent.getHtml(), "updated-html");
        assertEquals(updatedContent.getCss(), "updated-css");
        assertEquals(updatedContent.getJs(), "updated-js");

        // Test the rollback when inserting fails.
        try (MockedConstruction<NamedTemplate> mockedConstruction = mockConstruction(NamedTemplate.class,
                (mock, context) -> {
                    when(mock.executeBatchInsert(any(String.class), any(NamedQueryFilter.class), isNull()))
                            .thenThrow(DataAccessException.class);
                    doCallRealMethod().when(mock).executeUpdate(any(String.class), any(NamedQueryFilter.class));
                    doCallRealMethod().when(mock)
                            .executeQuery(any(String.class), any(RowMapper.class), any(NamedQueryFilter.class));
                })) {
            CustomLayoutContent customLayoutContent1 = new CustomLayoutContent.CustomLayoutContentBuilder()
                    .setHtml("updated1-html").setCss("updated1-css").setJs("updated1-js").build();
            assertThrows(BrandingPreferenceMgtException.class, () -> {
                orgCustomContentDAO.updateOrgCustomContent(customLayoutContent1, -1234);
            });
            updatedContent = orgCustomContentDAO.getOrgCustomContent(-1234);
            assertEquals(updatedContent.getHtml(), "updated-html");
            assertEquals(updatedContent.getCss(), "updated-css");
            assertEquals(updatedContent.getJs(), "updated-js");
        }

        // Test updating with null custom layout content.
        orgCustomContentDAO.updateOrgCustomContent(null, TEST_TENANT_ID_1);
        updatedContent = orgCustomContentDAO.getOrgCustomContent(TEST_TENANT_ID_1);
        assertNull(updatedContent);

        // Test removing CSS and JS from custom layout content.
        CustomLayoutContent customLayoutContent2 =
                new CustomLayoutContent.CustomLayoutContentBuilder().setHtml("test-html").build();
        orgCustomContentDAO.updateOrgCustomContent(customLayoutContent2, -1234);
        updatedContent = orgCustomContentDAO.getOrgCustomContent(-1234);
        assertEquals(updatedContent.getHtml(), "test-html");
        assertNull(updatedContent.getCss());
        assertNull(updatedContent.getJs());
    }

    @Test(description = "Test for deleting org custom content", dependsOnMethods = { "testUpdateOrgCustomContent" })
    public void testDeleteOrgCustomContent() throws Exception {

        // Test successful deletion of org custom content.
        orgCustomContentDAO.deleteOrgCustomContent(-1234);
        CustomLayoutContent deletedContent = orgCustomContentDAO.getOrgCustomContent(-1234);
        assertNull(deletedContent);

        // Test deletion of org custom content when the database query fails.
        try (MockedConstruction<NamedTemplate> mockedNamedTemplate = mockConstruction(NamedTemplate.class,
                (mock, context) -> {
                    doThrow(DataAccessException.class).when(mock)
                            .executeUpdate(any(String.class), any(NamedQueryFilter.class));
                })) {
            assertThrows(BrandingPreferenceMgtException.class, () -> {
                orgCustomContentDAO.deleteOrgCustomContent(-1234);
            });
        }
    }
}
