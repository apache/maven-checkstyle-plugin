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

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.apache.maven.api.di.Provides;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.checkstyle.exec.CheckstyleExecutor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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

    @Test
    void testProjectRootResourceIncludes(@TempDir Path directory) throws Exception {
        Files.write(directory.resolve("resource.xml"), "<resource/>".getBytes(StandardCharsets.UTF_8));
        Resource resource = new Resource();
        resource.setDirectory(directory.toString());
        resource.addInclude("**/*.xml");
        CheckstyleExecutor executor = mock(CheckstyleExecutor.class);
        resourceMojo(directory, resource, executor, false).execute();
        verify(executor).executeCheckstyle(any());
    }

    @Test
    void testProjectRootTestResourceIncludes(@TempDir Path directory) throws Exception {
        Files.write(directory.resolve("resource.xml"), "<resource/>".getBytes(StandardCharsets.UTF_8));
        Resource resource = new Resource();
        resource.setDirectory(directory.toString());
        resource.addInclude("**/*.xml");
        CheckstyleExecutor executor = mock(CheckstyleExecutor.class);
        resourceMojo(directory, resource, executor, true).execute();
        verify(executor).executeCheckstyle(any());
    }

    @Test
    void testProjectRootResourceExcludes(@TempDir Path directory) throws Exception {
        Files.write(directory.resolve("resource.properties"), "key=value".getBytes(StandardCharsets.UTF_8));
        Resource resource = new Resource();
        resource.setDirectory(directory.toString());
        resource.addExclude("**/*.properties");
        CheckstyleExecutor executor = mock(CheckstyleExecutor.class);
        resourceMojo(directory, resource, executor, false).execute();
        verifyNoInteractions(executor);
    }

    @Test
    void testNonRootResourceIncludesAreNotMerged(@TempDir Path directory) throws Exception {
        Path resources = Files.createDirectory(directory.resolve("resources"));
        Files.write(resources.resolve("resource.xml"), "<resource/>".getBytes(StandardCharsets.UTF_8));
        Resource resource = new Resource();
        resource.setDirectory(resources.toString());
        resource.addInclude("**/*.xml");
        CheckstyleExecutor executor = mock(CheckstyleExecutor.class);
        resourceMojo(directory, resource, executor, false).execute();
        verifyNoInteractions(executor);
    }

    private CheckstyleViolationCheckMojo resourceMojo(
            Path directory, Resource resource, CheckstyleExecutor executor, boolean testResources) throws Exception {
        MavenProject resourceProject = new MavenProject();
        resourceProject.setFile(directory.resolve("pom.xml").toFile());
        resourceProject.setPackaging("pom");
        CheckstyleViolationCheckMojo mojo = new CheckstyleViolationCheckMojo(executor);
        setField(mojo, "project", resourceProject);
        setField(mojo, "plugin", getPluginDescriptor());
        setField(mojo, "sourceDirectories", Collections.emptyList());
        setField(mojo, "resourceIncludes", "**/*.properties");
        setField(mojo, "includeResources", !testResources);
        setField(mojo, "includeTestResources", testResources);
        setField(mojo, testResources ? "testResources" : "resources", Collections.singletonList(resource));
        setField(mojo, "outputFile", directory.resolve("checkstyle-result.xml").toFile());
        return mojo;
    }

    private void setField(CheckstyleViolationCheckMojo mojo, String name, Object value) throws Exception {
        Field field = CheckstyleViolationCheckMojo.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(mojo, value);
    }

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

    @InjectMojo(goal = "check", pom = "src/test/resources/plugin-configs/no-source-check-plugin-config.xml")
    @MojoParameter(name = "sourceDirectories", value = "/src/test/plugin-configs/no-sources")
    @MojoParameter(name = "includeResources", value = "false")
    @MojoParameter(name = "includeTestResources", value = "false")
    @MojoParameter(
            name = "outputFile",
            value = "${basedir}/target/test-harness/checkstyle/no-source-check/checkstyle-result.xml")
    @MojoParameter(
            name = "cacheFile",
            value = "${basedir}/target/test-harness/checkstyle/no-source-check/checkstyle-cachefile")
    @Test
    public void testNoSourceDoesNotCreateOutputFiles(CheckstyleViolationCheckMojo mojo) throws Exception {
        project.setPackaging("pom");
        File outputDirectory = new File(project.getBasedir(), "target/test-harness/checkstyle/no-source-check");
        FileUtils.deleteDirectory(outputDirectory);
        assertFalse(outputDirectory.exists());

        mojo.execute();

        assertFalse(outputDirectory.exists());
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
