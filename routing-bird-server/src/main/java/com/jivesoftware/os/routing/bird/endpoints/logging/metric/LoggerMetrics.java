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
package com.jivesoftware.os.routing.bird.endpoints.logging.metric;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author jonathan
 */
public class LoggerMetrics {

    public final ConcurrentHashMap<String, ServiceStatusNode> nodes = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<String, Long> leafs = new ConcurrentHashMap<>();

    public interface MetricsStream {
        void callback(Entry<String, Long> e) throws Exception;
    }

    @JsonIgnore
    public void getAll(MetricsStream stream) throws Exception {
        for (Entry<String, Long> e : leafs.entrySet()) {
            stream.callback(e);
        }
        for (Entry<String, ServiceStatusNode> e : nodes.entrySet()) {
            e.getValue().callback(e.getKey(), stream);
        }
    }

    @JsonIgnore
    public Long get(final String key) {
        if (key == null || key.length() == 0) {
            Thread.dumpStack();
            return null;
        }
        String[] segments = key.split(">");
        Long got;
        if (segments.length == 1) {
            got = leafs.get(segments[0]);
            if (got != null) {
                return got;
            }
        } else {
            ServiceStatusNode node = null;
            for (int i = 0; i < segments.length - 1; i++) {
                if (node == null) {
                    node = nodes.get(segments[i]);
                    if (node == null) {
                        break;
                    }
                } else {
                    ServiceStatusNode next = node.getNode(segments[i]);
                    if (next == null) {
                        break;
                    }
                    node = next;
                }
            }
            if (node != null) {
                got = node.getLeaf(segments[segments.length - 1]);
                if (got != null) {
                    return got;
                }
            }
        }
        return 0L;

    }

    @JsonIgnore
    public void put(String key, Long value) {
        if (key == null || value == null) {
            return;
        }
        String[] segments = key.split(">");
        if (segments.length == 0) {
            return;
        }
        if (segments.length == 1) {
            leafs.put(segments[0], value);
        } else {
            ServiceStatusNode node = null;
            for (int i = 0; i < segments.length - 1; i++) {
                if (node == null) {
                    node = nodes.get(segments[i]);
                    if (node == null) {
                        node = new ServiceStatusNode();
                        ServiceStatusNode had = nodes.putIfAbsent(segments[i], node);
                        if (had != null) {
                            node = had;
                        }
                    }
                } else {
                    node = node.putNode(segments[i]);
                }
            }
            if (node != null) {
                node.putLeafs(segments[segments.length - 1], value);
            }
        }

    }
}
