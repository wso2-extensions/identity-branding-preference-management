package org.wso2.carbon.identity.branding.preference.management.core;

import org.wso2.carbon.identity.branding.preference.management.core.exception.BrandingPreferenceMgtException;
import org.wso2.carbon.identity.branding.preference.management.core.exception.NotImplementedException;
import org.wso2.carbon.identity.branding.preference.management.core.model.BrandingPreference;
import org.wso2.carbon.identity.branding.preference.management.core.model.CustomText;

/**
 * UI Branding Preference Resolver.
 */
public interface UIBrandingPreferenceResolver {

    BrandingPreference resolveBranding(String type, String name, String locale) throws BrandingPreferenceMgtException;

    default CustomText resolveCustomText(String type, String name, String screen, String locale)
            throws BrandingPreferenceMgtException {

        throw new NotImplementedException("This functionality is not implemented.");
    }
}
