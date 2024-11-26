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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.util.EntityUtils;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.branding.preference.management.core.exception.BrandingAIClientException;
import org.wso2.carbon.identity.branding.preference.management.core.exception.BrandingAIServerException;
import org.wso2.carbon.identity.core.util.IdentityUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_RETRIEVING_ACCESS_TOKEN;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_WHILE_CONNECTING_TO_BRANDING_AI_SERVICE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.ERROR_WHILE_GENERATING_BRANDING_PREFERENCE;
import static org.wso2.carbon.identity.branding.preference.management.core.constant.BrandingPreferenceMgtConstants.ErrorMessages.UNABLE_TO_ACCESS_AI_SERVICE_WITH_RENEW_ACCESS_TOKEN;

/**
 * Implementation of the AI Branding Preference Manager.
 */
public class BrandingAIPreferenceManagerImpl implements BrandingAIPreferenceManager {

    private static final String BRANDING_AI_ENDPOINT = IdentityUtil.getProperty(
            "AIServices.BrandingAI.BrandingAIEndpoint");
    private static final String BRANDING_AI_GENERATE_ENDPOINT = "/api/server/v1/branding-preference/generate";
    private static final String BRANDING_AI_STATUS_ENDPOINT = "/api/server/v1/branding-preference/status";
    private static final String BRANDING_AI_RESULT_ENDPOINT = "/api/server/v1/branding-preference/result";

    private static final Log LOG = LogFactory.getLog(BrandingAIPreferenceManagerImpl.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * This method will connect to the BrandingAI microservice and generate the branding preference for the given
     * website URL.
     *
     * @param websiteURL Website URL for which the branding preference should be generated.
     * @return Operation ID of the branding preference generation.
     * @throws BrandingAIServerException When errors occurs from the AI service.
     * @throws BrandingAIClientException When invalid request is sent to the AI service.
     */
    @Override
    public String generateBrandingPreference(String websiteURL) throws BrandingAIServerException,
            BrandingAIClientException {

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("website_url", websiteURL);
        Object response = executeRequest(BRANDING_AI_GENERATE_ENDPOINT, HttpPost.class, requestBody);
        return ((Map<String, String>) response).get("operation_id");
    }

    /**
     * This method will connect to the BrandingAI microservice and get the status of the branding preference generation.
     *
     * @param operationId Operation ID of the branding preference generation.
     * @return Status of the branding preference generation.
     * @throws BrandingAIServerException When errors occurs from the AI service.
     * @throws BrandingAIClientException When invalid request is sent to the AI service.
     */
    @Override
    public Object getBrandingPreferenceGenerationStatus(String operationId) throws BrandingAIServerException,
            BrandingAIClientException {

        return executeRequest(BRANDING_AI_STATUS_ENDPOINT + "/" + operationId, HttpGet.class, null);
    }

    /**
     * This method will connect to the BrandingAI microservice and get the result of the branding preference generation.
     *
     * @param operationId Operation ID of the branding preference generation.
     * @return Result of the branding preference generation.
     * @throws BrandingAIServerException When errors occurs from the AI service.
     * @throws BrandingAIClientException When invalid request is sent to the AI service.
     */
    @Override
    public Object getBrandingPreferenceGenerationResult(String operationId) throws BrandingAIServerException,
            BrandingAIClientException {

        return executeRequest(BRANDING_AI_RESULT_ENDPOINT + "/" + operationId, HttpGet.class, null);
    }


    private Object executeRequest(String endpoint, Class<? extends HttpUriRequest> requestType, Object requestBody)
            throws BrandingAIServerException, BrandingAIClientException {

        String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();

        try (CloseableHttpAsyncClient client = HttpAsyncClients.createDefault()) {
            client.start();
            String accessToken = BrandingAIAccessTokenService.getInstance().getAccessToken(false);
            String orgName = BrandingAIAccessTokenService.getInstance().getClientId();

            HttpUriRequest request = createRequest(BRANDING_AI_ENDPOINT + "/t/" + orgName + endpoint, requestType,
                    accessToken, requestBody);
            HttpResponseWrapper brandingAIServiceResponse = executeRequestWithRetry(client, request);

            int statusCode = brandingAIServiceResponse.getStatusCode();
            String responseBody = brandingAIServiceResponse.getResponseBody();

            if (statusCode >= 400) {
                handleErrorResponse(statusCode, responseBody, tenantDomain);
            }
            return convertJsonStringToObject(responseBody);
        } catch (IOException | InterruptedException | ExecutionException e) {
            throw new BrandingAIServerException("An error occurred while connecting to the AI Branding Service.",
                    ERROR_WHILE_CONNECTING_TO_BRANDING_AI_SERVICE.getCode(), e);
        }
    }

    /**
     * Create a HTTP request with the given parameters. This method will initialize the request with the access token
     * to the BrandingAI microservice.
     *
     * @param url         URL of the request (The endpoint with the path).
     * @param requestType Type of the request (GET or POST).
     * @param accessToken Access token to the BrandingAI microservice.
     * @param requestBody Request body of the request if it is a POST request. Otherwise, null.
     * @return HTTP request with the given parameters.
     * @throws IOException When an error occurs while creating the request.
     */
    private HttpUriRequest createRequest(String url, Class<? extends HttpUriRequest> requestType, String accessToken,
                                         Object requestBody)
            throws IOException {

        HttpUriRequest request;
        if (requestType == HttpPost.class) {
            HttpPost post = new HttpPost(url);
            if (requestBody != null) {
                post.setEntity(new StringEntity(objectMapper.writeValueAsString(requestBody)));
            }
            request = post;
        } else if (requestType == HttpGet.class) {
            request = new HttpGet(url);
        } else {
            throw new IllegalArgumentException("Unsupported request type: " + requestType.getName());
        }

        request.setHeader("Authorization", "Bearer " + accessToken);
        request.setHeader("Content-Type", "application/json");
        return request;
    }

    /**
     * Execute the given request with the given client. If the request fails with an unauthorized error, this method
     * will retry the request with a renewed access token.
     *
     * @param client  HTTP client to execute the request.
     * @param request Request to be executed.
     * @return HTTP response of the request.
     * @throws InterruptedException      When an error occurs while executing the request.
     * @throws ExecutionException        When an error occurs while executing the request.
     * @throws IOException               When an error occurs while executing the request.
     * @throws BrandingAIServerException When an error occurs while executing the request.
     */
    private HttpResponseWrapper executeRequestWithRetry(CloseableHttpAsyncClient client, HttpUriRequest request)
            throws InterruptedException, ExecutionException, IOException, BrandingAIServerException {

        HttpResponseWrapper response = HttpClientHelper.executeRequest(client, request);

        if (response.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
            String newAccessToken = BrandingAIAccessTokenService.getInstance().getAccessToken(true);
            if (newAccessToken == null) {
                throw new BrandingAIServerException("Failed to renew access token.",
                        ERROR_RETRIEVING_ACCESS_TOKEN.getCode());
            }
            request.setHeader("Authorization", "Bearer " + newAccessToken);
            response = HttpClientHelper.executeRequest(client, request);
        }

        return response;
    }

    private void handleErrorResponse(int statusCode, String responseBody, String tenantDomain)
            throws BrandingAIServerException, BrandingAIClientException {

        if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
            throw new BrandingAIServerException("Failed to access AI service with renewed access token for " +
                    "the tenant domain: " + tenantDomain,
                    UNABLE_TO_ACCESS_AI_SERVICE_WITH_RENEW_ACCESS_TOKEN.getCode());
        } else if (statusCode >= 400 && statusCode < 500) {
            throw new BrandingAIClientException(new HttpResponseWrapper(statusCode, responseBody),
                    "Client error occurred from tenant: " + tenantDomain + " with status code: '" + statusCode
                            + "' while generating branding preference.",
                    "1000");
        } else if (statusCode >= 500) {
            throw new BrandingAIServerException(new HttpResponseWrapper(statusCode, responseBody),
                    "Server error occurred from tenant: " + tenantDomain + " with status code: '" + statusCode
                            + "' while generating branding preference.",
                    "2000");
        }
    }

    private Object convertJsonStringToObject(String jsonString) throws BrandingAIServerException {

        try {
            return objectMapper.readValue(jsonString, Object.class);
        } catch (IOException e) {
            throw new BrandingAIServerException("Error occurred while parsing the JSON response from the AI service.",
                    ERROR_WHILE_GENERATING_BRANDING_PREFERENCE.getCode(), e);
        }
    }

    /**
     * Wrapper class to hold the HTTP response status code and the response body.
     */
    public static class HttpResponseWrapper {

        private final int statusCode;
        private final String responseBody;

        public HttpResponseWrapper(int statusCode, String responseBody) {

            this.statusCode = statusCode;
            this.responseBody = responseBody;
        }

        public int getStatusCode() {

            return statusCode;
        }

        public String getResponseBody() {

            return responseBody;
        }
    }

    /**
     * Helper class to execute HTTP requests.
     */
    public static class HttpClientHelper {

        public static HttpResponseWrapper executeRequest(CloseableHttpAsyncClient client, HttpUriRequest httpRequest)
                throws InterruptedException, ExecutionException, IOException {

            Future<HttpResponse> apiResponse = client.execute(httpRequest, new FutureCallback<HttpResponse>() {
                @Override
                public void completed(HttpResponse response) {

                    LOG.info("API request completed with status code: " + response.getStatusLine().getStatusCode());
                }

                @Override
                public void failed(Exception e) {

                    LOG.error("API request failed: " + e.getMessage(), e);
                }

                @Override
                public void cancelled() {

                    LOG.warn("API request was cancelled");
                }
            });

            HttpResponse httpResponse = apiResponse.get(); // Wait for the response to be available.
            int status = httpResponse.getStatusLine().getStatusCode();
            String response = EntityUtils.toString(httpResponse.getEntity());
            return new HttpResponseWrapper(status, response);
        }
    }
}
