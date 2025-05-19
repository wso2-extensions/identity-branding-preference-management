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
import org.wso2.carbon.identity.branding.preference.management.core.exception.BrandingPreferenceMgtException;
import org.wso2.carbon.identity.branding.preference.management.core.model.CustomLayoutContent;
import org.wso2.carbon.identity.core.util.JdbcUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_CUSTOM_LAYOUT_CONTENT_NOT_EXISTS;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_ADDING_CUSTOM_LAYOUT_CONTENT;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_CHECKING_CUSTOM_LAYOUT_CONTENT_EXISTS;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_DELETING_CUSTOM_LAYOUT_CONTENT;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_GETTING_CUSTOM_LAYOUT_CONTENT;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_UPDATING_CUSTOM_LAYOUT_CONTENT;
import static org.wso2.carbon.identity.branding.preference.management.core.dao.constants.DAOConstants.CustomContentTableColumns.CONTENT;
import static org.wso2.carbon.identity.branding.preference.management.core.dao.constants.DAOConstants.CustomContentTableColumns.CONTENT_TYPE;
import static org.wso2.carbon.identity.branding.preference.management.core.dao.constants.DAOConstants.CustomContentTableColumns.TENANT_ID;
import static org.wso2.carbon.identity.branding.preference.management.core.dao.constants.DAOConstants.CustomContentTypes.CONTENT_TYPE_CSS;
import static org.wso2.carbon.identity.branding.preference.management.core.dao.constants.DAOConstants.CustomContentTypes.CONTENT_TYPE_HTML;
import static org.wso2.carbon.identity.branding.preference.management.core.dao.constants.DAOConstants.CustomContentTypes.CONTENT_TYPE_JS;
import static org.wso2.carbon.identity.branding.preference.management.core.dao.constants.SQLConstants.DELETE_ORG_CUSTOM_CONTENT_SQL;
import static org.wso2.carbon.identity.branding.preference.management.core.dao.constants.SQLConstants.GET_ORG_CUSTOM_CONTENT_COUNT_SQL;
import static org.wso2.carbon.identity.branding.preference.management.core.dao.constants.SQLConstants.GET_ORG_CUSTOM_CONTENT_SQL;
import static org.wso2.carbon.identity.branding.preference.management.core.dao.constants.SQLConstants.INSERT_ORG_CUSTOM_CONTENT_SQL;
import static org.wso2.carbon.identity.branding.preference.management.core.dao.constants.SQLConstants.UPDATE_ORG_CUSTOM_CONTENT_SQL;
import static org.wso2.carbon.identity.branding.preference.management.core.util.BrandingPreferenceMgtUtils.handleServerException;

/**
 * This class is to perform CRUD operations for Organization vise Custom Layout Content
 */
public class OrgCustomContentDAOImpl implements OrgCustomContentDAO {

    @Override
    public boolean isOrgCustomContentAvailable(int tenantId) throws BrandingPreferenceMgtException {

        NamedJdbcTemplate template = JdbcUtils.getNewNamedJdbcTemplate();

        try {
            Integer count = template.fetchSingleRecord(GET_ORG_CUSTOM_CONTENT_COUNT_SQL,
                    (resultSet, rowNum) -> resultSet.getInt(1),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setInt(1, tenantId);
                    });
            if (count == 0) {
                throw handleServerException(ERROR_CODE_CUSTOM_LAYOUT_CONTENT_NOT_EXISTS, String.valueOf(tenantId));
            }
            return count > 0;
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_CHECKING_CUSTOM_LAYOUT_CONTENT_EXISTS,
                    String.valueOf(tenantId), e);
        }
    }

    /**
     * Inserts custom layout content for a specific ORG.
     *
     * @param template The JDBC template to use for database operations.
     * @param content The content to insert.
     * @param contentType The type of the content (e.g., "html", "css", "js").
     * @param tenantId Tenant ID.
     * @param timestamp The timestamp to be used for creation and update time.
     * @throws BrandingPreferenceMgtException if an error occurs during content insertion.
     */
    private static void insertContent(NamedJdbcTemplate template, String content, String contentType,
                                      int tenantId, Timestamp timestamp) throws BrandingPreferenceMgtException {

        try {
            template.executeUpdate(INSERT_ORG_CUSTOM_CONTENT_SQL, namedPreparedStatement -> {
                namedPreparedStatement.setBinaryStream(1,
                        new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
                namedPreparedStatement.setString(2, contentType);
                namedPreparedStatement.setInt(3, tenantId);
                namedPreparedStatement.setTimestamp(4, timestamp);
                namedPreparedStatement.setTimestamp(5, timestamp);
            });
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_ADDING_CUSTOM_LAYOUT_CONTENT, String.valueOf(tenantId), e);
        }
    }

    @Override
    public void addOrgCustomContent(CustomLayoutContent content, int tenantId) throws BrandingPreferenceMgtException {

        NamedJdbcTemplate template = JdbcUtils.getNewNamedJdbcTemplate();
        Timestamp currentTime = Timestamp.valueOf(LocalDateTime.now());

        insertContent(template, content.getHtmlContent(), CONTENT_TYPE_HTML, tenantId, currentTime);
        insertContent(template, content.getCssContent(), CONTENT_TYPE_CSS, tenantId, currentTime);
        insertContent(template, content.getJsContent(), CONTENT_TYPE_JS, tenantId, currentTime);
    }

    /**
     * Updates the custom layout content of a specific type for the given ORG.
     *
     * @param template The JDBC template to use.
     * @param content The new content to be updated.
     * @param contentType The type of the content (e.g., "html", "css", "js").
     * @param tenantId The tenant ID to which the content belongs.
     * @param timestamp The timestamp to set as the update time.
     * @throws BrandingPreferenceMgtException if a database error occurs during the update.
     */
    private static void updateContent(NamedJdbcTemplate template, String content, String contentType, int tenantId,
                                      Timestamp timestamp) throws BrandingPreferenceMgtException {

        try {
            template.executeUpdate(UPDATE_ORG_CUSTOM_CONTENT_SQL, namedPreparedStatement -> {
                namedPreparedStatement.setBinaryStream(1,
                        new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
                namedPreparedStatement.setInt(3, tenantId);
                namedPreparedStatement.setString(4, contentType);
                namedPreparedStatement.setTimestamp(2, timestamp);
            });
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_UPDATING_CUSTOM_LAYOUT_CONTENT, String.valueOf(tenantId), e);
        }
    }

    @Override
    public void updateOrgCustomContent(CustomLayoutContent content, int tenantId)
            throws BrandingPreferenceMgtException {

        NamedJdbcTemplate template = JdbcUtils.getNewNamedJdbcTemplate();
        Timestamp currentTime = Timestamp.valueOf(LocalDateTime.now());

        try {
            updateContent(template, content.getHtmlContent(), CONTENT_TYPE_HTML, tenantId, currentTime);
            updateContent(template, content.getCssContent(), CONTENT_TYPE_CSS, tenantId, currentTime);
            updateContent(template, content.getJsContent(), CONTENT_TYPE_JS, tenantId, currentTime);
        } catch (BrandingPreferenceMgtException e) {
            throw handleServerException(ERROR_CODE_ERROR_UPDATING_CUSTOM_LAYOUT_CONTENT, String.valueOf(tenantId), e);
        }
    }

    @Override
    public CustomLayoutContent getOrgCustomContent(int tenantId) throws BrandingPreferenceMgtException {

        CustomLayoutContent result;
        NamedJdbcTemplate template = JdbcUtils.getNewNamedJdbcTemplate();

        final String[] htmlContent = {""};
        final String[] cssContent = {""};
        final String[] jsContent = {""};

        try {
            template.executeQuery(
                    GET_ORG_CUSTOM_CONTENT_SQL,
                    (resultSet, rowNum) -> {
                        String type = resultSet.getString(CONTENT_TYPE);
                        String content = new String(resultSet.getBytes(CONTENT), StandardCharsets.UTF_8);

                        switch (type) {
                            case CONTENT_TYPE_HTML: htmlContent[0] = content;
                            break;
                            case CONTENT_TYPE_CSS: cssContent[0] = content;
                            break;
                            case CONTENT_TYPE_JS: jsContent[0] = content;
                            break;
                        }
                        return null;
                    },
                    namedPreparedStatement -> {
                        namedPreparedStatement.setInt(TENANT_ID, tenantId);
                    }
            );
            result = new CustomLayoutContent(htmlContent[0], cssContent[0], jsContent[0]);
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_GETTING_CUSTOM_LAYOUT_CONTENT, String.valueOf(tenantId), e);
        }
        return result;
    }

    @Override
    public void deleteOrgCustomContent(int tenantId) throws BrandingPreferenceMgtException {

        NamedJdbcTemplate namedJdbcTemplate = JdbcUtils.getNewNamedJdbcTemplate();
        try {
            namedJdbcTemplate.executeUpdate(DELETE_ORG_CUSTOM_CONTENT_SQL, namedPreparedStatement -> {
                namedPreparedStatement.setInt(TENANT_ID, tenantId);
                    });
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_DELETING_CUSTOM_LAYOUT_CONTENT, String.valueOf(tenantId), e);
        }
    }
}
