package org.wso2.carbon.identity.branding.preference.management.core;

import org.wso2.carbon.identity.branding.preference.management.core.exception.BrandingPreferenceMgtException;
import org.wso2.carbon.identity.branding.preference.management.core.model.BrandingPreference;

/**
 * UI Branding Preference Resolver.
 */
public interface UIBrandingPreferenceResolver {

    /**
     * This method is used to retrieve a resolved branding preference.
     *
     * @param type   Type of the branding preference.
     * @param name   Name of the tenant/application.
     * @param locale Language preference of the branding.
     * @return The requested branding preference. If not exists return the default branding preference.
     * @throws BrandingPreferenceMgtException if any error occurred.
     */
    BrandingPreference resolveBranding(String type, String name, String locale) throws BrandingPreferenceMgtException;

    /**
     * This method is used to clear the branding preference resolver cache.
     *
     * @param currentTenantDomain   Tenant domain where the cache needs to be cleared.
     * @throws BrandingPreferenceMgtException if any error occurred.
     */
    void clearBrandingResolverCache(String currentTenantDomain) throws BrandingPreferenceMgtException;
}
