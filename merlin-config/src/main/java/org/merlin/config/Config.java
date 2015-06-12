/*
 * A High-Level Framework for Application Configuration
 *
 * Copyright 2007 Merlin Hughes / Learning Objects, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.merlin.config;

/**
 * The ancestor of all configuration interfaces. Defines common methods inherited by all configuration interfaces.
 *
 * @author Merlin Hughes
 * @version 0.1, 2007/04/15
 */
public interface Config {

    /**
     * Returns the name that will be prefixed on to all properties except for @AbsoluteProperties
     */
    String name();

    /**
     * Apply the default values of all configuration fields specified in this interface that have defaults.
     */
    void applyDefaults();
}
