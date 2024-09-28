/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.branding.preference.management.core.ai;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.base.CarbonBaseConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.branding.preference.management.core.exception.BrandingAIClientException;
import org.wso2.carbon.identity.branding.preference.management.core.exception.BrandingAIServerException;
import org.wso2.carbon.identity.common.testng.realm.InMemoryRealmService;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.user.core.UserStoreException;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.wso2.carbon.base.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME;
import static org.wso2.carbon.base.MultitenantConstants.SUPER_TENANT_ID;

public class BrandingAIPreferenceManagerImplTest {

    private MockedStatic<BrandingAIAccessTokenService> brandingTokenServiceMock;
    private MockedStatic<BrandingAIPreferenceManagerImpl.HttpClientHelper> httpClientHelperMockedStatic;

    @InjectMocks
    private BrandingAIPreferenceManagerImpl aiBrandingPreferenceManager;

    @BeforeMethod
    public void setUp() throws UserStoreException {

        initMocks(this);
        setCarbonHome();
        setCarbonContextForTenant(SUPER_TENANT_DOMAIN_NAME, SUPER_TENANT_ID);
        brandingTokenServiceMock = mockStatic(BrandingAIAccessTokenService.class);
        httpClientHelperMockedStatic = mockStatic(BrandingAIPreferenceManagerImpl.HttpClientHelper.class);
    }

    @Test
    public void testGenerateBrandingPreference_Success() throws Exception {

        mockSuccessfulResponse("{\"operation_id\": \"12345\"}", HttpPost.class);
        String result = aiBrandingPreferenceManager.generateBrandingPreference("https://example.com");
        Assert.assertEquals(result, "12345");
    }

    @Test
    public void testGetBrandingPreferenceGenerationStatus_Success() throws Exception {

        mockSuccessfulResponse("{\"status\":\"COMPLETED\"}", HttpGet.class);
        Object result = aiBrandingPreferenceManager.getBrandingPreferenceGenerationStatus("operation123");
        Assert.assertTrue(result instanceof Map);
        Map<String, Object> resultMap = (Map<String, Object>) result;
        Assert.assertEquals(resultMap.get("status"), "COMPLETED");
    }

    @Test
    public void testGetBrandingPreferenceGenerationResult_Success() throws Exception {

        mockSuccessfulResponse("{\"result\":\"SUCCESS\"}", HttpGet.class);
        Object result = aiBrandingPreferenceManager.getBrandingPreferenceGenerationResult("operation123");
        Assert.assertTrue(result instanceof Map);
        Map<String, Object> resultMap = (Map<String, Object>) result;
        Assert.assertEquals(resultMap.get("result"), "SUCCESS");
    }

    @Test
    public void testTokenRenewalVerification() throws Exception {

        BrandingAIAccessTokenService mockTokenService = mock(BrandingAIAccessTokenService.class);
        brandingTokenServiceMock.when(BrandingAIAccessTokenService::getInstance).thenReturn(mockTokenService);
        when(mockTokenService.getAccessToken(false)).thenReturn("initialMockToken");
        when(mockTokenService.getAccessToken(true)).thenReturn("renewedMockToken");
        when(mockTokenService.getClientId()).thenReturn("mockOrgName");

        BrandingAIPreferenceManagerImpl.HttpResponseWrapper unauthorizedResponse =
                new BrandingAIPreferenceManagerImpl.HttpResponseWrapper(401, "Unauthorized");
        BrandingAIPreferenceManagerImpl.HttpResponseWrapper successResponse =
                new BrandingAIPreferenceManagerImpl.HttpResponseWrapper(200, "{\"result\":\"SUCCESS\"}");

        httpClientHelperMockedStatic.when(() -> BrandingAIPreferenceManagerImpl.HttpClientHelper.executeRequest(
                        any(CloseableHttpAsyncClient.class), any(HttpUriRequest.class)))
                .thenReturn(unauthorizedResponse)
                .thenReturn(successResponse);

        Object result = aiBrandingPreferenceManager.getBrandingPreferenceGenerationResult("operation123");
        Assert.assertTrue(result instanceof Map);
        Map<String, Object> resultMap = (Map<String, Object>) result;
        Assert.assertEquals(resultMap.get("result"), "SUCCESS");
        verify(mockTokenService).getAccessToken(true);
    }

    @Test(expectedExceptions = BrandingAIServerException.class)
    public void testTokenRenewalFailure() throws Exception {

        BrandingAIAccessTokenService mockTokenService = mock(BrandingAIAccessTokenService.class);
        brandingTokenServiceMock.when(BrandingAIAccessTokenService::getInstance).thenReturn(mockTokenService);
        when(mockTokenService.getAccessToken(false)).thenReturn("initialMockToken");
        when(mockTokenService.getAccessToken(true)).thenReturn(null);
        when(mockTokenService.getClientId()).thenReturn("mockClientId");

        BrandingAIPreferenceManagerImpl.HttpResponseWrapper unauthorizedResponse =
                new BrandingAIPreferenceManagerImpl.HttpResponseWrapper(401, "Unauthorized");
        httpClientHelperMockedStatic.when(() -> BrandingAIPreferenceManagerImpl.HttpClientHelper.executeRequest(
                any(CloseableHttpAsyncClient.class), any(HttpUriRequest.class))).thenReturn(unauthorizedResponse);

        aiBrandingPreferenceManager.getBrandingPreferenceGenerationResult("operation123");
    }

    @Test(expectedExceptions = BrandingAIClientException.class)
    public void testClientError() throws Exception {
        mockErrorResponse(400, "Client Error");
        aiBrandingPreferenceManager.getBrandingPreferenceGenerationResult("operation123");
    }

    @Test(expectedExceptions = BrandingAIServerException.class)
    public void testServerError() throws Exception {
        mockErrorResponse(500, "Server Error");
        aiBrandingPreferenceManager.getBrandingPreferenceGenerationResult("operation123");
    }

    @Test(expectedExceptions = BrandingAIServerException.class)
    public void testParsingError() throws Exception {
        mockSuccessfulResponse("{invalid_json}", HttpGet.class);
        aiBrandingPreferenceManager.getBrandingPreferenceGenerationResult("operation123");
    }

    @Test(expectedExceptions = BrandingAIServerException.class)
    public void testIOException() throws Exception {
        mockExceptionDuringRequest(new IOException("Simulated IOException"));
        aiBrandingPreferenceManager.getBrandingPreferenceGenerationResult("operation123");
    }

    @Test(expectedExceptions = BrandingAIServerException.class)
    public void testInterruptedException() throws Exception {
        mockExceptionDuringRequest(new InterruptedException("Simulated InterruptedException"));
        aiBrandingPreferenceManager.getBrandingPreferenceGenerationResult("operation123");
    }

    private void mockSuccessfulResponse(String responseBody, Class<? extends HttpUriRequest> requestClass)
            throws Exception {
        BrandingAIAccessTokenService mockTokenService = mock(BrandingAIAccessTokenService.class);
        brandingTokenServiceMock.when(BrandingAIAccessTokenService::getInstance).thenReturn(mockTokenService);
        when(mockTokenService.getAccessToken(anyBoolean())).thenReturn("mockAccessToken");

        BrandingAIPreferenceManagerImpl.HttpResponseWrapper mockResponse = new BrandingAIPreferenceManagerImpl
                .HttpResponseWrapper(200, responseBody);
        httpClientHelperMockedStatic.when(() -> BrandingAIPreferenceManagerImpl.HttpClientHelper.executeRequest(
                any(CloseableHttpAsyncClient.class), any(requestClass))).thenReturn(mockResponse);
    }

    private void mockErrorResponse(int statusCode, String responseBody) throws Exception {
        BrandingAIAccessTokenService mockTokenService = mock(BrandingAIAccessTokenService.class);
        brandingTokenServiceMock.when(BrandingAIAccessTokenService::getInstance).thenReturn(mockTokenService);
        when(mockTokenService.getAccessToken(false)).thenReturn("mockAccessToken");

        BrandingAIPreferenceManagerImpl.HttpResponseWrapper errorResponse =
                new BrandingAIPreferenceManagerImpl.HttpResponseWrapper(statusCode, responseBody);
        httpClientHelperMockedStatic.when(() -> BrandingAIPreferenceManagerImpl.HttpClientHelper.executeRequest(
                any(CloseableHttpAsyncClient.class), any(HttpUriRequest.class))).thenReturn(errorResponse);
    }

    private void mockExceptionDuringRequest(Exception exception) throws Exception {
        BrandingAIAccessTokenService mockTokenService = mock(BrandingAIAccessTokenService.class);
        brandingTokenServiceMock.when(BrandingAIAccessTokenService::getInstance).thenReturn(mockTokenService);
        when(mockTokenService.getAccessToken(false)).thenReturn("mockAccessToken");

        httpClientHelperMockedStatic.when(() -> BrandingAIPreferenceManagerImpl.HttpClientHelper.executeRequest(
                any(CloseableHttpAsyncClient.class), any(HttpUriRequest.class))).thenThrow(exception);
    }

    private void setCarbonHome() {
        String carbonHome = Paths.get(System.getProperty("user.dir"), "target", "test-classes",
                "repository").toString();
        System.setProperty(CarbonBaseConstants.CARBON_HOME, carbonHome);
        System.setProperty(CarbonBaseConstants.CARBON_CONFIG_DIR_PATH, Paths.get(carbonHome, "conf").toString());
    }

    private void setCarbonContextForTenant(String tenantDomain, int tenantId) throws UserStoreException {
        PrivilegedCarbonContext.startTenantFlow();
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain);
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(tenantId);
        InMemoryRealmService testSessionRealmService = new InMemoryRealmService(tenantId);
        IdentityTenantUtil.setRealmService(testSessionRealmService);
    }

    @AfterMethod
    public void tearDown() {
        brandingTokenServiceMock.close();
        httpClientHelperMockedStatic.close();
    }
}
