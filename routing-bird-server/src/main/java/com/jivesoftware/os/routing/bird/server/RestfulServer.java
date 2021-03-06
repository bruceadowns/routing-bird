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
package com.jivesoftware.os.routing.bird.server;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

public class RestfulServer {

    // Copying the default behavior inside jetty
    private static final int ACCEPTORS = Math.max(1, (Runtime.getRuntime().availableProcessors()) / 2);
    private static final int SELECTORS = Runtime.getRuntime().availableProcessors();

    private static final int MIN_THREADS = 8;
    private static final int IDLE_TIMEOUT = 60000;

    private final Server server;
    private final QueuedThreadPool queuedThreadPool;
    private final String applicationName;
    private final ContextHandlerCollection handlers;

    public RestfulServer(boolean loopback,
        int port,
        String applicationName,
        boolean sslEnabled,
        String keyStoreAlias,
        String keyStorePassword,
        String keyStorePath,
        int maxNumberOfThreads,
        int maxQueuedRequests) {

        this.applicationName = applicationName;
        int maxThreads = maxNumberOfThreads + ACCEPTORS + SELECTORS;
        BlockingArrayQueue<Runnable> queue = new BlockingArrayQueue<>(MIN_THREADS, MIN_THREADS, maxQueuedRequests);
        this.queuedThreadPool = new QueuedThreadPool(maxThreads, MIN_THREADS, IDLE_TIMEOUT, queue);
        this.server = new Server(queuedThreadPool);
        this.handlers = new ContextHandlerCollection();

        server.addEventListener(new MBeanContainer(ManagementFactory.getPlatformMBeanServer()));
        server.setHandler(handlers);

        if (sslEnabled) {
            server.addConnector(makeSslConnector(keyStoreAlias, keyStorePassword, keyStorePath, port));
        } else {
            server.addConnector(makeConnector(loopback, port));
        }
    }

    public int getThreads() {
        return queuedThreadPool.getThreads();
    }

    public int getIdleThreads() {
        return queuedThreadPool.getIdleThreads();
    }

    public int getBusyThreads() {
        return queuedThreadPool.getBusyThreads();
    }

    public int getMaxThreads() {
        return queuedThreadPool.getMaxThreads();
    }

    public boolean isLowOnThreads() {
        return queuedThreadPool.isLowOnThreads();
    }

    private Connector makeConnector(boolean loopback, int port) {

        HttpConfiguration httpConfig = buildHttpConfiguration(port);

        ServerConnector connector = new ServerConnector(server, ACCEPTORS, SELECTORS, new HttpConnectionFactory(httpConfig));
        if (loopback) {
            connector.setHost("127.0.0.1");
        }
        connector.setPort(port);
        connector.setIdleTimeout(30000); // Config
        return connector;
    }

    private Connector makeSslConnector(String keyStoreAlias,
        String keyStorePassword,
        String keyStorePath,
        int port) {

        HttpConfiguration httpConfig = buildHttpConfiguration(port);

        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setCertAlias(keyStoreAlias);
        if (keyStorePath != null) {
            sslContextFactory.setKeyStorePath(keyStorePath);
        }
        if (keyStorePassword != null) {
            sslContextFactory.setKeyStorePassword(keyStorePassword);
        }
        sslContextFactory.setExcludeCipherSuites("SSL_RSA_WITH_DES_CBC_SHA",
            "SSL_DHE_RSA_WITH_DES_CBC_SHA", "SSL_DHE_DSS_WITH_DES_CBC_SHA",
            "SSL_RSA_EXPORT_WITH_RC4_40_MD5",
            "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
            "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA",
            "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA");

        // SSL HTTP Configuration
        HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);
        httpsConfig.addCustomizer(new SecureRequestCustomizer());

        // SSL Connector
        ServerConnector sslConnector = new ServerConnector(server,
            new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
            new HttpConnectionFactory(httpsConfig));
        sslConnector.setPort(port);
        sslConnector.setIdleTimeout(30000); // Config
        return sslConnector;
    }

    private HttpConfiguration buildHttpConfiguration(int port) {
        // HTTP Configuration
        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setSecureScheme("https");
        httpConfig.setSecurePort(port);
        httpConfig.setOutputBufferSize(32768);
        httpConfig.setRequestHeaderSize(8192);
        httpConfig.setResponseHeaderSize(8192);
        httpConfig.setSendServerVersion(true);
        httpConfig.setSendDateHeader(false);
        httpConfig.setBlockingTimeout(30000); // Config
        // httpConfig.addCustomizer(new ForwardedRequestCustomizer());
        return httpConfig;
    }

    public void addContextHandler(String context, HasServletContextHandler contextHandler) {
        if (context == null || contextHandler == null) { // allows nulls to be ignored which works better with a chaining builder pattern
            return;
        }
        handlers.addHandler(contextHandler.getHandler(server, context, applicationName));
    }

    public void addClasspathResource(String path) throws Exception {
        addResourcesDir(path, "static");
    }

    private void addResourcesDir(String path, String dir) throws IOException, URISyntaxException {
        Resource newResource = Resource.newResource(this.getClass().getResource(path + "/" + dir).toURI());
        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setBaseResource(newResource);
        resourceHandler.setCacheControl("public, max-age=31536000");
        ContextHandler ctx = new ContextHandler("/" + dir);
        ctx.setHandler(resourceHandler);
        handlers.addHandler(ctx);
    }

    public void start() throws Exception {
        server.start();
    }

    public void stop() throws Exception {
        server.stop();
    }
}
