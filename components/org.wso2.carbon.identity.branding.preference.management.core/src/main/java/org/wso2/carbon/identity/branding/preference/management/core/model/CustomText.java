/*
 * Copyright (c) 2023, WSO2 Inc. (http://www.wso2.com).
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
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

package org.wso2.carbon.identity.branding.preference.management.core.model;

/**
 * A model class representing a custom text preference.
 */
public class CustomText {

    private String type;
    private String name;
    private String screen;
    private String locale;
    private Object preference;

    public String getType() {

        return type;
    }

    public void setType(String type) {

        this.type = type;
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {

        this.name = name;
    }

    public String getScreen() {

        return screen;
    }

    public void setScreen(String screen) {

        this.screen = screen;
    }

    public String getLocale() {

        return locale;
    }

    public void setLocale(String locale) {

        this.locale = locale;
    }

    public Object getPreference() {

        return preference;
    }

    public void setPreference(Object preference) {

        this.preference = preference;
    }
}
