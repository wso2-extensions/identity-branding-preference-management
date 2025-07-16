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

package org.wso2.carbon.identity.branding.preference.management.core.listener;

import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.branding.preference.management.core.BrandingPreferenceManagerImpl;
import org.wso2.carbon.identity.branding.preference.management.core.exception.BrandingPreferenceMgtClientException;
import org.wso2.carbon.identity.branding.preference.management.core.exception.BrandingPreferenceMgtException;
import org.wso2.carbon.identity.branding.preference.management.core.model.BrandingPreference;
import org.wso2.carbon.identity.core.ServiceURL;
import org.wso2.carbon.identity.core.ServiceURLBuilder;
import org.wso2.carbon.identity.core.URLBuilderException;
import org.wso2.carbon.identity.core.context.model.Flow;
import org.wso2.carbon.identity.flow.execution.engine.model.FlowExecutionContext;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.DEFAULT_LOCALE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ORGANIZATION_TYPE;

/**
 * Unit tests for {@link PortalURLResolver}.
 */
public class PortalURLResolverTest {

    public static final String SELF_SIGNUP_PORTAL_URL = "https://signup.wso2.com";
    public static final String RECOVERY_PORTAL_URL = "https://recovery.wso2.com";
    @Mock
    private BrandingPreferenceManagerImpl brandingPreferenceManager;

    @Mock
    private FlowExecutionContext flowContext;

    @Mock
    private BrandingPreference brandingPreference;

    private PortalURLResolver resolver;

    @BeforeMethod
    public void setup() {

        MockitoAnnotations.openMocks(this);
        resolver = new PortalURLResolver(brandingPreferenceManager);
    }

    @Test
    public void skipSetPortalUrl() {

        when(flowContext.getPortalUrl()).thenReturn("https://existing.url");

        boolean result = resolver.doPreExecute(flowContext);

        assertTrue(result);
        verify(flowContext, never()).setPortalUrl(any());
    }

    @DataProvider
    public Object[][] flowTypes() {

        return new Object[][]{
                {"PASSWORD_RECOVERY", "", "recoveryPortalURL", RECOVERY_PORTAL_URL},
                {"REGISTRATION", "appId", "selfSignUpURL", SELF_SIGNUP_PORTAL_URL},
                {"INVITED_USER_REGISTRATION", "", "recoveryPortalURL", RECOVERY_PORTAL_URL}
        };
    }

    @Test(dataProvider = "flowTypes")
    public void testValidPreferenceSetsConfiguredUrl(String flowType, String applicationId, String portalURL,
                                                     String configuredUrl)
            throws Exception {

        when(flowContext.getPortalUrl()).thenReturn(null);
        when(flowContext.getApplicationId()).thenReturn(applicationId);
        when(flowContext.getTenantDomain()).thenReturn("wso2.com");
        when(flowContext.getFlowType()).thenReturn(flowType);

        Map<String, String> urlsMap = new HashMap<>();
        urlsMap.put(portalURL, configuredUrl);
        Map<String, Object> prefMap = new HashMap<>();
        prefMap.put("urls", urlsMap);

        when(brandingPreferenceManager.getBrandingPreference(anyString(), anyString(), anyString()))
                .thenReturn(brandingPreference);
        when(brandingPreference.getPreference()).thenReturn(prefMap);

        try (MockedStatic<ServiceURLBuilder> serviceURLBuilder = mockStatic(ServiceURLBuilder.class)) {
            mockServiceURLBuilder();
            boolean result = resolver.doPreExecute(flowContext);
            assertTrue(result);
            verify(flowContext).setPortalUrl(configuredUrl);
        }
    }

    @Test
    public void validPref_setsCustomSignupUrl() throws Exception {

        when(flowContext.getPortalUrl()).thenReturn(null);
        when(flowContext.getApplicationId()).thenReturn(null);
        when(flowContext.getTenantDomain()).thenReturn("wso2.com");
        when(flowContext.getFlowType()).thenReturn("REGISTRATION");

        Map<String, String> urlsMap = new HashMap<>();
        urlsMap.put("selfSignUpURL", SELF_SIGNUP_PORTAL_URL);
        Map<String, Object> prefMap = new HashMap<>();
        prefMap.put("urls", urlsMap);

        when(brandingPreferenceManager.getBrandingPreference(ORGANIZATION_TYPE, "wso2.com", DEFAULT_LOCALE))
                .thenReturn(brandingPreference);
        when(brandingPreference.getPreference()).thenReturn(prefMap);

        try (MockedStatic<ServiceURLBuilder> serviceURLBuilder = mockStatic(ServiceURLBuilder.class)) {

            mockServiceURLBuilder();
            boolean result = resolver.doPreExecute(flowContext);
            assertTrue(result);
            verify(flowContext).setPortalUrl(SELF_SIGNUP_PORTAL_URL);
        }
    }

    @Test
    public void fallBackToDefaultUrl() throws Exception {

        when(flowContext.getPortalUrl()).thenReturn("");
        when(flowContext.getApplicationId()).thenReturn(null);
        when(flowContext.getTenantDomain()).thenReturn("wso2.com");
        when(flowContext.getFlowType()).thenReturn("REGISTRATION");

        when(brandingPreferenceManager.getBrandingPreference(ORGANIZATION_TYPE, "wso2.com", DEFAULT_LOCALE))
                .thenReturn(null);

        try (MockedStatic<ServiceURLBuilder> serviceURLBuilder = mockStatic(ServiceURLBuilder.class)) {

            mockServiceURLBuilder();
            boolean result = resolver.doPreExecute(flowContext);
            assertTrue(result);
        }
    }

    @Test
    public void fallBackToDefaultRecoveryUrl() throws Exception {

        when(flowContext.getPortalUrl()).thenReturn(null);
        when(flowContext.getApplicationId()).thenReturn(null);
        when(flowContext.getTenantDomain()).thenReturn("wso2.com");
        when(flowContext.getFlowType()).thenReturn(Flow.Name.INVITED_USER_REGISTRATION.name());

        when(brandingPreferenceManager.getBrandingPreference(ORGANIZATION_TYPE, "wso2.com", DEFAULT_LOCALE))
                .thenReturn(null);

        try (MockedStatic<ServiceURLBuilder> serviceURLBuilder = mockStatic(ServiceURLBuilder.class)) {
            mockServiceURLBuilder();
            boolean result = resolver.doPreExecute(flowContext);
            assertTrue(result);
            verify(flowContext).setPortalUrl("https://default.url");
        }
    }

    @Test
    public void prefWithoutSelfSignupUrl() throws Exception {

        when(flowContext.getPortalUrl()).thenReturn(null);
        when(flowContext.getApplicationId()).thenReturn(null);
        when(flowContext.getTenantDomain()).thenReturn("wso2.com");
        when(flowContext.getFlowType()).thenReturn("REGISTRATION");

        Map<String, Object> prefMap = new HashMap<>();
        prefMap.put("urls", new HashMap<>());
        when(brandingPreferenceManager.getBrandingPreference(ORGANIZATION_TYPE, "wso2.com", DEFAULT_LOCALE))
                .thenReturn(brandingPreference);
        when(brandingPreference.getPreference()).thenReturn(prefMap);

        try (MockedStatic<ServiceURLBuilder> serviceURLBuilder = mockStatic(ServiceURLBuilder.class)) {

            mockServiceURLBuilder();
            boolean result = resolver.doPreExecute(flowContext);
            assertTrue(result);
        }
    }

    private static void mockServiceURLBuilder() throws URLBuilderException {

        ServiceURLBuilder serviceURLBuilderMock = mock(ServiceURLBuilder.class);
        when(ServiceURLBuilder.create()).thenReturn(serviceURLBuilderMock);
        when(serviceURLBuilderMock.addPath(anyString())).thenReturn(serviceURLBuilderMock);
        ServiceURL serviceURL = mock(ServiceURL.class);
        when(serviceURLBuilderMock.build()).thenReturn(serviceURL);
        String url = "https://default.url";
        when(serviceURL.getAbsolutePublicURL()).thenReturn(url);
    }

    @Test
    public void clientExceptionForMissingConfig() throws Exception {

        when(flowContext.getPortalUrl()).thenReturn(null);
        when(flowContext.getApplicationId()).thenReturn(null);
        when(flowContext.getTenantDomain()).thenReturn("foo.com");
        when(flowContext.getFlowType()).thenReturn("REGISTRATION");

        BrandingPreferenceMgtClientException ex = new BrandingPreferenceMgtClientException("Not configured",
                "BRAND-60001");
        when(brandingPreferenceManager.getBrandingPreference(ORGANIZATION_TYPE, "foo.com",
                DEFAULT_LOCALE)).thenThrow(ex);

        try (MockedStatic<ServiceURLBuilder> serviceURLBuilder = mockStatic(ServiceURLBuilder.class)) {

            mockServiceURLBuilder();
            boolean result = resolver.doPreExecute(flowContext);
            assertTrue(result);
        }
    }

    @Test
    public void internalErrorDuringURLResolving() throws Exception {

        when(flowContext.getPortalUrl()).thenReturn(null);
        when(flowContext.getApplicationId()).thenReturn(null);
        when(flowContext.getTenantDomain()).thenReturn("foo.com");
        when(flowContext.getFlowType()).thenReturn("REGISTRATION");

        when(brandingPreferenceManager.getBrandingPreference(any(), any(), any()))
                .thenThrow(new BrandingPreferenceMgtException("Something broke", "BRAND-600xx"));

        boolean result = resolver.doPreExecute(flowContext);

        assertFalse(result);
    }
}
