/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.plugins.checkstyle.exec;

import javax.inject.Inject;

import java.lang.reflect.Method;

import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@MojoTest
public class CheckstyleExecutorTest {
    @Inject
    private CheckstyleExecutor executor;

    @Test
    public void testDefaultConfig() throws Exception {
        assertNotNull(executor, "project null.");
    }

    @Test
    public void testAddsMissingProperty() throws Exception {
        DefaultConfiguration config = new DefaultConfiguration("Checker");

        addAttributeIfNotExists(config, "charset", "UTF-8");

        assertEquals("UTF-8", config.getProperty("charset"));
    }

    @Test
    public void testKeepsExistingProperty() throws Exception {
        DefaultConfiguration config = new DefaultConfiguration("Checker");
        config.addProperty("charset", "ISO-8859-1");

        addAttributeIfNotExists(config, "charset", "UTF-8");

        assertEquals("ISO-8859-1", config.getProperty("charset"));
    }

    private void addAttributeIfNotExists(DefaultConfiguration config, String name, String value) throws Exception {
        Method method = DefaultCheckstyleExecutor.class.getDeclaredMethod(
                "addAttributeIfNotExists", DefaultConfiguration.class, String.class, String.class);
        method.setAccessible(true);
        method.invoke(executor, config, name, value);
    }
}
