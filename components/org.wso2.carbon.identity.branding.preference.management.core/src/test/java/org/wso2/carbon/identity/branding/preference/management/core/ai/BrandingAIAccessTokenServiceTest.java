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

import com.google.gson.JsonSyntaxException;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.branding.preference.management.core.exception.BrandingAIServerException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for BrandingAIAccessTokenService.
 */
public class BrandingAIAccessTokenServiceTest {

    private BrandingAIAccessTokenService.AccessTokenRequestHelper requestHelper;
    private CloseableHttpAsyncClient mockHttpClient;
    private HttpResponse mockHttpResponse;
    private StatusLine mockStatusLine;

    @BeforeMethod
    public void setUp() {

        mockHttpClient = mock(CloseableHttpAsyncClient.class);
        requestHelper = new BrandingAIAccessTokenService.AccessTokenRequestHelper("mockKey",
                "https://mock.endpoint", mockHttpClient);
    }

    @Test
    public void testRequestAccessToken_Success() throws Exception {

        mockHttpResponse = mock(HttpResponse.class);
        mockStatusLine = mock(StatusLine.class);
        when(mockHttpResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(200);
        when(mockHttpResponse.getEntity()).thenReturn(new StringEntity("{\"access_token\":\"mockAccessToken\"}"));

        // Simulate callback success.
        doAnswer(invocation -> {
            FutureCallback<HttpResponse> callback = invocation.getArgument(1);
            callback.completed(mockHttpResponse);
            return null;
        }).when(mockHttpClient).execute(any(), any(FutureCallback.class));

        String accessToken = requestHelper.requestAccessToken();

        Assert.assertEquals(accessToken, "mockAccessToken");
    }

    @Test
    public void testRequestAccessToken_SetsClientId() throws Exception {

        String clientId = "mockClientId";
        String header = Base64.getUrlEncoder().encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().encodeToString(("{\"client_id\":\"" + clientId + "\"}")
                .getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getUrlEncoder().encodeToString("dummySignature".getBytes(StandardCharsets.UTF_8));
        String mockAccessToken = String.format("%s.%s.%s", header, payload, signature);

        mockHttpResponse = mock(HttpResponse.class);
        mockStatusLine = mock(StatusLine.class);
        when(mockHttpResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(200);
        when(mockHttpResponse.getEntity())
                .thenReturn(new StringEntity("{\"access_token\":\"" + mockAccessToken + "\"}"));

        // Simulate callback success
        doAnswer(invocation -> {
            FutureCallback<HttpResponse> callback = invocation.getArgument(1);
            callback.completed(mockHttpResponse);
            return null;
        }).when(mockHttpClient).execute(any(), any(FutureCallback.class));

        BrandingAIAccessTokenService service = BrandingAIAccessTokenService.getInstance();
        service.setAccessTokenRequestHelper(requestHelper);

        String accessToken = service.getAccessToken(true);

        Assert.assertEquals(accessToken, mockAccessToken);
        Assert.assertEquals(service.getClientId(), clientId);
    }


    @Test(expectedExceptions = BrandingAIServerException.class)
    public void testRequestAccessToken_Non200Response() throws Exception {

        mockHttpResponse = mock(HttpResponse.class);
        mockStatusLine = mock(StatusLine.class);
        when(mockHttpResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(401); // Non-200 status code
        when(mockHttpResponse.getEntity()).thenReturn(new StringEntity("Unauthorized"));

        // Count the number of times execute is called.
        final int[] executeCount = {0};

        // Simulate callback with non-200 response
        doAnswer(invocation -> {
            FutureCallback<HttpResponse> callback = invocation.getArgument(1);
            executeCount[0]++;
            callback.completed(mockHttpResponse);
            return null;
        }).when(mockHttpClient).execute(any(), any(FutureCallback.class));

        try {
            requestHelper.requestAccessToken();
        } finally {
            Assert.assertEquals(executeCount[0], 3); // MAX_RETRIES is 3.
        }
    }

    @Test(expectedExceptions = JsonSyntaxException.class)
    public void testRequestAccessToken_ResponseParsingException() throws Exception {

        mockHttpResponse = mock(HttpResponse.class);
        mockStatusLine = mock(StatusLine.class);
        when(mockHttpResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(200);
        when(mockHttpResponse.getEntity()).thenReturn(new StringEntity("Invalid JSON"));

        // Simulate callback with response that causes parsing exception.
        doAnswer(invocation -> {
            FutureCallback<HttpResponse> callback = invocation.getArgument(1);
            callback.completed(mockHttpResponse);
            return null;
        }).when(mockHttpClient).execute(any(), any(FutureCallback.class));

        requestHelper.requestAccessToken();
    }

    @Test(expectedExceptions = BrandingAIServerException.class)
    public void testRequestAccessToken_FailedCallback() throws Exception {

        // Simulate the callback failure on client execution.
        doAnswer(invocation -> {
            FutureCallback<HttpResponse> callback = invocation.getArgument(1);
            callback.failed(new IOException("Simulated failure")); // Trigger a failed response.
            return null;
        }).when(mockHttpClient).execute(any(), any(FutureCallback.class));

        requestHelper.requestAccessToken();
    }

    @Test(expectedExceptions = BrandingAIServerException.class)
    public void testRequestAccessToken_Timeout() throws Exception {

        // Simulate client execution but do not trigger callback.
        doAnswer(invocation -> {
            // Do nothing, so latch.await() will timeout.
            return null;
        }).when(mockHttpClient).execute(any(), any(FutureCallback.class));

        requestHelper.requestAccessToken();
    }

    @Test(expectedExceptions = BrandingAIServerException.class)
    public void testRequestAccessToken_CancelledCallback() throws Exception {

        // Simulate the callback cancelled on client execution.
        doAnswer(invocation -> {
            FutureCallback<HttpResponse> callback = invocation.getArgument(1);
            callback.cancelled();
            return null;
        }).when(mockHttpClient).execute(any(), any(FutureCallback.class));

        requestHelper.requestAccessToken();
    }

    @Test
    public void testRequestAccessToken_ExceptionOnClientClose() throws Exception {

        mockHttpResponse = mock(HttpResponse.class);
        mockStatusLine = mock(StatusLine.class);
        when(mockHttpResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(200);
        when(mockHttpResponse.getEntity()).thenReturn(new StringEntity("{\"access_token\":\"mockAccessToken\"}"));

        // Simulate callback success.
        doAnswer(invocation -> {
            FutureCallback<HttpResponse> callback = invocation.getArgument(1);
            callback.completed(mockHttpResponse);
            return null;
        }).when(mockHttpClient).execute(any(), any(FutureCallback.class));

        // Simulate IOException when closing the client.
        doThrow(new IOException("Simulated close exception")).when(mockHttpClient).close();

        String accessToken = requestHelper.requestAccessToken();

        Assert.assertEquals(accessToken, "mockAccessToken");
        // No exception should be thrown despite the close exception.
    }

    @Test
    public void testGetAccessToken_RenewToken() throws Exception {

        BrandingAIAccessTokenService service = BrandingAIAccessTokenService.getInstance();
        BrandingAIAccessTokenService.AccessTokenRequestHelper mockHelper = mock(
                BrandingAIAccessTokenService.AccessTokenRequestHelper.class);
        service.setAccessTokenRequestHelper(mockHelper);

        when(mockHelper.requestAccessToken()).thenReturn("newAccessToken");

        // Act
        String accessToken = service.getAccessToken(true);

        // Assert
        Assert.assertEquals(accessToken, "newAccessToken");
    }

    @Test
    public void testGetAccessToken_CachedToken() throws Exception {

        // Arrange
        BrandingAIAccessTokenService service = BrandingAIAccessTokenService.getInstance();
        BrandingAIAccessTokenService.AccessTokenRequestHelper mockHelper = mock(
                BrandingAIAccessTokenService.AccessTokenRequestHelper.class);
        service.setAccessTokenRequestHelper(mockHelper);

        when(mockHelper.requestAccessToken()).thenReturn("cachedAccessToken");

        // First call to getAccessToken to set the token
        String accessToken1 = service.getAccessToken(false);

        // Second call, should return the cached token and not call requestAccessToken again
        String accessToken2 = service.getAccessToken(false);

        // Verify that requestAccessToken is called only once
        verify(mockHelper, times(1)).requestAccessToken();

        // Assert
        Assert.assertEquals(accessToken1, "cachedAccessToken");
        Assert.assertEquals(accessToken2, "cachedAccessToken");
    }

    @Test
    public void testRequestAccessToken_RetrySuccess() throws Exception {

        mockHttpResponse = mock(HttpResponse.class);
        mockStatusLine = mock(StatusLine.class);
        when(mockHttpResponse.getEntity()).thenReturn(new StringEntity("{\"access_token\":\"mockAccessToken\"}"));

        // Simulate first attempt fails with non-200, second attempt succeeds
        when(mockStatusLine.getStatusCode())
                .thenReturn(500)  // First attempt: Server error
                .thenReturn(200); // Second attempt: Success

        when(mockHttpResponse.getStatusLine()).thenReturn(mockStatusLine);

        // Count the number of times execute is called
        final int[] executeCount = {0};

        doAnswer(invocation -> {
            FutureCallback<HttpResponse> callback = invocation.getArgument(1);
            executeCount[0]++;

            if (executeCount[0] == 1) {
                // First attempt: simulate non-200 response
                callback.completed(mockHttpResponse);
            } else if (executeCount[0] == 2) {
                // Second attempt: simulate 200 response
                callback.completed(mockHttpResponse);
            }
            return null;
        }).when(mockHttpClient).execute(any(), any(FutureCallback.class));

        String accessToken = requestHelper.requestAccessToken();

        Assert.assertEquals(accessToken, "mockAccessToken");
        Assert.assertEquals(executeCount[0], 2); // Verify that it retried once
    }

    @Test(expectedExceptions = BrandingAIServerException.class)
    public void testRequestAccessToken_TimeoutRetries() throws Exception {

        // Count the number of times execute is called
        final int[] executeCount = {0};

        // Simulate client execution but do not trigger callback
        doAnswer(invocation -> {
            executeCount[0]++;
            // Do nothing, so latch.await() will timeout
            return null;
        }).when(mockHttpClient).execute(any(), any(FutureCallback.class));

        // Act
        try {
            requestHelper.requestAccessToken();
        } finally {
            // Assert
            Assert.assertEquals(executeCount[0], 3); // MAX_RETRIES is 3
        }
    }

    @Test
    public void testSetAccessTokenRequestHelper() {

        BrandingAIAccessTokenService service = BrandingAIAccessTokenService.getInstance();
        BrandingAIAccessTokenService.AccessTokenRequestHelper helper = new BrandingAIAccessTokenService
                .AccessTokenRequestHelper("key", "endpoint", mockHttpClient);

        service.setAccessTokenRequestHelper(helper);

        // Since the helper is private, we can test by checking if getAccessToken uses the helper we set
        // We'll mock the helper's requestAccessToken method.
        BrandingAIAccessTokenService.AccessTokenRequestHelper mockHelper = mock(
                BrandingAIAccessTokenService.AccessTokenRequestHelper.class);
        service.setAccessTokenRequestHelper(mockHelper);
        try {
            when(mockHelper.requestAccessToken()).thenReturn("testToken");
            String token = service.getAccessToken(true);
            Assert.assertEquals(token, "testToken");
        } catch (BrandingAIServerException e) {
            Assert.fail("Exception should not be thrown");
        }
    }
}
