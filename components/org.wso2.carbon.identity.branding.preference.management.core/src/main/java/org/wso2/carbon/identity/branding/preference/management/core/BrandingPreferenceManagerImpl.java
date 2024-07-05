/*
 * Copyright (c) 2022-2024, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.branding.preference.management.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.branding.preference.management.core.exception.BrandingPreferenceMgtClientException;
import org.wso2.carbon.identity.branding.preference.management.core.exception.BrandingPreferenceMgtException;
import org.wso2.carbon.identity.branding.preference.management.core.internal.BrandingPreferenceManagerComponentDataHolder;
import org.wso2.carbon.identity.branding.preference.management.core.model.BrandingPreference;
import org.wso2.carbon.identity.branding.preference.management.core.model.CustomText;
import org.wso2.carbon.identity.branding.preference.management.core.util.BrandingPreferenceMgtUtils;
import org.wso2.carbon.identity.configuration.mgt.core.ConfigurationManager;
import org.wso2.carbon.identity.configuration.mgt.core.exception.ConfigurationManagementException;
import org.wso2.carbon.identity.configuration.mgt.core.model.Resource;
import org.wso2.carbon.identity.configuration.mgt.core.model.ResourceFile;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.services.IdentityEventService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.APPLICATION_BRANDING_RESOURCE_TYPE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.APPLICATION_TYPE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.BRANDING_PREFERENCE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.BRANDING_RESOURCE_TYPE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.BRANDING_URLS;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.CUSTOM_TEXT_RESOURCE_TYPE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_APPLICATION_NOT_FOUND;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_BRANDING_PREFERENCE_ALREADY_EXISTS;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_BRANDING_PREFERENCE_NOT_CONFIGURED;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_CUSTOM_TEXT_ALREADY_EXISTS;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_CUSTOM_TEXT_PREFERENCE_NOT_EXISTS;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_ADDING_BRANDING_PREFERENCE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_ADDING_CUSTOM_TEXT_PREFERENCE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_BUILDING_BRANDING_PREFERENCE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_BUILDING_CUSTOM_TEXT_PREFERENCE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_BULK_DELETING_CUSTOM_TEXT_PREFERENCES;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_CHECKING_BRANDING_PREFERENCE_EXISTS;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_DELETING_BRANDING_PREFERENCE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_DELETING_CUSTOM_TEXT_PREFERENCE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_GETTING_BRANDING_PREFERENCE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_GETTING_CUSTOM_TEXT_PREFERENCE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_UPDATING_BRANDING_PREFERENCE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_UPDATING_CUSTOM_TEXT_PREFERENCE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_VALIDATING_BRANDING_PREFERENCE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_INVALID_BRANDING_PREFERENCE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_INVALID_CUSTOM_TEXT_PREFERENCE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_NOT_ALLOWED_BRANDING_PREFERENCE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.JAVASCRIPT;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.NEW_BRANDING_PREFERENCE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.OLD_BRANDING_PREFERENCE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.PRE_ADD_BRANDING_PREFERENCE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.PRE_UPDATE_BRANDING_PREFERENCE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.RESOURCES_NOT_EXISTS_ERROR_CODE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.RESOURCE_ALREADY_EXISTS_ERROR_CODE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.RESOURCE_NAME_SEPARATOR;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.RESOURCE_NOT_EXISTS_ERROR_CODE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.TENANT_DOMAIN;
import static org.wso2.carbon.identity.branding.preference.management.core.util.BrandingPreferenceMgtUtils.getFormattedLocale;
import static org.wso2.carbon.identity.branding.preference.management.core.util.BrandingPreferenceMgtUtils.handleClientException;
import static org.wso2.carbon.identity.branding.preference.management.core.util.BrandingPreferenceMgtUtils.handleServerException;

/**
 * Branding Preference Management service implementation.
 */
public class BrandingPreferenceManagerImpl implements BrandingPreferenceManager {

    private static final Log LOG = LogFactory.getLog(BrandingPreferenceManagerImpl.class);

    @Override
    public BrandingPreference addBrandingPreference(BrandingPreference brandingPreference)
            throws BrandingPreferenceMgtException {

        String resourceName = getResourceName
                (brandingPreference.getType(), brandingPreference.getName(), brandingPreference.getLocale());
        String resourceType = getResourceType(brandingPreference.getType());
        String tenantDomain = getTenantDomain();
        // Check whether a branding resource already exists with the same name in the particular tenant to be added.
        if (isResourceExists(resourceType, resourceName)) {
            throw handleClientException(ERROR_CODE_BRANDING_PREFERENCE_ALREADY_EXISTS, tenantDomain);
        }

        if (APPLICATION_TYPE.equals(brandingPreference.getType())) {
            // Check whether an application exists with the given name.
            if (!isApplicationExists(brandingPreference.getName(), tenantDomain)) {
                throw handleClientException
                        (ERROR_CODE_APPLICATION_NOT_FOUND, brandingPreference.getName(), tenantDomain);
            }
        }

        String preferencesJSON = generatePreferencesJSONFromPreference(brandingPreference.getPreference());
        if (!BrandingPreferenceMgtUtils.isValidJSONString(preferencesJSON)) {
            throw handleClientException(ERROR_CODE_INVALID_BRANDING_PREFERENCE, tenantDomain);
        }
        validatePreferenceUrls(brandingPreference);

        triggerPreAddBrandingPreferenceEvents(brandingPreference, tenantDomain);
        preferencesJSON = generatePreferencesJSONFromPreference(brandingPreference.getPreference());

        try (InputStream inputStream = new ByteArrayInputStream(preferencesJSON.getBytes(StandardCharsets.UTF_8))) {
            Resource brandingPreferenceResource = buildResource(resourceName, inputStream);
            getConfigurationManager().addResource(resourceType, brandingPreferenceResource);
            if (BrandingPreferenceManagerComponentDataHolder.getInstance().getUiBrandingPreferenceResolver() != null) {
                BrandingPreferenceManagerComponentDataHolder.getInstance().getUiBrandingPreferenceResolver()
                        .clearBrandingResolverCacheHierarchy(brandingPreference.getType(), brandingPreference.getName(),
                                tenantDomain);
            }
        } catch (ConfigurationManagementException e) {
            if (RESOURCE_ALREADY_EXISTS_ERROR_CODE.equals(e.getErrorCode())) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Branding preferences are already exists for tenant: " + tenantDomain, e);
                }
                throw handleClientException(ERROR_CODE_BRANDING_PREFERENCE_ALREADY_EXISTS, tenantDomain);
            }
            throw handleServerException(ERROR_CODE_ERROR_ADDING_BRANDING_PREFERENCE, tenantDomain, e);
        } catch (IOException e) {
            throw handleServerException(ERROR_CODE_ERROR_ADDING_BRANDING_PREFERENCE, tenantDomain, e);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Branding preference for tenant: " + tenantDomain + " added successfully");
        }
        return brandingPreference;
    }

    @Override
    public BrandingPreference getBrandingPreference(String type, String name, String locale)
            throws BrandingPreferenceMgtException {

        String resourceName = getResourceName(type, name, locale);
        String resourceType = getResourceType(type);
        String tenantDomain = getTenantDomain();
        try {
            // Return default branding preference.
            List<ResourceFile> resourceFiles = getConfigurationManager().getFiles(resourceType, resourceName);
            if (resourceFiles.isEmpty()) {
                throw handleClientException(ERROR_CODE_BRANDING_PREFERENCE_NOT_CONFIGURED, type, name, tenantDomain);
            }
            if (StringUtils.isBlank(resourceFiles.get(0).getId())) {
                throw handleClientException(ERROR_CODE_BRANDING_PREFERENCE_NOT_CONFIGURED, type, name, tenantDomain);
            }

            InputStream inputStream = getConfigurationManager().getFileById
                    (resourceType, resourceName, resourceFiles.get(0).getId());
            if (inputStream == null) {
                throw handleClientException(ERROR_CODE_BRANDING_PREFERENCE_NOT_CONFIGURED, type, name, tenantDomain);
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Branding preference for tenant: " + tenantDomain + " is retrieved successfully.");
            }
            return buildBrandingPreferenceFromResource(inputStream, type, name, locale);
        } catch (ConfigurationManagementException e) {
            if (RESOURCE_NOT_EXISTS_ERROR_CODE.equals(e.getErrorCode())) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Can not find a branding preference configurations for tenant: " + tenantDomain, e);
                }
                throw handleClientException(ERROR_CODE_BRANDING_PREFERENCE_NOT_CONFIGURED, type, name, tenantDomain);
            }
            throw handleServerException(ERROR_CODE_ERROR_GETTING_BRANDING_PREFERENCE, tenantDomain, e);
        } catch (IOException e) {
            throw handleServerException(ERROR_CODE_ERROR_BUILDING_BRANDING_PREFERENCE, tenantDomain);
        }
    }

    @Override
    public BrandingPreference resolveBrandingPreference(String type, String name, String locale)
            throws BrandingPreferenceMgtException {

        return BrandingPreferenceManagerComponentDataHolder.getInstance().getUiBrandingPreferenceResolver()
                .resolveBranding(type, name, locale);
    }

    /**
     * @deprecated Use {@link #resolveBrandingPreference(String, String, String)} instead.
     */
    @Override
    @Deprecated
    public BrandingPreference resolveApplicationBrandingPreference(String identifier, String locale)
            throws BrandingPreferenceMgtException {

        return resolveBrandingPreference(APPLICATION_TYPE, identifier, locale);
    }

    @Override
    public BrandingPreference replaceBrandingPreference(BrandingPreference brandingPreference)
            throws BrandingPreferenceMgtException {

        String resourceName = getResourceName
                (brandingPreference.getType(), brandingPreference.getName(), brandingPreference.getLocale());
        String resourceType = getResourceType(brandingPreference.getType());
        String tenantDomain = getTenantDomain();
        // Check whether the branding resource exists in the particular tenant.
        if (!isResourceExists(resourceType, resourceName)) {
            throw handleClientException(ERROR_CODE_BRANDING_PREFERENCE_NOT_CONFIGURED, brandingPreference.getType(),
                    brandingPreference.getName(), tenantDomain);
        }

        String preferencesJSON = generatePreferencesJSONFromPreference(brandingPreference.getPreference());
        if (!BrandingPreferenceMgtUtils.isValidJSONString(preferencesJSON)) {
            throw handleClientException(ERROR_CODE_INVALID_BRANDING_PREFERENCE, tenantDomain);
        }
        validatePreferenceUrls(brandingPreference);
        BrandingPreference oldBrandingPreference = getBrandingPreference(brandingPreference.getType(),
                brandingPreference.getName(), brandingPreference.getLocale());

        triggerPreUpdateBrandingPreferenceEvents(oldBrandingPreference, brandingPreference, tenantDomain);
        preferencesJSON = generatePreferencesJSONFromPreference(brandingPreference.getPreference());

        try (InputStream inputStream = new ByteArrayInputStream(preferencesJSON.getBytes(StandardCharsets.UTF_8))) {
            Resource brandingPreferenceResource = buildResource(resourceName, inputStream);
            getConfigurationManager().replaceResource(resourceType, brandingPreferenceResource);
        } catch (ConfigurationManagementException | IOException e) {
            throw handleServerException(ERROR_CODE_ERROR_UPDATING_BRANDING_PREFERENCE, tenantDomain, e);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Branding preference for tenant: " + tenantDomain + " replaced successfully.");
        }
        return brandingPreference;
    }

    @Override
    public void deleteBrandingPreference(String type, String name, String locale)
            throws BrandingPreferenceMgtException {

        String resourceName = getResourceName(type, name, locale);
        String resourceType = getResourceType(type);
        String tenantDomain = getTenantDomain();
        // Check whether the branding resource exists in the particular tenant.
        if (!isResourceExists(resourceType, resourceName)) {
            throw handleClientException(ERROR_CODE_BRANDING_PREFERENCE_NOT_CONFIGURED, type, name, tenantDomain);
        }

        try {
            getConfigurationManager().deleteResource(resourceType, resourceName);
        } catch (ConfigurationManagementException e) {
            throw handleServerException(ERROR_CODE_ERROR_DELETING_BRANDING_PREFERENCE, tenantDomain);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Branding preference for tenant: " + tenantDomain + " replaced successfully.");
        }
    }

    @Override
    public CustomText addCustomText(CustomText customText) throws BrandingPreferenceMgtException {

        String resourceName = getResourceNameForCustomText(customText.getScreen(), customText.getLocale());
        String tenantDomain = getTenantDomain();
        // Check whether a custom text resource already exists with the same name in the particular tenant to be added.
        if (isResourceExists(CUSTOM_TEXT_RESOURCE_TYPE, resourceName)) {
            throw handleClientException(ERROR_CODE_CUSTOM_TEXT_ALREADY_EXISTS, tenantDomain);
        }
        String preferencesJSON = generatePreferencesJSONFromPreference(customText.getPreference());
        if (!BrandingPreferenceMgtUtils.isValidJSONString(preferencesJSON)) {
            throw handleClientException(ERROR_CODE_INVALID_CUSTOM_TEXT_PREFERENCE, tenantDomain);
        }

        try (InputStream inputStream = new ByteArrayInputStream(preferencesJSON.getBytes(StandardCharsets.UTF_8))) {
            Resource customTextPreferenceResource = buildResource(resourceName, inputStream);
            getConfigurationManager().addResource(CUSTOM_TEXT_RESOURCE_TYPE, customTextPreferenceResource);
            if (BrandingPreferenceManagerComponentDataHolder.getInstance().getUiBrandingPreferenceResolver() != null) {
                BrandingPreferenceManagerComponentDataHolder.getInstance().getUiBrandingPreferenceResolver()
                        .clearCustomTextResolverCacheHierarchy
                                (tenantDomain, customText.getScreen(), customText.getLocale());
            }
        } catch (ConfigurationManagementException e) {
            if (RESOURCE_ALREADY_EXISTS_ERROR_CODE.equals(e.getErrorCode())) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Custom Text preferences are already exists for tenant: " + tenantDomain, e);
                }
                throw handleClientException(ERROR_CODE_CUSTOM_TEXT_ALREADY_EXISTS, tenantDomain);
            }
            throw handleServerException(ERROR_CODE_ERROR_ADDING_CUSTOM_TEXT_PREFERENCE, tenantDomain, e);
        } catch (IOException e) {
            throw handleServerException(ERROR_CODE_ERROR_ADDING_CUSTOM_TEXT_PREFERENCE, tenantDomain, e);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Custom Text preference for tenant: " + tenantDomain + " added successfully");
        }
        return customText;
    }

    @Override
    public CustomText getCustomText(String type, String name, String screen, String locale)
            throws BrandingPreferenceMgtException {

        String resourceName = getResourceNameForCustomText(screen, locale);
        String tenantDomain = getTenantDomain();
        try {
            List<ResourceFile> resourceFiles =
                    getConfigurationManager().getFiles(CUSTOM_TEXT_RESOURCE_TYPE, resourceName);
            if (resourceFiles.isEmpty()) {
                throw handleClientException(ERROR_CODE_CUSTOM_TEXT_PREFERENCE_NOT_EXISTS, tenantDomain);
            }
            if (StringUtils.isBlank(resourceFiles.get(0).getId())) {
                throw handleClientException(ERROR_CODE_CUSTOM_TEXT_PREFERENCE_NOT_EXISTS, tenantDomain);
            }

            InputStream inputStream = getConfigurationManager().getFileById
                    (CUSTOM_TEXT_RESOURCE_TYPE, resourceName, resourceFiles.get(0).getId());
            if (inputStream == null) {
                throw handleClientException(ERROR_CODE_CUSTOM_TEXT_PREFERENCE_NOT_EXISTS, tenantDomain);
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Custom Text preference for tenant: " + tenantDomain + " is retrieved successfully.");
            }
            return buildCustomTextFromResource(inputStream, type, name, screen, locale);
        } catch (ConfigurationManagementException e) {
            if (RESOURCE_NOT_EXISTS_ERROR_CODE.equals(e.getErrorCode())) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Can not find a custom text preference configurations for tenant: " + tenantDomain, e);
                }
                throw handleClientException(ERROR_CODE_CUSTOM_TEXT_PREFERENCE_NOT_EXISTS, tenantDomain);
            }
            throw handleServerException(ERROR_CODE_ERROR_GETTING_CUSTOM_TEXT_PREFERENCE, tenantDomain, e);
        } catch (IOException e) {
            throw handleServerException(ERROR_CODE_ERROR_BUILDING_CUSTOM_TEXT_PREFERENCE, tenantDomain);
        }
    }

    @Override
    public CustomText resolveCustomText(String type, String name, String screen, String locale)
            throws BrandingPreferenceMgtException {

        if (BrandingPreferenceManagerComponentDataHolder.getInstance().getUiBrandingPreferenceResolver() != null) {
            return BrandingPreferenceManagerComponentDataHolder.getInstance().getUiBrandingPreferenceResolver()
                    .resolveCustomText(type, name, screen, locale);
        }
        return getCustomText(type, name, screen, locale);
    }

    @Override
    public CustomText replaceCustomText(CustomText customText)
            throws BrandingPreferenceMgtException {

        String resourceName = getResourceNameForCustomText(customText.getScreen(), customText.getLocale());
        String tenantDomain = getTenantDomain();
        // Check whether the custom text resource exists in the particular tenant.
        if (!isResourceExists(CUSTOM_TEXT_RESOURCE_TYPE, resourceName)) {
            throw handleClientException(ERROR_CODE_CUSTOM_TEXT_PREFERENCE_NOT_EXISTS, tenantDomain);
        }

        String preferencesJSON = generatePreferencesJSONFromPreference(customText.getPreference());
        if (!BrandingPreferenceMgtUtils.isValidJSONString(preferencesJSON)) {
            throw handleClientException(ERROR_CODE_INVALID_BRANDING_PREFERENCE, tenantDomain);
        }

        try (InputStream inputStream = new ByteArrayInputStream(preferencesJSON.getBytes(StandardCharsets.UTF_8))) {
            Resource customTextResource = buildResource(resourceName, inputStream);
            getConfigurationManager().replaceResource(CUSTOM_TEXT_RESOURCE_TYPE, customTextResource);
        } catch (ConfigurationManagementException | IOException e) {
            throw handleServerException(ERROR_CODE_ERROR_UPDATING_CUSTOM_TEXT_PREFERENCE, tenantDomain, e);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Custom Text preference for tenant: " + tenantDomain + " replaced successfully.");
        }
        return customText;
    }

    @Override
    public void deleteCustomText(String type, String name, String screen, String locale)
            throws BrandingPreferenceMgtException {

        String resourceName = getResourceNameForCustomText(screen, locale);
        String tenantDomain = getTenantDomain();
        // Check whether the custom text resource exists in the particular tenant.
        if (!isResourceExists(CUSTOM_TEXT_RESOURCE_TYPE, resourceName)) {
            throw handleClientException(ERROR_CODE_CUSTOM_TEXT_PREFERENCE_NOT_EXISTS, tenantDomain);
        }

        try {
            getConfigurationManager().deleteResource(CUSTOM_TEXT_RESOURCE_TYPE, resourceName);
        } catch (ConfigurationManagementException e) {
            throw handleServerException(ERROR_CODE_ERROR_DELETING_CUSTOM_TEXT_PREFERENCE, tenantDomain);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Custom text preference for tenant: " + tenantDomain + " deleted successfully.");
        }
    }

    @Override
    public void deleteAllCustomText() throws BrandingPreferenceMgtException {

        String tenantDomain = getTenantDomain();
        try {
            getConfigurationManager().deleteResourcesByType(CUSTOM_TEXT_RESOURCE_TYPE);
        } catch (ConfigurationManagementException e) {
            if (RESOURCES_NOT_EXISTS_ERROR_CODE.equals(e.getErrorCode())) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Can not find a custom text preferences for tenant: " + tenantDomain, e);
                }
                throw handleClientException(ERROR_CODE_CUSTOM_TEXT_PREFERENCE_NOT_EXISTS, tenantDomain);
            }
            throw handleServerException(ERROR_CODE_ERROR_BULK_DELETING_CUSTOM_TEXT_PREFERENCES, tenantDomain);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Custom text preferences for tenant: " + tenantDomain + " deleted successfully.");
        }
    }

    /**
     * Check whether a branding preference resource already exists with the same name in the particular tenant.
     *
     * @param resourceType Resource type.
     * @param resourceName Resource name.
     * @return Return true if the resource already exists. If not return false.
     */
    private boolean isResourceExists(String resourceType, String resourceName)
            throws BrandingPreferenceMgtException {

        Resource resource;
        try {
            resource = getConfigurationManager().getResource(resourceType, resourceName);
        } catch (ConfigurationManagementException e) {
            if (RESOURCE_NOT_EXISTS_ERROR_CODE.equals(e.getErrorCode())) {
                return false;
            }
            throw handleServerException(ERROR_CODE_ERROR_CHECKING_BRANDING_PREFERENCE_EXISTS, e);
        }
        return resource != null;
    }

    /**
     * Build a JSON string which contains preferences from a preference object.
     *
     * @param object Preference object of Branding Preference Model/Custom Text Model.
     * @return JSON string which contains preferences.
     */
    private String generatePreferencesJSONFromPreference(Object object) {

        ObjectMapper mapper = new ObjectMapper();
        String preferencesJSON = null;
        try {
            preferencesJSON = mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Error while generating JSON string from the branding preference request.", e);
            }
        }
        return preferencesJSON;
    }

    /**
     * Build a resource object.
     *
     * @param resourceName Preference resource name.
     * @param inputStream  Preference file stream.
     * @return Resource object.
     */
    private Resource buildResource(String resourceName, InputStream inputStream) {

        Resource resource = new Resource();
        resource.setResourceName(resourceName);
        // Set file.
        ResourceFile file = new ResourceFile();
        file.setName(resourceName);
        file.setInputStream(inputStream);
        List<ResourceFile> resourceFiles = new ArrayList<>();
        resourceFiles.add(file);
        resource.setFiles(resourceFiles);
        return resource;
    }

    /**
     * Build a Branding Preference Model from branding preference file stream.
     *
     * @param inputStream Branding Preference file stream.
     * @param type        Branding resource type.
     * @param name        Tenant/Application name.
     * @param locale      Language preference
     * @return Branding Preference.
     */
    private BrandingPreference buildBrandingPreferenceFromResource(InputStream inputStream, String type,
                                                                   String name, String locale)
            throws IOException, BrandingPreferenceMgtException {

        String preferencesJSON = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
        if (!BrandingPreferenceMgtUtils.isValidJSONString(preferencesJSON)) {
            throw handleServerException(ERROR_CODE_ERROR_BUILDING_BRANDING_PREFERENCE, name);
        }

        ObjectMapper mapper = new ObjectMapper();
        Object preference = mapper.readValue(preferencesJSON, Object.class);
        BrandingPreference brandingPreference = new BrandingPreference();
        brandingPreference.setPreference(preference);
        brandingPreference.setType(type);
        brandingPreference.setName(name);
        brandingPreference.setLocale(locale);
        return brandingPreference;
    }

    /**
     * Build a Custom Text Model from custom text preference file stream.
     *
     * @param inputStream Preference file stream.
     * @param type        Custom Text resource type.
     * @param name        Tenant/Application name.
     * @param screen      Screen Name.
     * @param locale      Language preference.
     * @return Custom Text Preference.
     */
    private CustomText buildCustomTextFromResource(InputStream inputStream, String type, String name,
                                                   String screen, String locale)
            throws IOException, BrandingPreferenceMgtException {

        String preferencesJSON = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
        if (!BrandingPreferenceMgtUtils.isValidJSONString(preferencesJSON)) {
            throw handleServerException(ERROR_CODE_ERROR_BUILDING_CUSTOM_TEXT_PREFERENCE, name);
        }

        ObjectMapper mapper = new ObjectMapper();
        Object preference = mapper.readValue(preferencesJSON, Object.class);
        CustomText customText = new CustomText();
        customText.setPreference(preference);
        customText.setType(type);
        customText.setName(name);
        customText.setLocale(locale);
        customText.setScreen(screen);
        return customText;
    }

    /**
     * Generate and return resource name of the branding resource.
     *
     * @param type   Branding resource type.
     * @param name   Tenant/Application name.
     * @param locale Language preference
     * @return resource name of the branding resource.
     */
    private String getResourceName(String type, String name, String locale) {

        if (APPLICATION_TYPE.equals(type)) {
            return name.toLowerCase() + RESOURCE_NAME_SEPARATOR + locale;
        }
        return getTenantId() + RESOURCE_NAME_SEPARATOR + locale;
    }

    /**
     * Generate and return resource name of the custom text resource.
     *
     * @param screen Screen name where the custom texts need to be applied.
     * @param locale Language preference.
     * @return resource name for the custom text preference.
     */
    private String getResourceNameForCustomText(String screen, String locale) {

        String formattedLocale = getFormattedLocale(locale);
        return StringUtils.upperCase(screen) + RESOURCE_NAME_SEPARATOR + StringUtils.lowerCase(formattedLocale);
    }

    /**
     * Return resource type for the given branding type.
     *
     * @param type Branding Type(Organization branding/Application branding).
     * @return resource type of the branding resource.
     */
    private String getResourceType(String type) {

        if (APPLICATION_TYPE.equals(type)) {
            return APPLICATION_BRANDING_RESOURCE_TYPE;
        }
        return BRANDING_RESOURCE_TYPE;
    }

    private boolean isApplicationExists(String identifier, String tenantDomain) {

        //TODO: Implement this to check the existence of application(https://github.com/wso2/product-is/issues/19366).
        return true;
    }

    /**
     * Trigger pre add branding preference events.
     *
     * @param brandingPreference Branding Preference.
     * @param tenantDomain       Tenant domain.
     */
    private void triggerPreAddBrandingPreferenceEvents(BrandingPreference brandingPreference, String tenantDomain)
            throws BrandingPreferenceMgtException {

        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(BRANDING_PREFERENCE, brandingPreference);
        eventProperties.put(TENANT_DOMAIN, tenantDomain);
        Event brandingPreferenceEvent = new Event(PRE_ADD_BRANDING_PREFERENCE, eventProperties);
        try {
            IdentityEventService eventService =
                    BrandingPreferenceManagerComponentDataHolder.getInstance().getIdentityEventService();
            eventService.handleEvent(brandingPreferenceEvent);
        } catch (IdentityEventException e) {
            if (ERROR_CODE_NOT_ALLOWED_BRANDING_PREFERENCE.getCode().equals(e.getErrorCode())) {
                throw new BrandingPreferenceMgtClientException(e.getMessage(), e.getErrorCode());
            }
            throw handleServerException(ERROR_CODE_ERROR_VALIDATING_BRANDING_PREFERENCE, tenantDomain, e);
        }
    }

    /**
     * Trigger pre update branding preference events.
     *
     * @param oldBrandingPreference Old Branding Preference.
     * @param newBrandingPreference New Branding Preference.
     * @param tenantDomain          Tenant domain.
     */
    private void triggerPreUpdateBrandingPreferenceEvents(BrandingPreference oldBrandingPreference,
                                                          BrandingPreference newBrandingPreference, String tenantDomain)
            throws BrandingPreferenceMgtException {

        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(OLD_BRANDING_PREFERENCE, oldBrandingPreference);
        eventProperties.put(NEW_BRANDING_PREFERENCE, newBrandingPreference);
        eventProperties.put(TENANT_DOMAIN, tenantDomain);
        Event brandingPreferenceEvent = new Event(PRE_UPDATE_BRANDING_PREFERENCE, eventProperties);
        try {
            IdentityEventService eventService =
                    BrandingPreferenceManagerComponentDataHolder.getInstance().getIdentityEventService();
            eventService.handleEvent(brandingPreferenceEvent);
        } catch (IdentityEventException e) {
            if (ERROR_CODE_NOT_ALLOWED_BRANDING_PREFERENCE.getCode().equals(e.getErrorCode())) {
                throw new BrandingPreferenceMgtClientException(e.getMessage(), e.getErrorCode());
            }
            throw handleServerException(ERROR_CODE_ERROR_VALIDATING_BRANDING_PREFERENCE, tenantDomain, e);
        }
    }

    private int getTenantId() {

        return PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
    }

    private String getTenantDomain() {

        return PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
    }

    private ConfigurationManager getConfigurationManager() {

        return BrandingPreferenceManagerComponentDataHolder.getInstance().getConfigurationManager();
    }

    private void validatePreferenceUrls(BrandingPreference preference) throws BrandingPreferenceMgtClientException {

        if (preference.getPreference() == null) {
            return;
        }
        LinkedHashMap<String, String> urlsMap =
                (LinkedHashMap) ((LinkedHashMap) preference.getPreference()).get(BRANDING_URLS);
        if (MapUtils.isEmpty(urlsMap)) {
            return;
        }

        for (String url : urlsMap.values()) {
            if (StringUtils.isNotBlank(url)) {
                try {
                    /*
                    Provided string should contain only allowed uri characters with an exception for '{' and '}'
                    since they are required for specifying placeholders. Hence, replacing only these characters with
                    their encoded values.
                    */
                    URI providedUri = new URI(
                            url.replace("{{", "%7B%7B").replace("}}", "%7D%7D"));
                    if (StringUtils.equalsIgnoreCase(providedUri.getScheme(), JAVASCRIPT)) {
                        throw new BrandingPreferenceMgtClientException
                                (ERROR_CODE_INVALID_BRANDING_PREFERENCE.getMessage(),
                                        ERROR_CODE_INVALID_BRANDING_PREFERENCE.getCode());
                    }
                } catch (URISyntaxException e) {
                    throw new BrandingPreferenceMgtClientException
                            (ERROR_CODE_INVALID_BRANDING_PREFERENCE.getMessage(),
                                    ERROR_CODE_INVALID_BRANDING_PREFERENCE.getCode());

                }
            }
        }
    }
}
