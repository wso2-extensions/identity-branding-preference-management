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

import org.wso2.carbon.database.utils.jdbc.NamedJdbcTemplate;
import org.wso2.carbon.database.utils.jdbc.NamedTemplate;
import org.wso2.carbon.database.utils.jdbc.exceptions.DataAccessException;
import org.wso2.carbon.database.utils.jdbc.exceptions.TransactionException;
import org.wso2.carbon.identity.branding.preference.management.core.dao.OrgCustomContentDAO;
import org.wso2.carbon.identity.branding.preference.management.core.exception.BrandingPreferenceMgtException;
import org.wso2.carbon.identity.branding.preference.management.core.model.CustomLayoutContent;
import org.wso2.carbon.identity.branding.preference.management.core.util.BrandingPreferenceMgtUtils;
import org.wso2.carbon.identity.core.util.JdbcUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;

import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.CustomContentTypes.CONTENT_TYPE_CSS;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.CustomContentTypes.CONTENT_TYPE_HTML;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.CustomContentTypes.CONTENT_TYPE_JS;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_ADDING_CUSTOM_LAYOUT_CONTENT;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_DELETING_CUSTOM_LAYOUT_CONTENT;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_GETTING_CUSTOM_LAYOUT_CONTENT;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_UPDATING_CUSTOM_LAYOUT_CONTENT;
import static org.wso2.carbon.identity.branding.preference.management.core.dao.constants.SQLConstants.CustomContentTableColumns.CONTENT;
import static org.wso2.carbon.identity.branding.preference.management.core.dao.constants.SQLConstants.CustomContentTableColumns.CONTENT_TYPE;
import static org.wso2.carbon.identity.branding.preference.management.core.dao.constants.SQLConstants.CustomContentTableColumns.TENANT_ID;
import static org.wso2.carbon.identity.branding.preference.management.core.dao.constants.SQLConstants.DELETE_ORG_CUSTOM_CONTENT_SQL;
import static org.wso2.carbon.identity.branding.preference.management.core.dao.constants.SQLConstants.GET_ORG_CUSTOM_CONTENT_SQL;
import static org.wso2.carbon.identity.branding.preference.management.core.dao.constants.SQLConstants.INSERT_ORG_CUSTOM_CONTENT_SQL;
import static org.wso2.carbon.identity.branding.preference.management.core.util.BrandingPreferenceMgtUtils.handleServerException;

/**
 * This class is to perform CRUD operations for Organization vise Custom Layout Content
 */
class OrgCustomContentDAOImpl implements OrgCustomContentDAO {

    private static final OrgCustomContentDAO instance = new OrgCustomContentDAOImpl();

    private OrgCustomContentDAOImpl() {}

    public static OrgCustomContentDAO getInstance() {

        return instance;
    }

    @Override
    public void addOrgCustomContent(CustomLayoutContent content, int tenantId) throws BrandingPreferenceMgtException {

        NamedJdbcTemplate jdbcTemplate = JdbcUtils.getNewNamedJdbcTemplate();
        try {
            jdbcTemplate.withTransaction(template -> {
                insertContent(template, content, tenantId);
                return null;
            });
        } catch (TransactionException e) {
            throw handleServerException(ERROR_CODE_ERROR_ADDING_CUSTOM_LAYOUT_CONTENT, String.valueOf(tenantId), e);
        }
    }

    @Override
    public void updateOrgCustomContent(CustomLayoutContent content, int tenantId)
            throws BrandingPreferenceMgtException {

        NamedJdbcTemplate jdbcTemplate = JdbcUtils.getNewNamedJdbcTemplate();
        try {
            jdbcTemplate.withTransaction(template -> {
                deleteContent(template, tenantId);
                insertContent(template, content, tenantId);
                return null;
            });
        } catch (TransactionException e) {
            throw handleServerException(ERROR_CODE_ERROR_UPDATING_CUSTOM_LAYOUT_CONTENT, String.valueOf(tenantId), e);
        }
    }

    @Override
    public CustomLayoutContent getOrgCustomContent(int tenantId) throws BrandingPreferenceMgtException {

        NamedJdbcTemplate jdbcTemplate = JdbcUtils.getNewNamedJdbcTemplate();
        CustomLayoutContent.CustomLayoutContentBuilder customLayoutContentBuilder =
                new CustomLayoutContent.CustomLayoutContentBuilder();
        try {
            jdbcTemplate.withTransaction(template -> {
                template.executeQuery(
                        GET_ORG_CUSTOM_CONTENT_SQL,
                        (resultSet, rowNum) -> {
                            String type = resultSet.getString(CONTENT_TYPE);
                            String content = new String(resultSet.getBytes(CONTENT), StandardCharsets.UTF_8);

                            switch (type) {
                                case CONTENT_TYPE_HTML:
                                    customLayoutContentBuilder.setHtml(content);
                                    break;
                                case CONTENT_TYPE_CSS:
                                    customLayoutContentBuilder.setCss(content);
                                    break;
                                case CONTENT_TYPE_JS:
                                    customLayoutContentBuilder.setJs(content);
                                    break;
                            }
                            return null;
                        },
                        namedPreparedStatement -> {
                            namedPreparedStatement.setInt(TENANT_ID, tenantId);
                        }
                                     );
                return null;
            });
            return customLayoutContentBuilder.build();
        } catch (TransactionException e) {
            throw handleServerException(ERROR_CODE_ERROR_GETTING_CUSTOM_LAYOUT_CONTENT, String.valueOf(tenantId), e);
        }
    }

    @Override
    public void deleteOrgCustomContent(int tenantId) throws BrandingPreferenceMgtException {

        NamedJdbcTemplate jdbcTemplate = JdbcUtils.getNewNamedJdbcTemplate();
        try {
            jdbcTemplate.withTransaction(template -> {
                deleteContent(template, tenantId);
                return null;
            });
        } catch (TransactionException e) {
            throw handleServerException(ERROR_CODE_ERROR_DELETING_CUSTOM_LAYOUT_CONTENT, String.valueOf(tenantId), e);
        }
    }

    /**
     * Inserts custom layout content for a specific ORG.
     *
     * @param template The JDBC template to use for database operations.
     * @param content  The content to insert.
     * @param tenantId Tenant ID.
     * @throws DataAccessException If an error occurs during content insertion.
     */
    private void insertContent(NamedTemplate<Object> template, CustomLayoutContent content, int tenantId)
            throws DataAccessException {

        Map<String, String> contents = BrandingPreferenceMgtUtils.resolveContentTypes(content);

        if (contents.isEmpty()) {
            return;
        }

        Iterator<String> iterator = contents.keySet().iterator();
        if (contents.size() == 1) {
            String contentKey = iterator.next();
            template.executeInsert(INSERT_ORG_CUSTOM_CONTENT_SQL, namedPreparedStatement -> {
                namedPreparedStatement.setBinaryStream(CONTENT,
                        new ByteArrayInputStream(contents.get(contentKey).getBytes(StandardCharsets.UTF_8)),
                        contents.get(contentKey).length());
                namedPreparedStatement.setString(CONTENT_TYPE, contentKey);
                namedPreparedStatement.setInt(TENANT_ID, tenantId);
            }, null, false);
        } else {
            template.executeBatchInsert(INSERT_ORG_CUSTOM_CONTENT_SQL, namedPreparedStatement -> {
                String contentKey;
                while (iterator.hasNext()) {
                    contentKey = iterator.next();
                    namedPreparedStatement.setBinaryStream(CONTENT,
                            new ByteArrayInputStream(contents.get(contentKey).getBytes(StandardCharsets.UTF_8)),
                            contents.get(contentKey).length());
                    namedPreparedStatement.setString(CONTENT_TYPE, contentKey);
                    namedPreparedStatement.setInt(TENANT_ID, tenantId);
                    namedPreparedStatement.addBatch();
                }
            }, null);
        }
    }

    /**
     * Deletes custom layout content for a specific ORG.
     *
     * @param template The JDBC template to use for database operations.
     * @param tenantId Tenant ID.
     * @throws DataAccessException If an error occurs during content deletion.
     */
    private void deleteContent(NamedTemplate<Object> template, int tenantId) throws DataAccessException {

        template.executeUpdate(DELETE_ORG_CUSTOM_CONTENT_SQL, namedPreparedStatement -> {
            namedPreparedStatement.setInt(TENANT_ID, tenantId);
        });
    }
}
