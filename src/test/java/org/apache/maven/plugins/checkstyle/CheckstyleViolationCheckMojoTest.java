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
package org.apache.maven.plugins.checkstyle;

import java.io.File;
import java.util.Arrays;

import org.apache.maven.model.Build;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Edwin Punzalan
 *
 */
public class CheckstyleViolationCheckMojoTest extends AbstractMojoTestCase {

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void testDefaultConfig() throws Exception {
        File pluginXmlFile = new File(getBasedir(), "src/test/resources/plugin-configs/check-plugin-config.xml");

        CheckstyleViolationCheckMojo mojo = (CheckstyleViolationCheckMojo) lookupMojo("check", pluginXmlFile);

        // mojo setup

        setVariableValueToObject((Mojo) mojo, "project", new MavenProjectStub() {

            public File getFile() {
                return new File(getBasedir(), "target/classes");
            }

            public Build getBuild() {
                return new Build() {
                    private static final long serialVersionUID = -743084937617131258L;

                    public String getDirectory() {
                        return getBasedir() + "/target/classes";
                    }
                };
            }
        });

        setVariableValueToObject((Mojo) mojo, "configLocation", "sun_checks.xml");
        setVariableValueToObject((Mojo) mojo, "cacheFile", getBasedir() + "/target/classes/checkstyle-cachefile");
        setVariableValueToObject(
                (Mojo) mojo,
                "sourceDirectories",
                Arrays.asList(
                        getBasedir() + "/src/test/plugin-configs/src")); // new File( getBasedir() + "/target" ) );
        setVariableValueToObject((Mojo) mojo, "inputEncoding", "UTF-8");
        setVariableValueToObject((Mojo) mojo, "skipExec", Boolean.TRUE);

        Assertions.assertNotNull(mojo, "Mojo not found.");

        Assertions.assertNotNull(mojo.project, "project null.");

        try {
            mojo.execute();

            fail("Must throw an exception on violations");
        } catch (MojoFailureException e) {
            // expected
        }
    }

    @Test
    public void testInvalidFormatWithSkipExec() throws Exception {
        File pluginXmlFile = new File(getBasedir(), "src/test/resources/plugin-configs/check-plugin-config.xml");

        Mojo mojo = lookupMojo("check", pluginXmlFile);

        Assertions.assertNotNull(mojo, "Mojo not found.");

        // mojo setup

        setVariableValueToObject(mojo, "project", new MavenProjectStub() {

            public File getFile() {
                return new File(getBasedir(), "target/classes");
            }

            public Build getBuild() {
                return new Build() {
                    private static final long serialVersionUID = -743084937617131258L;

                    public String getDirectory() {
                        return getBasedir() + "/target/classes";
                    }
                };
            }
        });

        setVariableValueToObject(mojo, "configLocation", "sun_checks.xml");
        setVariableValueToObject(mojo, "cacheFile", getBasedir() + "/target/classes/checkstyle-cachefile");
        setVariableValueToObject(
                mojo,
                "sourceDirectories",
                Arrays.asList(
                        getBasedir() + "/src/test/plugin-configs/src")); // new File( getBasedir() + "/target" ) );
        setVariableValueToObject(mojo, "inputEncoding", "UTF-8");
        setVariableValueToObject(mojo, "skipExec", Boolean.TRUE);

        setVariableValueToObject(mojo, "outputFileFormat", "plain");

        try {
            mojo.execute();

            fail("Must throw an exception invalid format: plain");
        } catch (MojoExecutionException e) {
            // expected
        }
    }

    @Test
    public void testNoOutputFile() throws Exception {
        File pluginXmlFile = new File(getBasedir(), "src/test/resources/plugin-configs/check-plugin-config.xml");

        Mojo mojo = lookupMojo("check", pluginXmlFile);

        Assertions.assertNotNull(mojo, "Mojo not found.");

        // mojo setup

        setVariableValueToObject(mojo, "project", new MavenProjectStub() {

            public File getFile() {
                return new File(getBasedir(), "target/classes");
            }

            public Build getBuild() {
                return new Build() {
                    private static final long serialVersionUID = -743084937617131258L;

                    public String getDirectory() {
                        return getBasedir() + "/target/classes";
                    }
                };
            }
        });

        setVariableValueToObject(mojo, "configLocation", "sun_checks.xml");
        setVariableValueToObject(mojo, "cacheFile", getBasedir() + "/target/classes/checkstyle-cachefile");
        setVariableValueToObject(
                mojo,
                "sourceDirectories",
                Arrays.asList(
                        getBasedir() + "/src/test/plugin-configs/src")); // new File( getBasedir() + "/target" ) );
        setVariableValueToObject(mojo, "inputEncoding", "UTF-8");
        setVariableValueToObject(mojo, "skipExec", Boolean.TRUE);

        setVariableValueToObject(mojo, "outputFile", new File("target/NoSuchFile.xml"));

        mojo.execute();
    }

    @Test
    public void testPlainOutputFileFailOnError() throws Exception {
        try {
            File pluginXmlFile =
                    new File(getBasedir(), "src/test/resources/plugin-configs/check-plugin-plain-output.xml");

            Mojo mojo = lookupMojo("check", pluginXmlFile);

            Assertions.assertNotNull(mojo, "Mojo not found.");

            PluginDescriptor descriptorStub = new PluginDescriptor();
            descriptorStub.setGroupId("org.apache.maven.plugins");
            descriptorStub.setArtifactId("maven-checkstyle-plugin");
            setVariableValueToObject(mojo, "plugin", descriptorStub);

            setVariableValueToObject(mojo, "failsOnError", true);

            mojo.execute();

            fail("Must fail on violations");
        } catch (MojoExecutionException e) {
            // expected
        }
    }

    @Test
    public void testPlainOutputFile() throws Exception {
        File pluginXmlFile = new File(getBasedir(), "src/test/resources/plugin-configs/check-plugin-plain-output.xml");

        Mojo mojo = lookupMojo("check", pluginXmlFile);

        Assertions.assertNotNull(mojo, "Mojo not found.");

        PluginDescriptor descriptorStub = new PluginDescriptor();
        descriptorStub.setGroupId("org.apache.maven.plugins");
        descriptorStub.setArtifactId("maven-checkstyle-plugin");
        setVariableValueToObject(mojo, "plugin", descriptorStub);

        setVariableValueToObject(mojo, "failsOnError", false);

        mojo.execute();
    }

    @Test
    public void testNoFail() throws Exception {
        File pluginXmlFile = new File(getBasedir(), "src/test/resources/plugin-configs/check-plugin-config.xml");

        Mojo mojo = lookupMojo("check", pluginXmlFile);

        Assertions.assertNotNull(mojo, "Mojo not found.");

        // mojo setup

        setVariableValueToObject(mojo, "project", new MavenProjectStub() {

            public File getFile() {
                return new File(getBasedir(), "target/classes");
            }

            public Build getBuild() {
                return new Build() {
                    private static final long serialVersionUID = -743084937617131258L;

                    public String getDirectory() {
                        return getBasedir() + "/target/classes";
                    }
                };
            }
        });

        setVariableValueToObject(mojo, "configLocation", "sun_checks.xml");
        setVariableValueToObject(mojo, "cacheFile", getBasedir() + "/target/classes/checkstyle-cachefile");
        setVariableValueToObject(
                mojo,
                "sourceDirectories",
                Arrays.asList(
                        getBasedir() + "/src/test/plugin-configs/src")); // new File( getBasedir() + "/target" ) );
        setVariableValueToObject(mojo, "inputEncoding", "UTF-8");
        setVariableValueToObject(mojo, "skipExec", Boolean.TRUE);

        setVariableValueToObject(mojo, "failOnViolation", Boolean.FALSE);

        mojo.execute();
    }
}
