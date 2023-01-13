package org.wso2.carbon.identity.branding.preference.management.core;

import org.wso2.carbon.identity.branding.preference.management.core.exception.BrandingPreferenceMgtException;
import org.wso2.carbon.identity.branding.preference.management.core.model.BrandingPreference;

/**
 * UI Branding Preference Resolver.
 */
public interface UIBrandingPreferenceResolver {

    BrandingPreference resolveBranding(String type, String name, String locale) throws BrandingPreferenceMgtException;
}
