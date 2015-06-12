/*
 * Copyright 2015 Jive Software.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jivesoftware.os.routing.bird.endpoints.base;

import java.util.List;

/**
 *
 * @author jonathan.colt
 */
public class HasUI {

    public List<UI> uis;

    public HasUI() {
    }

    public HasUI(List<UI> uis) {
        this.uis = uis;
    }

    public static class UI {

        public String name;
        public String portName;
        public String url;

        public UI() {
        }

        public UI(String name, String portName, String url) {
            this.name = name;
            this.portName = portName;
            this.url = url;
        }
    }
}
