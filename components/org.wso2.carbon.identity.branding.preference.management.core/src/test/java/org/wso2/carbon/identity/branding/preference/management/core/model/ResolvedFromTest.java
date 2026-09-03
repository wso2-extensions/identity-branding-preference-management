/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.branding.preference.management.core.model;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.APPLICATION_TYPE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ORGANIZATION_TYPE;

/**
 * Unit tests for ResolvedFrom.
 */
public class ResolvedFromTest {

    private static final String ORGANIZATION = "root-organization";
    private static final String APPLICATION = "fa9b9ac5-a429-49e2-9c51-4259c7ebe45e";

    @Test
    public void testOrganizationTypeResolvedFrom() {

        ResolvedFrom resolvedFrom = new ResolvedFrom(ORGANIZATION_TYPE, ORGANIZATION, null);

        assertEquals(resolvedFrom.getType(), ORGANIZATION_TYPE);
        assertEquals(resolvedFrom.getOrganization(), ORGANIZATION);
        assertNull(resolvedFrom.getApplication());
    }

    @Test
    public void testApplicationTypeResolvedFrom() {

        ResolvedFrom resolvedFrom = new ResolvedFrom(APPLICATION_TYPE, ORGANIZATION, APPLICATION);

        assertEquals(resolvedFrom.getType(), APPLICATION_TYPE);
        assertEquals(resolvedFrom.getOrganization(), ORGANIZATION);
        assertEquals(resolvedFrom.getApplication(), APPLICATION);
    }

    @Test(description = "Test that the deprecated name retains the organization for the ORG type and the " +
            "application for the APP type.")
    public void testDeprecatedNameIsDerivedFromTheType() {

        assertEquals(new ResolvedFrom(ORGANIZATION_TYPE, ORGANIZATION, null).getName(), ORGANIZATION);
        assertEquals(new ResolvedFrom(APPLICATION_TYPE, ORGANIZATION, APPLICATION).getName(), APPLICATION);
    }

    @Test(description = "Test that the deprecated constructor assigns the name based on the type.")
    public void testDeprecatedConstructor() {

        ResolvedFrom orgResolvedFrom = new ResolvedFrom(ORGANIZATION_TYPE, ORGANIZATION);
        assertEquals(orgResolvedFrom.getOrganization(), ORGANIZATION);
        assertNull(orgResolvedFrom.getApplication());
        assertEquals(orgResolvedFrom.getName(), ORGANIZATION);

        ResolvedFrom appResolvedFrom = new ResolvedFrom(APPLICATION_TYPE, APPLICATION);
        assertNull(appResolvedFrom.getOrganization());
        assertEquals(appResolvedFrom.getApplication(), APPLICATION);
        assertEquals(appResolvedFrom.getName(), APPLICATION);
    }
}
