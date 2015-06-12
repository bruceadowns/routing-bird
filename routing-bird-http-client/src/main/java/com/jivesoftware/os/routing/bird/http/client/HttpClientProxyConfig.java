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

/**
 *
 */
public class HttpClientProxyConfig implements HttpClientConfiguration {

    private final String proxyHost;
    private final int proxyPort;
    private final String proxyUsername;
    private final String proxyPassword;

    private HttpClientProxyConfig(
        String proxyHost, int proxyPort, String proxyUsername, String proxyPassword) {
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.proxyUsername = proxyUsername;
        this.proxyPassword = proxyPassword;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public String getProxyUsername() {
        return proxyUsername;
    }

    public String getProxyPassword() {
        return proxyPassword;
    }

    @Override
    public String toString() {
        return "HttpClientConfig{"
            + ", proxyHost=" + proxyHost
            + ", proxyPort=" + proxyPort
            + ", proxyUsername=" + proxyUsername
            + ", proxyPassword=" + "*******" // dont expose password in logs
            + '}';
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    final public static class Builder {

        private String proxyHost = "";
        private int proxyPort = -1;
        private String proxyUsername = "";
        private String proxyPassword = "";

        private Builder() {
        }

        public Builder setProxyConfiguration(String proxyHost, int proxyPort, String proxyUsername, String proxyPassword) {
            this.proxyHost = proxyHost;
            this.proxyPort = proxyPort;
            this.proxyUsername = proxyUsername;
            this.proxyPassword = proxyPassword;
            return this;
        }

        public HttpClientProxyConfig build() {
            return new HttpClientProxyConfig(proxyHost, proxyPort, proxyUsername, proxyPassword);
        }
    }
}
