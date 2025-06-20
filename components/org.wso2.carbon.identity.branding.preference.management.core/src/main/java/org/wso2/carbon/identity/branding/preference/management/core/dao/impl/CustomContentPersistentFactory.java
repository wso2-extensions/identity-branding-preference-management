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

import org.wso2.carbon.identity.branding.preference.management.core.dao.CustomContentPersistentDAO;

/**
 * Factory class to get the instance of CustomContentPersistentDAO.
 */
public class CustomContentPersistentFactory {

    private static final CustomContentPersistentDAO PERSISTENT_MANAGER = new CustomContentPersistentDAOImpl();

    private CustomContentPersistentFactory() {

    }

    /**
     * Get the instance of CustomContentPersistentDAO.
     *
     * @return CustomContentPersistentDAO instance.
     */
    public static CustomContentPersistentDAO getCustomContentPersistentDAO() {

        return PERSISTENT_MANAGER;
    }
}
