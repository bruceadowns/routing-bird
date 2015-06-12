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

import java.util.Map.Entry;

public class CounterStatusEntry implements Entry<String, Long> {
    private final String key;
    private final Long value;

    public CounterStatusEntry(String key, Long value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public Long getValue() {
        return value;
    }

    @Override
    public Long setValue(Long value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
