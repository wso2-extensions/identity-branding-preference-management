package org.wso2.carbon.identity.branding.preference.resolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.branding.preference.management.core.UIBrandingPreferenceResolver;
import org.wso2.carbon.identity.branding.preference.management.core.exception.BrandingPreferenceMgtException;
import org.wso2.carbon.identity.branding.preference.management.core.model.BrandingPreference;
import org.wso2.carbon.identity.branding.preference.management.core.util.BrandingPreferenceMgtUtils;
import org.wso2.carbon.identity.branding.preference.resolver.internal.BrandingResolverComponentDataHolder;
import org.wso2.carbon.identity.configuration.mgt.core.ConfigurationManager;
import org.wso2.carbon.identity.configuration.mgt.core.exception.ConfigurationManagementException;
import org.wso2.carbon.identity.configuration.mgt.core.model.ResourceFile;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.model.Organization;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.BRANDING_RESOURCE_TYPE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_BRANDING_PREFERENCE_NOT_EXISTS;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_BUILDING_BRANDING_PREFERENCE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_CODE_ERROR_GETTING_BRANDING_PREFERENCE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.RESOURCE_NAME_SEPARATOR;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.RESOURCE_NOT_EXISTS_ERROR_CODE;
import static org.wso2.carbon.identity.branding.preference.management.core.util.BrandingPreferenceMgtUtils.handleClientException;
import static org.wso2.carbon.identity.branding.preference.management.core.util.BrandingPreferenceMgtUtils.handleServerException;

/**
 * UI Branding Preference Resolver Implementation.
 */
public class UIBrandingPreferenceResolverImpl implements UIBrandingPreferenceResolver {

    private static final Log LOG = LogFactory.getLog(UIBrandingPreferenceResolverImpl.class);

    @Override
    public BrandingPreference resolveBranding(String type, String name, String locale)
            throws BrandingPreferenceMgtException {

        Optional<BrandingPreference> brandingPreference =
                getBrandingPreference(type, name, locale, getTenantDomain());
        if (brandingPreference.isPresent()) {
            return brandingPreference.get();
        }

        try {
            String organizationId = getOrganizationId();

            OrganizationManager organizationManager =
                    BrandingResolverComponentDataHolder.getInstance().getOrganizationManager();

            // Get the details of the parent organization and resolve the branding preferences.
            Organization organization = organizationManager.getOrganization(organizationId, false, false);
            String parentId = organization.getParent().getId();
            String parentTenantDomain = organizationManager.resolveTenantDomain(parentId);
            int parentDepthInHierarchy = organizationManager.getOrganizationDepthInHierarchy(parentId);

            while (parentDepthInHierarchy > 0) {
                try {
                    PrivilegedCarbonContext.startTenantFlow();
                    PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(parentTenantDomain, true);
                    PrivilegedCarbonContext.getThreadLocalCarbonContext().setOrganizationId(parentId);

                    brandingPreference =
                            getBrandingPreference(type, name, locale, parentTenantDomain);
                    if (brandingPreference.isPresent()) {
                        return brandingPreference.get();
                    }

                    // Go to the parent organization again.
                    organization = organizationManager.getOrganization(organizationId, false, false);
                    parentId = organization.getParent().getId();
                    parentTenantDomain = organizationManager.resolveTenantDomain(parentId);
                    parentDepthInHierarchy = organizationManager.getOrganizationDepthInHierarchy(parentId);
                } finally {
                    PrivilegedCarbonContext.endTenantFlow();
                }
            }

        } catch (OrganizationManagementException e) {
            throw new RuntimeException(e);
        }

        throw handleClientException(ERROR_CODE_BRANDING_PREFERENCE_NOT_EXISTS, getTenantDomain());
    }

    private Optional<BrandingPreference> getBrandingPreference(String type, String name, String locale,
                                                               String tenantDomain)
            throws BrandingPreferenceMgtException {

        try {
            String resourceName = getResourceName(type, name, locale);
            List<ResourceFile> resourceFiles = getConfigurationManager().getFiles(BRANDING_RESOURCE_TYPE, resourceName);
            if (resourceFiles.isEmpty()) {
                return Optional.empty();
            }
            if (StringUtils.isBlank(resourceFiles.get(0).getId())) {
                return Optional.empty();
            }

            InputStream inputStream = getConfigurationManager().getFileById
                    (BRANDING_RESOURCE_TYPE, resourceName, resourceFiles.get(0).getId());
            if (inputStream == null) {
                return Optional.empty();
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Branding preference for tenant: " + tenantDomain + " is retrieved successfully.");
            }
            return Optional.of(buildBrandingPreferenceFromResource(inputStream, type, name, locale));
        } catch (ConfigurationManagementException e) {
            if (!RESOURCE_NOT_EXISTS_ERROR_CODE.equals(e.getErrorCode())) {
                throw handleServerException(ERROR_CODE_ERROR_GETTING_BRANDING_PREFERENCE, tenantDomain, e);
            }
        } catch (IOException e) {
            throw handleServerException(ERROR_CODE_ERROR_BUILDING_BRANDING_PREFERENCE, tenantDomain);
        }
        return Optional.empty();
    }

    private ConfigurationManager getConfigurationManager() {

        return BrandingResolverComponentDataHolder.getInstance().getConfigurationManager();
    }

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


    private String getTenantDomain() {

        return PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
    }

    private int getTenantId() {

        return PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
    }
    private String getOrganizationId() {

        return PrivilegedCarbonContext.getThreadLocalCarbonContext().getOrganizationId();
    }

    private String getResourceName(String type, String name, String locale) {

        /*
          Currently, this API provides the support to only configure tenant wise branding preference for 'en-US' locale.
          So always use resource name as default resource name.
          Default resource name is the name used to save organization level branding for 'en-US' language.
         */
        return getTenantId() + RESOURCE_NAME_SEPARATOR + locale;
    }
}
