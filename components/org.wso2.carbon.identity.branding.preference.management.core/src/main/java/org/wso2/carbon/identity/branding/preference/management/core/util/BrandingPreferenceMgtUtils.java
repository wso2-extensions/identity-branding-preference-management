/*
 * Copyright (c) 2022-2025, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.branding.preference.management.core.util;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.carbon.identity.branding.preference.management.core.BrandingPreferenceManager;
import org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants;
import org.wso2.carbon.identity.branding.preference.management.core.dao.CustomContentPersistentDAO;
import org.wso2.carbon.identity.branding.preference.management.core.dao.impl.CustomContentPersistentFactory;
import org.wso2.carbon.identity.branding.preference.management.core.exception.BrandingPreferenceMgtClientException;
import org.wso2.carbon.identity.branding.preference.management.core.exception.BrandingPreferenceMgtException;
import org.wso2.carbon.identity.branding.preference.management.core.exception.BrandingPreferenceMgtServerException;
import org.wso2.carbon.identity.branding.preference.management.core.model.BrandingPreference;
import org.wso2.carbon.identity.branding.preference.management.core.model.CustomLayoutContent;
import org.wso2.carbon.identity.core.ServiceURLBuilder;
import org.wso2.carbon.identity.core.URLBuilderException;
import org.wso2.carbon.identity.core.util.IdentityUtil;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ACTIVE_LAYOUT_KEY;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.APPLICATION_TYPE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.BRANDING_URLS;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.CONFIGS;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.CSS_CONTENT_KEY;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.CUSTOM_CONTENT_KEY;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.CUSTOM_LAYOUT;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.CustomContentTypes.CONTENT_TYPE_CSS;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.CustomContentTypes.CONTENT_TYPE_HTML;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.CustomContentTypes.CONTENT_TYPE_JS;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.DEFAULT_LOCALE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_INVALID_BRANDING_PREFERENCE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_MAXIMUM_CUSTOM_CONTENT_SIZE_EXCEEDED;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.HTML_CONTENT_KEY;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.IS_BRANDING_ENABLED;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.JS_CONTENT_KEY;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.LAYOUT_KEY;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.LOCAL_CODE_SEPARATOR;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ORGANIZATION_TYPE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.RECOVERY_PORTAL_URL;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.RESOURCE_NAME_SEPARATOR;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.SELF_SIGN_UP_URL;
import static org.wso2.carbon.identity.flow.mgt.Constants.FlowTypes.REGISTRATION;

/**
 * Util class for branding preference management.
 */
public class BrandingPreferenceMgtUtils {

    public static final String DEFAULT_REGISTRATION_PORTAL_URL = "/authenticationendpoint/register.do";
    public static final String DEFAULT_RECOVERY_PORTAL_URL = "/authenticationendpoint/recovery.do";
    private static final Log log = LogFactory.getLog(BrandingPreferenceMgtUtils.class);

    /**
     * Check whether the given string is a valid preference object or not.
     *
     * @param preference   Input String.
     * @param tenantDomain Tenant domain.
     * @throws BrandingPreferenceMgtException If the preference is not a valid JSON.
     */
    public static void isValidBrandingPreference(String preference, String tenantDomain)
            throws BrandingPreferenceMgtException {

        if (!isValidJSONString(preference)) {
            throw handleClientException(ERROR_CODE_INVALID_BRANDING_PREFERENCE, tenantDomain);
        }
        validateCustomLayoutContent(preference, tenantDomain);
    }

    /**
     * Validate the custom layout content in the given preference string.
     *
     * @param preference   Input String.
     * @param tenantDomain Tenant domain.
     * @throws BrandingPreferenceMgtException If the custom layout content is invalid.
     */
    private static void validateCustomLayoutContent(String preference, String tenantDomain)
            throws BrandingPreferenceMgtException {

        try {
            JSONObject preferenceJSON = new JSONObject(preference);
            if (preferenceJSON.has(LAYOUT_KEY)) {
                JSONObject layout = preferenceJSON.getJSONObject(LAYOUT_KEY);
                if (layout.has(ACTIVE_LAYOUT_KEY) &&
                        StringUtils.equals(layout.getString(ACTIVE_LAYOUT_KEY), CUSTOM_LAYOUT)) {
                    if (layout.has(CUSTOM_CONTENT_KEY)) {
                        JSONObject customContent = layout.getJSONObject(CUSTOM_CONTENT_KEY);
                        if (!customContent.has(HTML_CONTENT_KEY) ||
                                StringUtils.isBlank(customContent.getString(HTML_CONTENT_KEY))) {
                            throw handleClientException(
                                    BrandingPreferenceMgtConstants.ErrorMessages
                                            .ERROR_CODE_INVALID_CUSTOM_LAYOUT_CONTENT);
                        }
                        validateCustomLayoutContentSize(customContent.getString(HTML_CONTENT_KEY));
                        validateMandatoryComponentsInLayoutContent(customContent.getString(HTML_CONTENT_KEY));
                        if (customContent.has(CSS_CONTENT_KEY)) {
                            validateCustomLayoutContentSize(customContent.getString(CSS_CONTENT_KEY));
                        }
                        if (customContent.has(JS_CONTENT_KEY)) {
                            validateCustomLayoutContentSize(customContent.getString(JS_CONTENT_KEY));
                        }
                    }
                }
            }
        } catch (JSONException e) {
            throw handleClientException(ERROR_CODE_INVALID_BRANDING_PREFERENCE, tenantDomain);
        }
    }

    /**
     * Validate the custom layout content size.
     *
     * @param content Custom layout content.
     * @throws BrandingPreferenceMgtException If the custom layout content size exceeds the configured limit.
     */
    private static void validateCustomLayoutContentSize(String content) throws BrandingPreferenceMgtException {

        if (StringUtils.isBlank(content)) {
            return;
        }

        String maxFileSizeConfig =
                IdentityUtil.getProperty(BrandingPreferenceMgtConstants.CUSTOM_CONTENT_SIZE_LIMIT_CONFIG_KEY);
        int maxFileSize = BrandingPreferenceMgtConstants.CUSTOM_CONTENT_SIZE_LIMIT_DEFAULT;
        if (StringUtils.isNotBlank(maxFileSizeConfig)) {
            try {
                maxFileSize = Integer.parseInt(maxFileSizeConfig);
            } catch (NumberFormatException e) {
                log.warn("Invalid custom content size limit configuration found. Using default value: "
                        + BrandingPreferenceMgtConstants.CUSTOM_CONTENT_SIZE_LIMIT_DEFAULT, e);
            }
        }
        byte[] utf8Bytes = content.getBytes(StandardCharsets.UTF_8);
        if (utf8Bytes.length <= maxFileSize) {
            return;
        }
        throw handleClientException(ERROR_CODE_MAXIMUM_CUSTOM_CONTENT_SIZE_EXCEEDED, String.valueOf(maxFileSize));
    }

    /**
     * Validate the mandatory components in the custom layout content.
     *
     * @param html HTML content of the custom layout.
     * @throws BrandingPreferenceMgtException If the mandatory components are not found in the layout content.
     */
    private static void validateMandatoryComponentsInLayoutContent(String html) throws BrandingPreferenceMgtException {

        // Validate for the MainSection component.
        Pattern mainSectionPattern = Pattern.compile("\\{\\{\\{[ \\t]*MainSection[ \\t]*\\}\\}\\}");
        if (!mainSectionPattern.matcher(html).find()) {
            throw handleClientException(
                    BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_MANDATORY_COMPONENT_NOT_FOUND,
                    "{{{" + BrandingPreferenceMgtConstants.CustomLayoutComponents.MAIN_SECTION.getComponentName() +
                            "}}}");
        }
    }

    /**
     * Check whether the given string is a valid JSON or not.
     *
     * @param stringJSON Input String.
     * @return True if the input string is a valid JSON.
     */
    public static boolean isValidJSONString(String stringJSON) {

        if (StringUtils.isBlank(stringJSON)) {
            return false;
        }
        try {
            JSONObject objectJSON = new JSONObject(stringJSON);
            if (objectJSON.length() == 0) {
                return false;
            }
        } catch (JSONException exception) {
            // If the preference string is not in the valid json format JSONException will be thrown.
            if (log.isDebugEnabled()) {
                log.debug("Invalid json string. Error occurred while validating preference string", exception);
            }
            return false;
        }
        return true;
    }

    /**
     * This method can be used to generate a BrandingPreferenceMgtClientException from
     * BrandingPreferenceMgtConstants.ErrorMessages object when no exception is thrown.
     *
     * @param error BrandingPreferenceMgtConstants.ErrorMessages.
     * @param data  data to replace if message needs to be replaced.
     * @return BrandingPreferenceMgtClientException.
     */
    public static BrandingPreferenceMgtClientException handleClientException(
            BrandingPreferenceMgtConstants.ErrorMessages error, String... data) {

        String message = populateMessageWithData(error, data);
        return new BrandingPreferenceMgtClientException(message, error.getCode());
    }

    public static BrandingPreferenceMgtClientException handleClientException(
            BrandingPreferenceMgtConstants.ErrorMessages error, String data, Throwable e) {

        String message = populateMessageWithData(error, data);
        return new BrandingPreferenceMgtClientException(message, error.getCode(), e);
    }

    /**
     * This method can be used to generate a BrandingPreferenceMgtServerException from
     * BrandingPreferenceMgtConstants.ErrorMessages object when no exception is thrown.
     *
     * @param error SecretConstants.ErrorMessages.
     * @param data  data to replace if message needs to be replaced.
     * @return BrandingPreferenceMgtClientException.
     */
    public static BrandingPreferenceMgtServerException handleServerException(
            BrandingPreferenceMgtConstants.ErrorMessages error, String... data) {

        String message = populateMessageWithData(error, data);
        return new BrandingPreferenceMgtServerException(message, error.getCode());
    }

    public static BrandingPreferenceMgtServerException handleServerException(
            BrandingPreferenceMgtConstants.ErrorMessages error, String data, Throwable e) {

        String message = populateMessageWithData(error, data);
        return new BrandingPreferenceMgtServerException(message, error.getCode(), e);
    }

    public static BrandingPreferenceMgtServerException handleServerException(
            BrandingPreferenceMgtConstants.ErrorMessages error, Throwable e) {

        String message = populateMessageWithData(error);
        return new BrandingPreferenceMgtServerException(message, error.getCode(), e);
    }

    /**
     * Replace '_' with '-' in the locale code for support both the locale code formats like en-US & en_US.
     *
     * @param locale Locale code.
     * @return Formatted locale code.
     */
    public static String getFormattedLocale(String locale) {

        String formattedLocale = locale;
        if (StringUtils.isNotBlank(locale)) {
            formattedLocale = locale.replace(RESOURCE_NAME_SEPARATOR, LOCAL_CODE_SEPARATOR);
        }
        return formattedLocale;
    }

    /**
     * Check whether the given branding preference is published or not.
     *
     * @param brandingPreference Branding preference that needs to be checked.
     * @return True if the branding preference is published.
     */
    public static boolean isBrandingPublished(BrandingPreference brandingPreference) {

        JSONObject preferences = new JSONObject((LinkedHashMap) brandingPreference.getPreference());

        // If configs.isBrandingEnabled is not found in preferences, it is assumed that branding is enabled by default.
        return !preferences.has(CONFIGS) ||
                preferences.getJSONObject(CONFIGS).optBoolean(IS_BRANDING_ENABLED, true);
    }

    private static String populateMessageWithData(BrandingPreferenceMgtConstants.ErrorMessages error, String... data) {

        String message;
        if (data != null && data.length != 0) {
            message = String.format(error.getMessage(), data);
        } else {
            message = error.getMessage();
        }
        return message;
    }

    private static String populateMessageWithData(BrandingPreferenceMgtConstants.ErrorMessages error) {

        return error.getMessage();
    }

    /**
     * Resolves the content types from the CustomLayoutContent object.
     *
     * @param content The CustomLayoutContent object containing HTML, CSS, and JS content.
     * @return A map containing content types as keys and their corresponding content as values.
     */
    public static Map<String, String> resolveContentTypes(CustomLayoutContent content) {

        Map<String, String> contents = new HashMap<>();

        if (content == null) {
            return contents;
        }

        contents.put(CONTENT_TYPE_HTML, content.getHtml());
        if (StringUtils.isNotBlank(content.getCss())) {
            contents.put(CONTENT_TYPE_CSS, content.getCss());
        }
        if (StringUtils.isNotBlank(content.getJs())) {
            contents.put(CONTENT_TYPE_JS, content.getJs());
        }
        return contents;
    }

    /**
     * Extract the custom layout content from the preferences.
     *
     * @param preferences The Object containing the branding preferences.
     * @return CustomLayoutContent object containing the custom layout content.
     */
    public static CustomLayoutContent extractCustomLayoutContent(Object preferences) {

        if (preferences instanceof Map) {
            Map<?, ?> preferenceMap = (Map<?, ?>) preferences;
            if (preferenceMap.get(LAYOUT_KEY) instanceof Map) {
                Map<?, ?> layout = (Map<?, ?>) preferenceMap.get(LAYOUT_KEY);
                if (layout.get(ACTIVE_LAYOUT_KEY) instanceof String) {
                    String activeLayout = (String) layout.get(ACTIVE_LAYOUT_KEY);
                    if (StringUtils.equals(activeLayout, CUSTOM_LAYOUT) &&
                            layout.get(CUSTOM_CONTENT_KEY) instanceof Map) {
                        Map<?, ?> customContent = (Map<?, ?>) layout.get(CUSTOM_CONTENT_KEY);
                        CustomLayoutContent.CustomLayoutContentBuilder customLayoutContentBuilder =
                                new CustomLayoutContent.CustomLayoutContentBuilder();
                        customLayoutContentBuilder.setHtml((String) customContent.get(HTML_CONTENT_KEY));
                        customLayoutContentBuilder.setCss((String) customContent.get(CSS_CONTENT_KEY));
                        customLayoutContentBuilder.setJs((String) customContent.get(JS_CONTENT_KEY));
                        return customLayoutContentBuilder.build();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Adds custom layout content to the preferences if the active layout is `custom`.
     *
     * @param preferences  The Object containing the branding preferences.
     * @param appId        The application ID.
     * @param tenantDomain The tenant domain.
     * @throws BrandingPreferenceMgtException If an error occurs while adding custom layout content.
     */
    public static void addCustomLayoutContentToPreferences(Object preferences, String appId, String tenantDomain)
            throws BrandingPreferenceMgtException {

        CustomContentPersistentDAO customContentDAO = CustomContentPersistentFactory.getCustomContentPersistentDAO();
        if (preferences instanceof Map) {
            Map<String, Object> preferenceMap = (Map<String, Object>) preferences;
            if (preferenceMap.get(LAYOUT_KEY) instanceof Map) {
                Map<String, Object> layout = (Map<String, Object>) preferenceMap.get(LAYOUT_KEY);
                if (layout.get(ACTIVE_LAYOUT_KEY) instanceof String &&
                        StringUtils.equals((String) layout.get(ACTIVE_LAYOUT_KEY), CUSTOM_LAYOUT)) {
                    CustomLayoutContent customLayoutContent =
                            customContentDAO.getCustomContent(appId, tenantDomain);
                    if (customLayoutContent != null) {
                        Map<String, String> customContent = new LinkedHashMap<>();
                        customContent.put(HTML_CONTENT_KEY, customLayoutContent.getHtml());
                        if (StringUtils.isNotBlank(customLayoutContent.getCss())) {
                            customContent.put(CSS_CONTENT_KEY, customLayoutContent.getCss());
                        }
                        if (StringUtils.isNotBlank(customLayoutContent.getJs())) {
                            customContent.put(JS_CONTENT_KEY, customLayoutContent.getJs());
                        }
                        layout.put(CUSTOM_CONTENT_KEY, customContent);
                    }
                }
            }
        }
    }

    /**
     * Handle exceptions related to branding preference management.
     *
     * @param t Throwable instance.
     * @throws BrandingPreferenceMgtException Throws appropriate BrandingPreferenceMgtException based on the type
     *                                        of the Throwable.
     */
    public static void handleBrandingMgtException(Throwable t) throws BrandingPreferenceMgtException {

        if (t == null) {
            return;
        }

        if (t instanceof BrandingPreferenceMgtClientException) {
            throw (BrandingPreferenceMgtClientException) t;
        } else if (t instanceof BrandingPreferenceMgtServerException) {
            throw (BrandingPreferenceMgtServerException) t;
        } else if (t instanceof BrandingPreferenceMgtException) {
            throw (BrandingPreferenceMgtException) t;
        }
    }

    private static String buildDefaultPortalUrl(String flowType) throws URLBuilderException {

        ServiceURLBuilder builder = ServiceURLBuilder.create();
        String path = REGISTRATION.getType().equalsIgnoreCase(flowType) ? DEFAULT_REGISTRATION_PORTAL_URL
                : DEFAULT_RECOVERY_PORTAL_URL;
        return builder.addPath(path).build().getAbsolutePublicURL();
    }

    /**
     * Builds the configured portal URL based on the application id, tenant domain, and flow type.
     *
     * @param applicationId             Application ID for which the portal URL is to be built.
     * @param tenantDomain              Tenant domain of the application or organization.
     * @param brandingPreferenceManager BrandingPreferenceManager instance to retrieve branding preferences.
     * @param flowType                  Flow type to determine the default URL if the configured URL is not found.
     * @return Configured portal URL or default URL if not found.
     * @throws URLBuilderException            If there is an error building the URL.
     * @throws BrandingPreferenceMgtException If there is an error retrieving the branding preference.
     */
    public static String buildConfiguredPortalURL(String applicationId, String tenantDomain,
                                                  BrandingPreferenceManager brandingPreferenceManager,
                                                  String flowType)
            throws URLBuilderException, BrandingPreferenceMgtException {

        BrandingPreference preference;
        String configuredURL = StringUtils.EMPTY;

        String type = StringUtils.isBlank(applicationId) ? ORGANIZATION_TYPE : APPLICATION_TYPE;
        String name = StringUtils.isBlank(applicationId) ? tenantDomain : applicationId;
        String requiredURL = REGISTRATION.getType().equalsIgnoreCase(flowType) ? SELF_SIGN_UP_URL :
                RECOVERY_PORTAL_URL;

        try {
            if (brandingPreferenceManager != null) {
                preference = brandingPreferenceManager.getBrandingPreference(type, name, DEFAULT_LOCALE);
                if (preference != null) {
                    Map<String, Object> prefMap = (Map<String, Object>) preference.getPreference();
                    Map<String, String> urlMap = (Map<String, String>) prefMap.get(BRANDING_URLS);
                    configuredURL = (urlMap != null) ? urlMap.get(requiredURL) : null;
                }
            }
        } catch (BrandingPreferenceMgtClientException e) {
            if (log.isDebugEnabled()) {
                log.debug("Failed to build configured portal URL for tenant: " + tenantDomain, e);
            }
        }

        if (StringUtils.isBlank(configuredURL)) {
            configuredURL = buildDefaultPortalUrl(flowType);
            logMissingPortalUrl(tenantDomain, configuredURL);
        }
        return configuredURL;
    }

    private static void logMissingPortalUrl(String tenantDomain, String defaultURL) {

        log.debug("Portal URL is not configured for tenant: " + tenantDomain + ". Using default URL: "
                + defaultURL);
    }
}
