/*
 * Copyright (c) 2016-2024, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.branding.preference.resolver.dao;

import org.wso2.carbon.database.utils.jdbc.NamedJdbcTemplate;
import org.wso2.carbon.database.utils.jdbc.NamedQueryFilter;
import org.wso2.carbon.database.utils.jdbc.exceptions.DataAccessException;
import org.wso2.carbon.identity.branding.preference.management.core.model.CustomContent;
import org.wso2.carbon.identity.core.util.JdbcUtils;

import java.util.HashMap;
import java.util.Map;

import static org.wso2.carbon.identity.branding.preference.resolver.dao.constants.DaoConstants.CustomContentTableColumns.*;
import static org.wso2.carbon.identity.branding.preference.resolver.dao.constants.SQLConstants.*;

/**
 * This class is to perform CRUD operations for Application vise Custom Content
 */

public class OrgCustomContentDAO {

    public void addOrgCustomContent(String customContent, String contentType, int tenantId) {
        NamedJdbcTemplate namedJdbcTemplate = JdbcUtils.getNewNamedJdbcTemplate();
        try {
            namedJdbcTemplate.executeInsert(INSERT_ORG_CUSTOM_CONTENT_SQL, (preparedStatement -> {
                preparedStatement.setString(CONTENT_TYPE, contentType);
                preparedStatement.setString(CONTENT, customContent);
                preparedStatement.setInt(TENANT_ID, tenantId);
            }), null, true);
        } catch (DataAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public CustomContent getOrgCustomContent(int tenantId) throws Exception {
        NamedQueryFilter namedQueryFilter = query -> query.setInt("TENANT_ID", tenantId);
        NamedJdbcTemplate template = JdbcUtils.getNewNamedJdbcTemplate();

        final String[] htmlContent = {""};
        final String[] cssContent = {""};
        final String[] jsContent = {""};

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
                namedQueryFilter
        );
        return new CustomContent(htmlContent[0], cssContent[0], jsContent[0]);
    }

    public CustomContent updateOrgCustomContent(String customContent, String contentType, int tenantId) {
        NamedJdbcTemplate template = JdbcUtils.getNewNamedJdbcTemplate();

        template.executeUpdate( UPDATE_ORG_CUSTOM_CONTENT_SQL, )
    }

}
