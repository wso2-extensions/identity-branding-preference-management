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

import com.google.gson.Gson;
import org.wso2.carbon.database.utils.jdbc.NamedJdbcTemplate;
import org.wso2.carbon.database.utils.jdbc.exceptions.DataAccessException;
import org.wso2.carbon.identity.branding.preference.management.core.model.CustomContent;
import org.wso2.carbon.identity.core.util.JdbcUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;

import static org.wso2.carbon.identity.branding.preference.management.core.dao.constants.DaoConstants.CustomContentTableColumns.*;
import static org.wso2.carbon.identity.branding.preference.management.core.dao.constants.SQLConstants.*;

/**
 * This class is to perform CRUD operations for Application vise Custom Content
 */

public class AppCustomContentDAO {

    public static CustomContent addAppCustomContent(String customContent, String contentType, int appId, int tenantId) {
        CustomContent result = null;
        NamedJdbcTemplate template = JdbcUtils.getNewNamedJdbcTemplate();

//        final String[] htmlContent = {"<div class=\"login-box\">\n  <h1>Welcome to {{ organization }}</h1>\n  <form action=\"/login\" method=\"POST\">\n    <input type=\"text\" placeholder=\"Username\" name=\"username\" required />\n    <input type=\"password\" placeholder=\"Password\" name=\"password\" required />\n    <button type=\"submit\">Login</button>\n  </form>\n</div>"};
//        final String[] cssContent = {"body {\\n  background-color: #f7f7f7;\\n  font-family: Arial, sans-serif;\\n}\\n\\n.login-box {\\n  max-width: 400px;\\n  margin: 100px auto;\\n  padding: 2rem;\\n  background: white;\\n  border-radius: 8px;\\n  box-shadow: 0 2px 10px rgba(0,0,0,0.1);\\n}\\n\\nbutton {\\n  background-color: #007BFF;\\n  color: white;\\n  border: none;\\n  padding: 10px 20px;\\n  cursor: pointer;\\n  border-radius: 4px;\\n}\\n\\nbutton:hover {\\n  background-color: #0056b3;\\n}\",\n"};
//        final String[] jsContent = {"document.addEventListener(\\\"DOMContentLoaded\\\", function () {\\n  const form = document.querySelector(\\\"form\\\");\\n  form.addEventListener(\\\"submit\\\", function (e) {\\n    const username = form.username.value.trim();\\n    if (!username) {\\n      e.preventDefault();\\n      alert(\\\"Username is required\\\");\\n    }\\n  });\\n});\",\n"};

        String contentJson = new Gson().toJson(customContent);
        byte[] contentByteArray = contentJson.getBytes(StandardCharsets.UTF_8);
        int contentLength = contentByteArray.length;

        try (InputStream contentStream = new ByteArrayInputStream(contentByteArray)){
            template.executeInsert(INSERT_APP_CUSTOM_CONTENT_SQL,
                    (namedPreparedStatement -> {
                        namedPreparedStatement.setBinaryStream(CONTENT,contentStream,contentLength);
                        namedPreparedStatement.setString(CONTENT_TYPE,contentType);
                        namedPreparedStatement.setInt(APP_ID, appId);
                        namedPreparedStatement.setInt(TENANT_ID,tenantId);
                    }),customContent,false

            );
        }catch (DataAccessException e) {
            String error =
                    String.format("Error while adding custom content to application in %s tenant.",
                            tenantId);
        } catch (IOException e) {
        }
        return result;
    }
    /**
     * @param appId
     * @return CustomContent
     */
    public static CustomContent getAppCustomContent(int appId){
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
                        namedPreparedStatement.setInt(APP_ID, appId);
                    }
            );
            result = new CustomContent(htmlContent[0], cssContent[0], jsContent[0]);
        } catch (Exception e) {
            System.err.println("Warning: Failed to fetch custom content for tenantId " + appId + ". Using empty content.");
            e.printStackTrace();
        }
        return result;
    }

    public static void deleteAppCustomContent(int appId) {
        NamedJdbcTemplate namedJdbcTemplate = JdbcUtils.getNewNamedJdbcTemplate();
        try{
            namedJdbcTemplate.executeUpdate(DELETE_APP_CUSTOM_CONTENT_SQL,
                    namedPreparedStatement -> {
                        namedPreparedStatement.setInt(APP_ID, appId);
                    });
        } catch (DataAccessException e){
            String error =
                    String.format("Error while deleting the custom content in %s app.",
                            appId);
        }
    }

}
