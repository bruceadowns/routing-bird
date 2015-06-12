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

import com.jivesoftware.os.routing.bird.server.util.Resource;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;

public class StaticEndpoint implements HasServletContextHandler {

    private Resource resource;

    public StaticEndpoint(Resource resource) {
        this.resource = resource;
    }

    @Override
    public Handler getHandler(Server server, String context, String applicationName) {
        ContextHandler handler = new ContextHandler();
        handler.setContextPath(context);
        handler.setHandler(resource.getResourceHandler());
        handler.setDisplayName(applicationName);
        return handler;
    }
}
