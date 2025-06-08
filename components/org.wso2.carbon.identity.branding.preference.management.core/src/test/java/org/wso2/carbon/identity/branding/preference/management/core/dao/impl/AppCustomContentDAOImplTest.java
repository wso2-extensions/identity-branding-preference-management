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
import org.wso2.carbon.identity.branding.preference.management.core.dao.AppCustomContentDAO;
import org.wso2.carbon.identity.branding.preference.management.core.exception.BrandingPreferenceMgtException;
import org.wso2.carbon.identity.branding.preference.management.core.model.CustomLayoutContent;
import org.wso2.carbon.identity.common.testng.WithCarbonHome;
import org.wso2.carbon.identity.common.testng.WithH2Database;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertThrows;

/**
 * Test class for AppCustomContentDAOImpl.
 */
@WithCarbonHome
@WithH2Database(files = {"dbscripts/config/h2.sql"})
public class AppCustomContentDAOImplTest {

    private final AppCustomContentDAO appCustomContentDAO = AppCustomContentDAOImpl.getInstance();
    private static final String TEST_APP_ID_1 = "550e8400-e29b-41d4-a716-446655440000";
    private static final String TEST_APP_ID_2 = "550e8400-e29b-41d4-a716-446655440001";
    private static final String TEST_APP_ID_3 = "550e8400-e29b-41d4-a716-446655440002";

    @Test(description = "Test for adding app custom content")
    public void testAddAppCustomContent() throws Exception {

        // Test successful addition of app custom content.
        CustomLayoutContent customLayoutContent =
                new CustomLayoutContent.CustomLayoutContentBuilder().setHtml("test-html").setCss("test-css")
                        .setJs("test-js").build();
        appCustomContentDAO.addAppCustomContent(customLayoutContent, TEST_APP_ID_1, -1234);

        // Test the unique constraint violation when adding the same app custom content again.
        assertThrows(BrandingPreferenceMgtException.class, () -> {
            appCustomContentDAO.addAppCustomContent(customLayoutContent, TEST_APP_ID_1, -1234);
        });

        // Try to add null custom layout content.
        appCustomContentDAO.addAppCustomContent(null, TEST_APP_ID_2, -1234);

        // Test adding app custom content only with HTML.
        CustomLayoutContent customLayoutContent2 =
                new CustomLayoutContent.CustomLayoutContentBuilder().setHtml("test-html").build();
        appCustomContentDAO.addAppCustomContent(customLayoutContent2, TEST_APP_ID_3, -1234);

        // Test adding app custom content with null values.
        assertThrows(BrandingPreferenceMgtException.class, () -> {
            appCustomContentDAO.addAppCustomContent(customLayoutContent2, null, -1234);
        });
    }

    @Test(description = "Test for getting app custom content", dependsOnMethods = { "testAddAppCustomContent" })
    public void testGetAppCustomContent() throws Exception {

        // Test successful retrieval of app custom content.
        CustomLayoutContent customLayoutContent = appCustomContentDAO.getAppCustomContent(TEST_APP_ID_1, -1234);
        assertEquals(customLayoutContent.getHtml(), "test-html");
        assertEquals(customLayoutContent.getCss(), "test-css");
        assertEquals(customLayoutContent.getJs(), "test-js");
        customLayoutContent = appCustomContentDAO.getAppCustomContent(TEST_APP_ID_3, -1234);
        assertEquals(customLayoutContent.getHtml(), "test-html");
        assertNull(customLayoutContent.getCss());
        assertNull(customLayoutContent.getJs());

        // Test failure retrieval of app custom content with wrong app id.
        customLayoutContent = appCustomContentDAO.getAppCustomContent("wrong-app-id", -1234);
        assertNull(customLayoutContent);

        // Test failure retrieval of app custom content with correct app id.
        customLayoutContent = appCustomContentDAO.getAppCustomContent(TEST_APP_ID_2, -1234);
        assertNull(customLayoutContent);

        // Test retrieval of app custom content when the database query fails.
        try (MockedConstruction<NamedTemplate> mockedNamedTemplate = mockConstruction(NamedTemplate.class,
                (mock, context) -> {
            when(mock.executeQuery(any(String.class), any(RowMapper.class), any(NamedQueryFilter.class)))
                    .thenThrow(DataAccessException.class);
        })) {
            assertThrows(BrandingPreferenceMgtException.class, () -> {
                appCustomContentDAO.getAppCustomContent(null, -1234);
            });
        }
    }

    @Test(description = "Test for updating app custom content", dependsOnMethods = { "testGetAppCustomContent" })
    public void testUpdateAppCustomContent() throws Exception {

        // Test successful update of app custom content.
        CustomLayoutContent customLayoutContent =
                new CustomLayoutContent.CustomLayoutContentBuilder().setHtml("updated-html").setCss("updated-css")
                        .setJs("updated-js").build();
        appCustomContentDAO.updateAppCustomContent(customLayoutContent, TEST_APP_ID_1, -1234);
        CustomLayoutContent updatedContent = appCustomContentDAO.getAppCustomContent(TEST_APP_ID_1, -1234);
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
                appCustomContentDAO.updateAppCustomContent(customLayoutContent1, TEST_APP_ID_1, -1234);
            });
            updatedContent = appCustomContentDAO.getAppCustomContent(TEST_APP_ID_1, -1234);
            assertEquals(updatedContent.getHtml(), "updated-html");
            assertEquals(updatedContent.getCss(), "updated-css");
            assertEquals(updatedContent.getJs(), "updated-js");
        }

        // Test updating with null custom layout content.
        appCustomContentDAO.updateAppCustomContent(null, TEST_APP_ID_3, -1234);
        updatedContent = appCustomContentDAO.getAppCustomContent(TEST_APP_ID_3, -1234);
        assertNull(updatedContent);

        // Test removing CSS and JS from custom layout content.
        CustomLayoutContent customLayoutContent2 =
                new CustomLayoutContent.CustomLayoutContentBuilder().setHtml("test-html").build();
        appCustomContentDAO.updateAppCustomContent(customLayoutContent2, TEST_APP_ID_1, -1234);
        updatedContent = appCustomContentDAO.getAppCustomContent(TEST_APP_ID_1, -1234);
        assertEquals(updatedContent.getHtml(), "test-html");
        assertNull(updatedContent.getCss());
        assertNull(updatedContent.getJs());
    }

    @Test(description = "Test for deleting app custom content", dependsOnMethods = { "testUpdateAppCustomContent" })
    public void testDeleteAppCustomContent() throws Exception {

        // Test successful deletion of app custom content.
        appCustomContentDAO.deleteAppCustomContent(TEST_APP_ID_1, -1234);
        CustomLayoutContent deletedContent = appCustomContentDAO.getAppCustomContent(TEST_APP_ID_1, -1234);
        assertNull(deletedContent);

        // Test deletion of app custom content when the database query fails.
        try (MockedConstruction<NamedTemplate> mockedNamedTemplate = mockConstruction(NamedTemplate.class,
                (mock, context) -> {
                    doThrow(DataAccessException.class).when(mock)
                            .executeUpdate(any(String.class), any(NamedQueryFilter.class));
        })) {
            assertThrows(BrandingPreferenceMgtException.class, () -> {
                appCustomContentDAO.deleteAppCustomContent(null, -1234);
            });
        }
    }
}
