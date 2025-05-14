/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.wso2.carbon.identity.branding.preference.management.core.dao.constants.DaoConstants.CustomContentTableColumns.TENANT_ID;
import static org.wso2.carbon.identity.branding.preference.management.core.dao.constants.DaoConstants.CustomContentTypes.CONTENT_TYPE_HTML;
import static org.wso2.carbon.identity.branding.preference.management.core.dao.constants.DaoConstants.CustomContentTypes.CONTENT_TYPE_CSS;
import static org.wso2.carbon.identity.branding.preference.management.core.dao.constants.DaoConstants.CustomContentTypes.CONTENT_TYPE_JS;
import static org.wso2.carbon.identity.branding.preference.management.core.dao.constants.SQLConstants.GET_ORG_CUSTOM_CONTENT_COUNT_SQL;
import static org.wso2.carbon.identity.branding.preference.management.core.dao.constants.SQLConstants.GET_ORG_CUSTOM_CONTENT_SQL;
import static org.wso2.carbon.identity.branding.preference.management.core.dao.constants.SQLConstants.INSERT_ORG_CUSTOM_CONTENT_SQL;
import static org.wso2.carbon.identity.branding.preference.management.core.dao.constants.SQLConstants.DELETE_ORG_CUSTOM_CONTENT_SQL;
import static org.wso2.carbon.identity.branding.preference.management.core.dao.constants.SQLConstants.UPDATE_ORG_CUSTOM_CONTENT_SQL;

/**
 * This class is to perform CRUD operations for Application vise Custom Content
 */

public class OrgCustomContentDAO {

    public boolean isOrgCustomContentAvailable(int tenantId) throws CustomContentServerException {
        NamedJdbcTemplate template = JdbcUtils.getNewNamedJdbcTemplate();
        try {
            Integer count = template.fetchSingleRecord(GET_ORG_CUSTOM_CONTENT_COUNT_SQL,
                    (resultSet, rowNum) -> resultSet.getInt(1),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setInt(1, tenantId);
                    });
            return count != null && count > 0;
        } catch (DataAccessException e) {
            throw new CustomContentServerException("Error checking if custom content exists for tenant " + tenantId,"",e);
        }
    }

    private static void insertContent(NamedJdbcTemplate template, String content, String contentType, int tenantId, Timestamp timestamp) throws CustomContentServerException {
        try {
            template.executeUpdate(INSERT_ORG_CUSTOM_CONTENT_SQL, namedPreparedStatement -> {
                namedPreparedStatement.setBinaryStream(1, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
                namedPreparedStatement.setString(2, contentType);
                namedPreparedStatement.setInt(3, tenantId);
                namedPreparedStatement.setTimestamp(4, timestamp);
                namedPreparedStatement.setTimestamp(5, timestamp);
            });
        } catch (DataAccessException e){
            throw new CustomContentServerException("Error while adding custom content to organization in " + tenantId + " tenant.", "",e);
        }
    }

    public void addOrgCustomContent(CustomContent content, int tenantId) throws CustomContentServerException {
        NamedJdbcTemplate template = JdbcUtils.getNewNamedJdbcTemplate();
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());

        insertContent(template, content.getHtmlContent(), CONTENT_TYPE_HTML, tenantId, now);
        insertContent(template, content.getCssContent(), CONTENT_TYPE_CSS, tenantId, now);
        insertContent(template, content.getJsContent(), CONTENT_TYPE_JS, tenantId, now);

    }

    private static void updateContent(NamedJdbcTemplate template, String content, String contentType, int tenantId, Timestamp timestamp) throws DataAccessException, CustomContentServerException {
        try{
            template.executeUpdate(UPDATE_ORG_CUSTOM_CONTENT_SQL, namedPreparedStatement -> {
                namedPreparedStatement.setBinaryStream(1, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
                namedPreparedStatement.setInt(3, tenantId);
                namedPreparedStatement.setString(4, contentType);
                namedPreparedStatement.setTimestamp(2, timestamp);
            });
        } catch (DataAccessException e){
            throw new CustomContentServerException("Error while updating custom content to organization in " + tenantId + " tenant.", "",e);
        }
    }

    public void updateOrgCustomContent(CustomContent content, int tenantId) throws CustomContentServerException{
        NamedJdbcTemplate template = JdbcUtils.getNewNamedJdbcTemplate();
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());

        try {
            updateContent(template, content.getHtmlContent(), CONTENT_TYPE_HTML, tenantId, now);
            updateContent(template, content.getCssContent(), CONTENT_TYPE_CSS, tenantId, now);
            updateContent(template, content.getJsContent(), CONTENT_TYPE_JS, tenantId, now);
        } catch (DataAccessException e) {
            throw new CustomContentServerException("Error while updating custom content for tenant " + tenantId, "",e);
        }
    }

    /**
     * @param tenantId
     * @return CustomContent
     */
    public CustomContent getOrgCustomContent(int tenantId) throws CustomContentServerException{
        CustomContent result = null;
        NamedJdbcTemplate template = JdbcUtils.getNewNamedJdbcTemplate();

        final String[] htmlContent = {""};
        final String[] cssContent = {""};
        final String[] jsContent = {""};

        try{
            template.executeQuery(
                    GET_ORG_CUSTOM_CONTENT_SQL,
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
                        namedPreparedStatement.setInt(TENANT_ID, tenantId);
                    }
            );
            result = new CustomContent(htmlContent[0], cssContent[0], jsContent[0]);
        } catch (DataAccessException e) {
            throw new CustomContentServerException("Error while fetching custom content for tenant " + tenantId,"", e);
        }
        return result;
    }

    public void deleteOrgCustomContent(int tenantId) throws CustomContentServerException {
        NamedJdbcTemplate namedJdbcTemplate = JdbcUtils.getNewNamedJdbcTemplate();
        try{
            namedJdbcTemplate.executeUpdate(DELETE_ORG_CUSTOM_CONTENT_SQL,
                    namedPreparedStatement -> {
                        namedPreparedStatement.setInt(TENANT_ID, tenantId);
                    });
        } catch (DataAccessException e){
            throw new CustomContentServerException("Error while deleting the custom content in " + tenantId + " tenant.", "",e);
        }
    }

}
