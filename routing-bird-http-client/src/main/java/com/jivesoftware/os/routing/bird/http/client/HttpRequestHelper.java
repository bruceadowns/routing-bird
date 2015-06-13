/*
 * Copyright 2013 Jive Software, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.jivesoftware.os.routing.bird.http.client;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import java.io.IOException;
import java.nio.charset.Charset;

public class HttpRequestHelper {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger(HttpRequestHelper.class);
    public static final int DEFAULT_STREAM_TIMEOUT_MILLIS = 5000;

    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    private static final byte[] EMPTY_RESPONSE = new byte[0];
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    public HttpRequestHelper(HttpClient httpClient, ObjectMapper mapper) {
        this.httpClient = httpClient;
        this.mapper = mapper;
    }

    /**
     * Sends the request to the server and returns the deserialized results.
     * <p>
     * If the response body is empty, and the status code is successful, the client returns an empty (but valid) result.
     *
     * @param endpointUrl path to the REST service
     * @param resultClass type of the result class
     * @param emptyResult an instance an empty result.
     * @return the result
     * @throws RuntimeException on marshalling, request, or deserialization failure
     */
    public <T> T executeGetRequest(String endpointUrl, Class<T> resultClass, T emptyResult) {

        byte[] responseBody = executeGetJson(httpClient, endpointUrl);

        if (responseBody.length == 0) {
            LOG.warn("Received empty response from http call. The endpoint posted to was " + endpointUrl + "\".");
            return emptyResult;
        }

        return extractResultFromResponse(responseBody, resultClass);
    }

    public <T> T executeDeleteRequest(String endpointUrl, Class<T> resultClass, T emptyResult) {
        byte[] responseBody = executeDeleteJson(httpClient, endpointUrl);
        if (responseBody.length == 0) {
            LOG.warn("Received empty response from http call. The endpoint posted to was " + endpointUrl + "\".");
            return emptyResult;
        } else {
            return this.extractResultFromResponse(responseBody, resultClass);
        }
    }

    /**
     * Sends the request to the server and returns the deserialized results.
     * <p>
     * If the response body is empty, and the status code is successful, the client returns an empty (but valid) result.
     *
     * @param requestParamsObject request object
     * @param endpointUrl         path to the REST service
     * @param resultClass         type of the result class
     * @param emptyResult         an instance an empty result.
     * @return the result
     * @throws RuntimeException on marshalling, request, or deserialization failure
     */
    public <T> T executeRequest(Object requestParamsObject, String endpointUrl, Class<T> resultClass, T emptyResult) {

        String postEntity;
        try {
            postEntity = mapper.writeValueAsString(requestParamsObject);
        } catch (IOException e) {
            throw new RuntimeException("Error serializing request parameters object to a string.  Object "
                + "was " + requestParamsObject, e);
        }

        byte[] responseBody = executePostJson(httpClient, endpointUrl, postEntity);

        if (responseBody.length == 0) {
            LOG.warn("Received empty response from http call.  Posted request body was: " + postEntity);
            return emptyResult;
        }

        return extractResultFromResponse(responseBody, resultClass);
    }

    /**
     * Sends the request to the server and returns the deserialized results.
     * <p>
     * If the response body is empty, and the status code is successful, the client returns an empty (but valid) result.
     *
     * @param requestParamsObject request object
     * @param endpointUrl         path to the REST service
     * @param parametrized        type of the result class
     * @param parameterClasses    parameters of the result class
     * @param emptyResult         an instance an empty result.
     * @return the result
     * @throws RuntimeException on marshalling, request, or deserialization failure
     */
    public <T> T executeRequest(Object requestParamsObject, String endpointUrl, Class<T> parametrized, Class<?>[] parameterClasses, T emptyResult) {

        String postEntity;
        try {
            postEntity = mapper.writeValueAsString(requestParamsObject);
        } catch (IOException e) {
            throw new RuntimeException("Error serializing request parameters object to a string.  Object "
                + "was " + requestParamsObject, e);
        }

        byte[] responseBody = executePostJson(httpClient, endpointUrl, postEntity);

        if (responseBody.length == 0) {
            LOG.warn("Received empty response from http call.  Posted request body was: " + postEntity);
            return emptyResult;
        }
        JavaType resultType = mapper.getTypeFactory().constructParametricType(parametrized, parameterClasses);
        return extractResultFromResponse(responseBody, resultType);
    }

    public HttpStreamResponse executeStreamingPostRequest(Object requestParamsObject, String endpointUrl) throws HttpClientException {

        String postEntity;
        try {
            postEntity = mapper.writeValueAsString(requestParamsObject);
        } catch (IOException e) {
            throw new RuntimeException("Error serializing request parameters object to a string.  Object "
                + "was " + requestParamsObject, e);
        }

        return httpClient.streamingPost(endpointUrl, postEntity, null);
    }

    private byte[] executeGetJson(HttpClient httpClient, String endpointUrl) {
        HttpResponse response;
        try {
            response = httpClient.get(endpointUrl, null);
        } catch (HttpClientException e) {
            throw new RuntimeException("Error posting query request to server.  The endpoint posted to was \"" + endpointUrl + "\".", e);
        }

        byte[] responseBody = response.getResponseBody();

        if (responseBody == null) {
            responseBody = EMPTY_RESPONSE;
        }

        if (!isSuccessStatusCode(response.getStatusCode())) {
            throw new NonSuccessStatusCodeException(response.getStatusCode(), "Received non success status code (" + response.getStatusCode() + ") "
                + "from the server.  The reason phrase on the response was \"" + response.getStatusReasonPhrase() + "\" "
                + "and the body of the response was \"" + new String(responseBody, UTF_8) + "\".");
        }

        return responseBody;
    }

    private byte[] executeDeleteJson(HttpClient httpClient, String endpointUrl) {
        HttpResponse response;
        try {
            response = httpClient.delete(endpointUrl, null);
        } catch (HttpClientException var5) {
            throw new RuntimeException("Error posting query request to server.  The endpoint posted to was \"" + endpointUrl + "\".", var5);
        }

        byte[] responseBody = response.getResponseBody();
        if (responseBody == null) {
            responseBody = EMPTY_RESPONSE;
        }

        if (!this.isSuccessStatusCode(response.getStatusCode())) {
            throw new NonSuccessStatusCodeException(response.getStatusCode(),
                "Received non success status code (" + response.getStatusCode() + ") " + "from the server.  The reason phrase on the response was \"" +
                    response.getStatusReasonPhrase() + "\" " + "and the body of the response was \"" + new String(
                    responseBody, UTF_8) + "\".");
        } else {
            return responseBody;
        }
    }

    private byte[] executePostJson(HttpClient httpClient, String endpointUrl, String postEntity) {
        HttpResponse response;
        try {
            response = httpClient.postJson(endpointUrl, postEntity, null);
        } catch (HttpClientException e) {
            throw new RuntimeException("Error posting query request to server.  The entity posted "
                + "was \"" + postEntity + "\" and the endpoint posted to was \"" + endpointUrl + "\".", e);
        }

        byte[] responseBody = response.getResponseBody();

        if (responseBody == null) {
            responseBody = EMPTY_RESPONSE;
        }

        if (!isSuccessStatusCode(response.getStatusCode())) {
            throw new NonSuccessStatusCodeException(response.getStatusCode(), "Received non success status code (" + response.getStatusCode() + ") "
                + "from the server.  The reason phrase on the response was \"" + response.getStatusReasonPhrase() + "\" "
                + "and the body of the response was \"" + new String(responseBody, UTF_8) + "\".");
        }

        return responseBody;
    }

    private <T> T extractResultFromResponse(byte[] responseBody, Class<T> resultClass) {
        T result;
        try {
            result = mapper.readValue(responseBody, 0, responseBody.length, resultClass);
        } catch (Exception e) {
            throw new RuntimeException("Error deserializing response body into result "
                + "object.  Response body was \"" + (responseBody != null ? new String(responseBody, UTF_8) : "null")
                + "\".", e);
        }

        return result;
    }

    private <T> T extractResultFromResponse(byte[] responseBody, JavaType type) {
        T result;
        try {
            result = mapper.readValue(responseBody, 0, responseBody.length, type);
        } catch (IOException e) {
            throw new RuntimeException("Error deserializing response body into result "
                + "object.  Response body was \"" + (responseBody != null ? new String(responseBody, UTF_8) : "null")
                + "\".", e);
        }

        return result;
    }

    private boolean isSuccessStatusCode(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

}
