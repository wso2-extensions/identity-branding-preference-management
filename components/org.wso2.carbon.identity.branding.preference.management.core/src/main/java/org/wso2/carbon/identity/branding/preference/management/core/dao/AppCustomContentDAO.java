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

package org.wso2.carbon.identity.branding.preference.management.core.dao;

import org.wso2.carbon.database.utils.jdbc.NamedJdbcTemplate;
import org.wso2.carbon.database.utils.jdbc.exceptions.DataAccessException;
import org.wso2.carbon.identity.branding.preference.management.core.exception.CustomContentException;
import org.wso2.carbon.identity.branding.preference.management.core.exception.CustomContentServerException;
import org.wso2.carbon.identity.branding.preference.management.core.model.CustomContent;
import org.wso2.carbon.identity.core.util.JdbcUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.wso2.carbon.identity.branding.preference.management.core.dao.constants.DaoConstants.CustomContentTableColumns.*;
import static org.wso2.carbon.identity.branding.preference.management.core.dao.constants.DaoConstants.CustomContentTypes.CONTENT_TYPE_CSS;
import static org.wso2.carbon.identity.branding.preference.management.core.dao.constants.DaoConstants.CustomContentTypes.CONTENT_TYPE_HTML;
import static org.wso2.carbon.identity.branding.preference.management.core.dao.constants.DaoConstants.CustomContentTypes.CONTENT_TYPE_JS;
import static org.wso2.carbon.identity.branding.preference.management.core.dao.constants.SQLConstants.*;

/**
 * This class is to perform CRUD operations for Application vise Custom Content
 */

public class AppCustomContentDAO {

    /**
     * Checks whether custom content exists for the given APP.
     *
     * @param applicationUuid   Application UUID.
     * @param tenantId          Tenant ID.
     * @return                  {@code true} if custom content exists for the tenant, {@code false} otherwise.
     * @throws CustomContentServerException if an error occurs during the database access.
     */
    public boolean isAppCustomContentAvailable(String applicationUuid, int tenantId) throws CustomContentServerException {
        NamedJdbcTemplate template = JdbcUtils.getNewNamedJdbcTemplate();
        try {
            Integer count = template.fetchSingleRecord(GET_APP_CUSTOM_CONTENT_COUNT_SQL,
                    (resultSet, rowNum) -> resultSet.getInt(1),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(1, applicationUuid);
                        namedPreparedStatement.setInt(2, tenantId);
                    });
            return count != null && count > 0;
        } catch (DataAccessException e) {
            throw new CustomContentServerException("Error checking if custom content exists for application " + applicationUuid, "",e);
        }
    }

    /**
     * Inserts custom content for a specific APP.
     *
     * @param template The JDBC template to use for database operations.
     * @param content The content to insert.
     * @param contentType The type of the content (e.g., "html", "css", "js").
     * @param applicationUuid Application UUID.
     * @param tenantId Tenant ID.
     * @param timestamp The timestamp to be used for creation and update time.
     * @throws CustomContentServerException if an error occurs during content insertion.
     */
    private static void insertContent(NamedJdbcTemplate template, String content, String contentType, String applicationUuid, int tenantId, Timestamp timestamp) throws CustomContentServerException {
        try{
            template.executeUpdate(INSERT_APP_CUSTOM_CONTENT_SQL, namedPreparedStatement -> {
                namedPreparedStatement.setBinaryStream(1, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
                namedPreparedStatement.setString(2, contentType);
                namedPreparedStatement.setString(3, applicationUuid);
                namedPreparedStatement.setInt(4, tenantId);
                namedPreparedStatement.setTimestamp(5, timestamp);
                namedPreparedStatement.setTimestamp(6, timestamp);
            });
        } catch (DataAccessException e){
            throw new CustomContentServerException("Error while adding custom content to application " + applicationUuid , "",e);
        }
    }

    /**
     * Adds new custom content (HTML, CSS, JS) for a given APP.
     *
     * @param content The {@link CustomContent} object containing HTML, CSS, and JS content.
     * @param applicationUuid Application UUID.
     * @param tenantId Tenant ID.
     * @throws CustomContentServerException if an error occurs during insertion of any content.
     */
    public void addAppCustomContent(CustomContent content, String applicationUuid, int tenantId) throws CustomContentServerException {
        NamedJdbcTemplate template = JdbcUtils.getNewNamedJdbcTemplate();
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());

        insertContent(template, content.getHtmlContent(), CONTENT_TYPE_HTML, applicationUuid, tenantId, now);
        insertContent(template, content.getCssContent(), CONTENT_TYPE_CSS, applicationUuid, tenantId, now);
        insertContent(template, content.getJsContent(), CONTENT_TYPE_JS, applicationUuid, tenantId, now);

    }

    /**
     * Updates the custom content of a specific type for the given APP.
     *
     * @param template The JDBC template to use.
     * @param content The new content to be updated.
     * @param contentType The type of the content (e.g., "html", "css", "js").
     * @param applicationUuid Application UUID.
     * @param tenantId Tenant ID.
     * @param timestamp The timestamp to set as the update time.
     * @throws CustomContentServerException if a database error occurs during the update.
     */
    private static void updateContent(NamedJdbcTemplate template, String content, String contentType, String applicationUuid, int tenantId, Timestamp timestamp) throws DataAccessException,CustomContentServerException {
        try{
            template.executeUpdate(UPDATE_APP_CUSTOM_CONTENT_SQL, namedPreparedStatement -> {
                namedPreparedStatement.setBinaryStream(1, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
                namedPreparedStatement.setTimestamp(5, timestamp);
                namedPreparedStatement.setString(CONTENT_TYPE, contentType);
                namedPreparedStatement.setString(APP_ID, applicationUuid);
                namedPreparedStatement.setInt(TENANT_ID, tenantId);
            });
        } catch (DataAccessException e){
            throw new CustomContentServerException("Error while updating custom content in application " + applicationUuid, "", e);
        }
    }

    /**
     * Updates the custom content (HTML, CSS, JS) for the given APP.
     *
     * @param content The {@link CustomContent} object containing updated content.
     * @param applicationUuid Application UUID.
     * @param tenantId Tenant ID.
     * @throws CustomContentServerException if an error occurs during update.
     */
    public void updateAppCustomContent(CustomContent content, String applicationUuid, int tenantId) throws CustomContentServerException {
        NamedJdbcTemplate template = JdbcUtils.getNewNamedJdbcTemplate();
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());

        try {
            updateContent(template, content.getHtmlContent(), CONTENT_TYPE_HTML, applicationUuid, tenantId,now);
            updateContent(template, content.getCssContent(), CONTENT_TYPE_CSS, applicationUuid, tenantId,now);
            updateContent(template, content.getJsContent(), CONTENT_TYPE_JS, applicationUuid, tenantId,now);
        } catch (CustomContentServerException e) {
            throw new CustomContentServerException("Error while updating custom content in application " + applicationUuid, "", e);
        } catch (DataAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieves the custom content (HTML, CSS, JS) for the specified APP.
     *
     * @param applicationUuid Application UUID.
     * @param tenantId Tenant ID.
     * @return A {@link CustomContent} object containing the APP's custom content.
     * @throws CustomContentServerException if an error occurs while fetching the content.
     */
    public CustomContent getAppCustomContent(String applicationUuid, int tenantId) throws CustomContentServerException{
        CustomContent result = null;
        NamedJdbcTemplate template = JdbcUtils.getNewNamedJdbcTemplate();

        final String[] htmlContent = {""};
        final String[] cssContent = {""};
        final String[] jsContent = {""};

        try{
            template.executeQuery(
                    GET_APP_CUSTOM_CONTENT_SQL,
                    (resultSet, rowNum) -> {
                        String type = resultSet.getString("CONTENT_TYPE");
                        String content = new String(resultSet.getBytes("CONTENT"));

                        switch (type) {
                            case "html": htmlContent[0] = content; break;
                            case "css": cssContent[0] = content; break;
                            case "js": jsContent[0] = content; break;
                        }
                        return null;
                    },
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(APP_ID, applicationUuid);
                        namedPreparedStatement.setInt(TENANT_ID, tenantId);
                    }
            );
            result = new CustomContent(htmlContent[0], cssContent[0], jsContent[0]);
        } catch (DataAccessException e) {
            throw new CustomContentServerException("Error while fetching custom content for application " + applicationUuid, "", e);
        }
        return result;
    }

    /**
     * Deletes all custom content (HTML, CSS, JS) for the specified APP.
     *
     * @param applicationUuid Application UUID.
     * @param tenantId Tenant ID.
     * @throws CustomContentServerException if an error occurs during deletion.
     */
    public void deleteAppCustomContent(String applicationUuid, int tenantId) throws CustomContentServerException{
        NamedJdbcTemplate namedJdbcTemplate = JdbcUtils.getNewNamedJdbcTemplate();
        try{
            namedJdbcTemplate.executeUpdate(DELETE_APP_CUSTOM_CONTENT_SQL,
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(APP_ID, applicationUuid);
                        namedPreparedStatement.setInt(TENANT_ID, tenantId);
                    });
        } catch (DataAccessException e){
            throw new CustomContentServerException("Error while deleting the custom content in " + applicationUuid, "", e);
        }
    }

}
