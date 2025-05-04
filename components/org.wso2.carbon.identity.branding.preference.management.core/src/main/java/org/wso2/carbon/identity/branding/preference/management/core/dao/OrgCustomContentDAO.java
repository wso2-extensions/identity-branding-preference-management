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

package org.wso2.carbon.identity.branding.preference.management.core.dao;

import org.wso2.carbon.database.utils.jdbc.NamedJdbcTemplate;
//import org.wso2.carbon.database.utils.jdbc.exceptions.DataAccessException;
import org.wso2.carbon.database.utils.jdbc.exceptions.DataAccessException;
import org.wso2.carbon.identity.branding.preference.management.core.model.CustomContent;
import org.wso2.carbon.identity.core.util.JdbcUtils;

import static org.wso2.carbon.identity.branding.preference.management.core.dao.constants.DaoConstants.CustomContentTableColumns.TENANT_ID;
import static org.wso2.carbon.identity.branding.preference.management.core.dao.constants.SQLConstants.GET_ORG_CUSTOM_CONTENT_SQL;
import static org.wso2.carbon.identity.branding.preference.management.core.dao.constants.SQLConstants.DELETE_ORG_CUSTOM_CONTENT_SQL;

/**
 * This class is to perform CRUD operations for Application vise Custom Content
 */

public class OrgCustomContentDAO {

    public void addOrgCustomContent(String customContent, String contentType, int tenantId) {
//        NamedJdbcTemplate namedJdbcTemplate = JdbcUtils.getNewNamedJdbcTemplate();
//        try {
//            namedJdbcTemplate.executeInsert(INSERT_ORG_CUSTOM_CONTENT_SQL, (preparedStatement -> {
//                preparedStatement.setString(CONTENT_TYPE, contentType);
//                preparedStatement.setString(CONTENT, customContent);
//                preparedStatement.setInt(TENANT_ID, tenantId);
//            }), null, true);
//        } catch (DataAccessException e) {
//            throw new RuntimeException(e);
//        }
    }

    /**
     * @param tenantId
     * @return CustomContent
     */
    public static CustomContent getOrgCustomContent(int tenantId){
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
        } catch (Exception e) {
            // Log instead of throwing to avoid breaking the API class at startup
            System.err.println("Warning: Failed to fetch custom content for tenantId " + tenantId + ". Using empty content.");
            e.printStackTrace();
        }
        return result;
    }

    public static void deleteOrgCustomContent(int tenantId) {
        NamedJdbcTemplate namedJdbcTemplate = JdbcUtils.getNewNamedJdbcTemplate();
        try{
            namedJdbcTemplate.executeUpdate(DELETE_ORG_CUSTOM_CONTENT_SQL,
                    namedPreparedStatement -> {
                        namedPreparedStatement.setInt(TENANT_ID, tenantId);
                    });
        } catch (DataAccessException e){
            String error =
                    String.format("Error while deleting the custom content in %s tenant.",
                            tenantId);
        }
    }

//    public CustomContent updateOrgCustomContent(String customContent, String contentType, int tenantId) {
//        NamedJdbcTemplate template = JdbcUtils.getNewNamedJdbcTemplate();
//
//        template.executeUpdate( UPDATE_ORG_CUSTOM_CONTENT_SQL, )
//    }

}
