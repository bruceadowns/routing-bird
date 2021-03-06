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
package com.jivesoftware.os.routing.bird.health;

import com.jivesoftware.os.routing.bird.health.api.ResettableHealthCheck;
import com.jivesoftware.os.routing.bird.health.api.ScheduledHealthCheck;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class HealthCheckService {

    private static class NamedThreadFactory implements ThreadFactory {

        private final ThreadGroup g;
        private final String namePrefix;
        private final AtomicLong threadNumber;

        public NamedThreadFactory(ThreadGroup g, String name) {
            this.g = g;
            namePrefix = name + "-";
            threadNumber = new AtomicLong();
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(g, r,
                namePrefix + threadNumber.getAndIncrement(),
                0);
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }

    private final ScheduledExecutorService scheduledHealthChecks = Executors.newScheduledThreadPool(12,
        new NamedThreadFactory(Thread.currentThread().getThreadGroup(),
            "health-checkers")); // TODO clean up yuck.

    private final List<HealthCheckWrapper> healthChecks = new ArrayList<>();

    public void addHealthCheck(List<HealthCheck> toAdd) {
        for (HealthCheck add : toAdd) {
            HealthCheckWrapper wrapper = new HealthCheckWrapper(add);
            synchronized (healthChecks) {
                healthChecks.add(wrapper);
            }
            if (add instanceof ScheduledHealthCheck) {
                ScheduledHealthCheck scheduledHealthCheck = (ScheduledHealthCheck) add;
                long checkIntervalInMillis = scheduledHealthCheck.getCheckIntervalInMillis();
                wrapper.scheduledFuture = scheduledHealthChecks.scheduleWithFixedDelay(scheduledHealthCheck, 0, checkIntervalInMillis, TimeUnit.MILLISECONDS);
            }
        }
    }

    public void removeHealthCheck(List<HealthCheck> toRemove) {
        for (HealthCheck remove : toRemove) {
            // brute force removal, hopefully used sparingly (or never)
            synchronized (healthChecks) {
                Iterator<HealthCheckWrapper> iter = healthChecks.iterator();
                while (iter.hasNext()) {
                    HealthCheckWrapper wrapper = iter.next();
                    if (wrapper.healthCheck == remove) {
                        wrapper.cancel();
                        iter.remove();
                        // don't break, in case the health check was added multiple times
                    }
                }
            }
        }
    }

    public List<HealthCheckResponse> checkHealth() throws Exception {
        List<HealthCheckResponse> response = new ArrayList<>();
        synchronized (healthChecks) {
            for (HealthCheckWrapper wrapper : healthChecks) {
                HealthCheckResponse healthCheckResponse = wrapper.healthCheck.checkHealth();
                if (-Double.MAX_VALUE != healthCheckResponse.getHealth()) {
                    response.add(healthCheckResponse);
                }
            }
        }
        return response;
    }

    public void resetHealthChecks() {
        synchronized (healthChecks) {
            for (HealthCheckWrapper wrapper : healthChecks) {
                if (wrapper.healthCheck instanceof ResettableHealthCheck) {
                    ((ResettableHealthCheck) wrapper.healthCheck).reset();
                }
            }
        }
    }

    private static class HealthCheckWrapper {

        private final HealthCheck healthCheck;

        private ScheduledFuture<?> scheduledFuture;

        private HealthCheckWrapper(HealthCheck healthCheck) {
            this.healthCheck = healthCheck;
        }

        private void cancel() {
            if (scheduledFuture != null) {
                scheduledFuture.cancel(true);
            }
        }
    }

}
