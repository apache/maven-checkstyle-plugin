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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.maven.api.di.Provides;
import org.apache.maven.api.plugin.testing.Basedir;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs <code>checkstyle:check</code> against the scenarios in <code>src/test/resources/check-scenarios</code>, with
 * the real Checkstyle executor, and asserts on the plugin log and the failure message.
 * <p>
 * The log of the plugin is recorded. The executor logs through SLF4J instead, so what it writes to
 * <code>System.err</code> is captured as well (<code>simplelogger.properties</code> enables its debug level).
 */
@MojoTest
public class CheckstyleViolationCheckScenarioTest {

    @Inject
    private MavenProject project;

    /**
     * The scenarios are copied to <code>target/test-classes</code> and write their own <code>target</code> directory,
     * which has to be gone before a scenario runs, as with <code>mvn clean</code> in an integration test.
     */
    @BeforeEach
    public void cleanScenarioBuildDirectory() throws IOException {
        FileUtils.deleteDirectory(new File(project.getBasedir(), "target"));
    }

    @Provides
    public PluginDescriptor getPluginDescriptor() {
        PluginDescriptor descriptorStub = new PluginDescriptor();
        descriptorStub.setGroupId("org.apache.maven.plugins");
        descriptorStub.setArtifactId("maven-checkstyle-plugin");
        return descriptorStub;
    }

    /** MCHECKSTYLE-268: a violation severity of warning counts the warnings of google_checks.xml. */
    @Basedir("/check-scenarios/MCHECKSTYLE-268")
    @InjectMojo(goal = "check", pom = "pom.xml")
    @Test
    public void testViolationSeverityWarning(CheckstyleViolationCheckMojo mojo) throws Exception {
        project.addCompileSourceRoot("src/main/java");

        Execution execution = execute(mojo);

        assertEquals(
                "You have 3 Checkstyle violations.",
                execution.failure(MojoFailureException.class).getMessage());
    }

    /** MCHECKSTYLE-295: resources matching resourceIncludes are checked, other rules than the default apply. */
    @Basedir("/check-scenarios/MCHECKSTYLE-295")
    @InjectMojo(goal = "check", pom = "pom.xml")
    @Test
    public void testTestResourcesWithInlineRules(CheckstyleViolationCheckMojo mojo) throws Exception {
        Execution execution = execute(mojo);

        assertTrue(
                execution.output.contains("There are 2 errors reported by Checkstyle"),
                () -> "Missing the error count in: " + execution.output);
        assertEquals(
                "You have 2 Checkstyle violations.",
                execution.failure(MojoFailureException.class).getMessage());
    }

    /** MCHECKSTYLE-347: a blank violationIgnore ignores nothing. */
    @Basedir("/check-scenarios/MCHECKSTYLE-347")
    @InjectMojo(goal = "check", pom = "pom.xml")
    @Test
    public void testBlankViolationIgnore(CheckstyleViolationCheckMojo mojo) throws Exception {
        project.addCompileSourceRoot("src/main/java");
        setViolationIgnore(mojo, "");

        Execution execution = execute(mojo);

        assertEquals(
                "You have 1 Checkstyle violation.",
                execution.failure(MojoFailureException.class).getMessage());
    }

    /** check-fail: the violations of the default configuration are counted. */
    @Basedir("/check-scenarios/check-fail")
    @InjectMojo(goal = "check", pom = "pom.xml")
    @Test
    public void testDefaultConfigurationViolation(CheckstyleViolationCheckMojo mojo) throws Exception {
        project.addCompileSourceRoot("src/main/java");

        Execution execution = execute(mojo);

        assertEquals(
                "You have 1 Checkstyle violation.",
                execution.failure(MojoFailureException.class).getMessage());
    }

    /** MCHECKSTYLE-371: violations within maxAllowedViolations pass and the log states the maximum. */
    @Basedir("/check-scenarios/MCHECKSTYLE-371")
    @InjectMojo(goal = "check", pom = "pom.xml")
    @Test
    public void testMaxAllowedViolations(CheckstyleViolationCheckMojo mojo) throws Exception {
        Execution execution = execute(mojo);

        assertNull(execution.failure, () -> "Unexpected failure: " + execution.failure);
        assertTrue(
                execution.output.contains(
                        "[INFO] You have 1 Checkstyle violation. The maximum number of allowed violations is 10."),
                () -> "Missing the violation count in: " + execution.output);
    }

    /** MCHECKSTYLE-412: with excludeGeneratedSources, source roots below the build directory are not checked. */
    @Basedir("/check-scenarios/MCHECKSTYLE-412")
    @InjectMojo(goal = "check", pom = "pom.xml")
    @Test
    public void testExcludeGeneratedSources(CheckstyleViolationCheckMojo mojo) throws Exception {
        Build build = new Build();
        build.setDirectory(new File(project.getBasedir(), "build").getAbsolutePath());
        project.getModel().setBuild(build);
        project.addCompileSourceRoot("src/main/java");
        project.addCompileSourceRoot("build/generated-sources/antlr4");

        Execution execution = execute(mojo);

        assertNull(execution.failure, () -> "Unexpected failure: " + execution.failure);
        assertTrue(
                execution.output.contains("[INFO] You have 0 Checkstyle violations."),
                () -> "Missing the violation count in: " + execution.output);
    }

    /** MCHECKSTYLE-214: a resource that is excluded from the jar, but matches resourceIncludes, is still checked. */
    @Basedir("/check-scenarios/MCHECKSTYLE-214-basedir-resource")
    @InjectMojo(goal = "check", pom = "pom.xml")
    @Test
    public void testResourceInBasedir(CheckstyleViolationCheckMojo mojo) throws Exception {
        Execution execution = execute(mojo);

        // LICENSE.txt has 2 errors, src/main/resources/README.txt has 1 error
        assertTrue(
                execution.output.contains("There are 3 errors reported by Checkstyle"),
                () -> "Missing the error count in: " + execution.output);
        assertEquals(
                "You have 3 Checkstyle violations.",
                execution.failure(MojoFailureException.class).getMessage());
        String result = readResult(mojo);
        assertEquals(2, count(result, "<file "), () -> "Files in the result: " + result);
    }

    /** MCHECKSTYLE-219: a missing header file is only fatal when the configuration uses it. */
    @Basedir("/check-scenarios/MCHECKSTYLE-219-no-license_failure")
    @InjectMojo(goal = "check", pom = "pom.xml")
    @Test
    public void testNoLicenseFailure(CheckstyleViolationCheckMojo mojo) throws Exception {
        Execution execution = execute(mojo);

        assertFalse(execution.output.contains("The resource 'LICENSE.txt' was found"));
        assertTrue(
                execution.output.contains("Unable to process header location: LICENSE.txt"),
                () -> "Missing the header warning in: " + execution.output);
        assertTrue(
                execution.output.contains("Checkstyle will throw exception if ${checkstyle.header.file} is used"),
                () -> "Missing the header warning in: " + execution.output);
        // Maven reports the messages of the whole cause chain
        String messages = causeMessages(execution.failure(MojoExecutionException.class));
        assertTrue(
                messages.contains("Failed during checkstyle configuration"), () -> "Unexpected failure: " + messages);
        assertTrue(
                messages.contains("Property ${checkstyle.header.file} has not been set"),
                () -> "Unexpected failure: " + messages);
    }

    /** MCHECKSTYLE-219: the default configuration does not use the header file, so a missing one is not an error. */
    @Basedir("/check-scenarios/MCHECKSTYLE-219-no-license_success")
    @InjectMojo(goal = "check", pom = "pom.xml")
    @Test
    public void testNoLicenseSuccess(CheckstyleViolationCheckMojo mojo) throws Exception {
        Execution execution = execute(mojo);

        assertNull(execution.failure, () -> "Unexpected failure: " + execution.failure);
        assertFalse(execution.output.contains("The resource 'LICENSE.txt' was found"));
        assertTrue(
                execution.output.contains("Unable to process header location: LICENSE.txt"),
                () -> "Missing the header warning in: " + execution.output);
        assertTrue(
                execution.output.contains("Checkstyle will throw exception if ${checkstyle.header.file} is used"),
                () -> "Missing the header warning in: " + execution.output);
    }

    /** MCHECKSTYLE-220: the debug output has no MalformedURLException. */
    @Basedir("/check-scenarios/MCHECKSTYLE-220")
    @InjectMojo(goal = "check", pom = "pom.xml")
    @Test
    public void testNoMalformedUrlException(CheckstyleViolationCheckMojo mojo) throws Exception {
        project.addCompileSourceRoot("src/main/java");

        Execution execution = execute(mojo);

        assertNull(execution.failure, () -> "Unexpected failure: " + execution.failure);
        assertTrue(
                execution.output.contains("headerLocation"),
                () -> "The debug output of the executor is not captured: " + execution.output);
        assertFalse(execution.output.contains("java.net.MalformedURLException"));
    }

    /** MCHECKSTYLE-70: several source roots, main and test, are all checked. */
    @Basedir("/check-scenarios/MCHECKSTYLE-70-multi-source-directory")
    @InjectMojo(goal = "check", pom = "pom.xml")
    @Test
    public void testMultipleSourceDirectories(CheckstyleViolationCheckMojo mojo) throws Exception {
        project.addCompileSourceRoot("src/additional/java");
        project.addTestCompileSourceRoot("src/test-additional/java");

        Execution execution = execute(mojo);

        // 2 errors in src/additional/java and 1 error in src/test-additional/java
        assertTrue(
                execution.output.contains("There are 3 errors reported by Checkstyle"),
                () -> "Missing the error count in: " + execution.output);
        assertEquals(
                "You have 3 Checkstyle violations.",
                execution.failure(MojoFailureException.class).getMessage());
    }

    /** check-tests-only: a project with test sources only is checked, and failsOnError reports the error count. */
    @Basedir("/check-scenarios/check-tests-only")
    @InjectMojo(goal = "check", pom = "pom.xml")
    @Test
    public void testOnlyTestSources(CheckstyleViolationCheckMojo mojo) throws Exception {
        project.addTestCompileSourceRoot("src/test/java");

        Execution execution = execute(mojo);

        MojoExecutionException failure = execution.failure(MojoExecutionException.class);
        assertEquals("Failed during checkstyle execution", failure.getMessage());
        assertTrue(
                Pattern.compile("There are \\d+ errors reported by Checkstyle")
                        .matcher(failure.getCause().getMessage())
                        .find(),
                () -> "Unexpected cause: " + failure.getCause().getMessage());
    }

    /** MCHECKSTYLE-337: the plain output file lists the violations. */
    @Basedir("/check-scenarios/MCHECKSTYLE-337")
    @InjectMojo(goal = "check", pom = "pom.xml")
    @Test
    public void testPlainOutputFile(CheckstyleViolationCheckMojo mojo) throws Exception {
        project.addCompileSourceRoot("src/main/java");
        Locale defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
        try {
            Execution execution = execute(mojo);

            assertEquals(
                    "You have 1 Checkstyle violation.",
                    execution.failure(MojoFailureException.class).getMessage());
        } finally {
            Locale.setDefault(defaultLocale);
        }
        File plainOutput = new File(project.getBasedir(), "target/checkstyle-result.txt");
        assertTrue(plainOutput.exists(), "Plain output file exists");
        String text = new String(Files.readAllBytes(plainOutput.toPath()), StandardCharsets.UTF_8);
        assertTrue(
                text.contains("'{' at column 1 should be on the previous line. [LeftCurly]"),
                () -> "Unexpected plain output: " + text);
    }

    private static String causeMessages(Throwable failure) {
        StringBuilder messages = new StringBuilder();
        for (Throwable t = failure; t != null; t = t.getCause()) {
            messages.append(t.getMessage()).append('\n');
        }
        return messages.toString();
    }

    private static void setViolationIgnore(CheckstyleViolationCheckMojo mojo, String value) throws Exception {
        java.lang.reflect.Field field = CheckstyleViolationCheckMojo.class.getDeclaredField("violationIgnore");
        field.setAccessible(true);
        field.set(mojo, value);
    }

    private String readResult(CheckstyleViolationCheckMojo mojo) throws Exception {
        File outputFile =
                (File) org.apache.maven.api.plugin.testing.MojoExtension.getVariableValueFromObject(mojo, "outputFile");
        assertNotNull(outputFile);
        return new String(Files.readAllBytes(outputFile.toPath()), StandardCharsets.UTF_8);
    }

    private static int count(String text, String part) {
        int count = 0;
        for (int i = text.indexOf(part); i >= 0; i = text.indexOf(part, i + part.length())) {
            count++;
        }
        return count;
    }

    /**
     * Runs the mojo, recording what it logs and what the executor writes to the standard error stream.
     */
    private static Execution execute(CheckstyleViolationCheckMojo mojo) throws Exception {
        StringBuilder pluginLog = new StringBuilder();
        mojo.setLog(new RecordingLog(pluginLog));

        Exception failure = null;
        try (StandardErrorCapture err = new StandardErrorCapture()) {
            try {
                mojo.execute();
            } catch (MojoExecutionException | MojoFailureException e) {
                failure = e;
            }
            return new Execution(pluginLog + err.text(), failure);
        }
    }

    private static final class Execution {
        private final String output;

        private final Exception failure;

        private Execution(String output, Exception failure) {
            this.output = output;
            this.failure = failure;
        }

        private <T extends Exception> T failure(Class<T> type) {
            assertNotNull(failure, "The build must fail");
            return assertThrows(type, () -> {
                throw failure;
            });
        }
    }

    private static final class RecordingLog extends SystemStreamLog {
        private final StringBuilder lines;

        private RecordingLog(StringBuilder lines) {
            this.lines = lines;
        }

        @Override
        public void info(CharSequence content) {
            lines.append("[INFO] ").append(content).append('\n');
        }

        @Override
        public void warn(CharSequence content) {
            lines.append("[WARNING] ").append(content).append('\n');
        }

        @Override
        public void error(CharSequence content) {
            lines.append("[ERROR] ").append(content).append('\n');
        }
    }
}
