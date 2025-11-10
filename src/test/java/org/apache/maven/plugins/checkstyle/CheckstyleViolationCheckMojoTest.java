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

import javax.inject.Inject;

import org.apache.maven.api.di.Provides;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Edwin Punzalan
 *
 */
@MojoTest
public class CheckstyleViolationCheckMojoTest {

    @Inject
    private MavenProject project;

    @Inject
    private PluginDescriptor plugin;

    @InjectMojo(goal = "check", pom = "src/test/resources/plugin-configs/check-plugin-config.xml")
    @MojoParameter(name = "configLocation", value = "sun_checks.xml")
    @MojoParameter(name = "cacheFile", value = "/target/classes/checkstyle-cachefile")
    @MojoParameter(name = "sourceDirectories", value = "/src/test/plugin-configs/src")
    @MojoParameter(name = "inputEncoding", value = "UTF-8")
    @MojoParameter(name = "skipExec", value = "true")
    @Test
    public void testDefaultConfig(CheckstyleViolationCheckMojo mojo) throws Exception {
        try {
            mojo.execute();

            fail("Must throw an exception on violations");
        } catch (MojoFailureException e) {
            // expected
        }
    }

    @InjectMojo(goal = "check", pom = "src/test/resources/plugin-configs/check-plugin-config.xml")
    @MojoParameter(name = "configLocation", value = "sun_checks.xml")
    @MojoParameter(name = "cacheFile", value = "/target/classes/checkstyle-cachefile")
    @MojoParameter(name = "sourceDirectories", value = "/src/test/plugin-configs/src")
    @MojoParameter(name = "inputEncoding", value = "UTF-8")
    @MojoParameter(name = "skipExec", value = "true")
    @MojoParameter(name = "outputFileFormat", value = "plain")
    @Test
    public void testInvalidFormatWithSkipExec(CheckstyleViolationCheckMojo mojo) throws Exception {
        try {
            mojo.execute();

            fail("Must throw an exception invalid format: plain");
        } catch (MojoExecutionException e) {
            // expected
        }
    }

    @InjectMojo(goal = "check", pom = "src/test/resources/plugin-configs/check-plugin-config.xml")
    @MojoParameter(name = "configLocation", value = "sun_checks.xml")
    @MojoParameter(name = "cacheFile", value = "/target/classes/checkstyle-cachefile")
    @MojoParameter(name = "sourceDirectories", value = "/src/test/plugin-configs/src")
    @MojoParameter(name = "inputEncoding", value = "UTF-8")
    @MojoParameter(name = "skipExec", value = "true")
    @MojoParameter(name = "outputFile", value = "target/NoSuchFile.xml")
    @Test
    public void testNoOutputFile(CheckstyleViolationCheckMojo mojo) throws Exception {
        mojo.execute();
    }

    @InjectMojo(goal = "check", pom = "src/test/resources/plugin-configs/check-plugin-plain-output.xml")
    @MojoParameter(name = "failsOnError", value = "true")
    @Test
    public void testPlainOutputFileFailOnError(CheckstyleViolationCheckMojo mojo) throws Exception {
        try {
            mojo.execute();

            fail("Must fail on violations");
        } catch (MojoExecutionException e) {
            // expected
        }
    }

    @Provides
    public PluginDescriptor getPluginDescriptor() {
        PluginDescriptor descriptorStub = new PluginDescriptor();
        descriptorStub.setGroupId("org.apache.maven.plugins");
        descriptorStub.setArtifactId("maven-checkstyle-plugin");
        return descriptorStub;
    }

    @InjectMojo(goal = "check", pom = "src/test/resources/plugin-configs/check-plugin-plain-output.xml")
    @MojoParameter(name = "failsOnError", value = "false")
    @MojoParameter(name = "failOnViolation", value = "false")
    @Test
    public void testPlainOutputFile(CheckstyleViolationCheckMojo mojo) throws Exception {
        mojo.execute();
    }

    @InjectMojo(goal = "check", pom = "src/test/resources/plugin-configs/check-plugin-config.xml")
    @MojoParameter(name = "configLocation", value = "sun_checks.xml")
    @MojoParameter(name = "cacheFile", value = "/target/classes/checkstyle-cachefile")
    @MojoParameter(name = "sourceDirectories", value = "/src/test/plugin-configs/src")
    @MojoParameter(name = "inputEncoding", value = "UTF-8")
    @MojoParameter(name = "skipExec", value = "true")
    @MojoParameter(name = "failOnViolation", value = "false")
    @Test
    public void testNoFail(CheckstyleViolationCheckMojo mojo) throws Exception {
        mojo.execute();
    }
}
