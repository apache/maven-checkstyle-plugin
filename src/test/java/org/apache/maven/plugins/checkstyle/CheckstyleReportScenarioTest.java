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
import java.util.Arrays;
import java.util.Collections;

import org.apache.maven.api.plugin.testing.Basedir;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.internal.aether.DefaultRepositorySystemSessionFactory;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.apache.maven.api.plugin.testing.MojoExtension.setVariableValueToObject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Generates the <code>checkstyle</code> report for the scenarios in <code>src/test/resources/report-scenarios</code>
 * and asserts on the generated files.
 */
@MojoTest
public class CheckstyleReportScenarioTest {

    @Inject
    private MavenProject testMavenProject;

    @Inject
    private MavenSession mavenSession;

    @Inject
    private DefaultRepositorySystemSessionFactory repoSessionFactory;

    @Inject
    private MojoExecution mojoExecution;

    @BeforeEach
    public void setUp() throws Exception {
        // the scenarios are copied to target/test-classes and write their own target directory: clean it, as
        // mvn clean does in an integration test
        FileUtils.deleteDirectory(new File(testMavenProject.getBasedir(), "target"));

        ArtifactRepository localRepo = Mockito.mock(ArtifactRepository.class);
        Mockito.when(localRepo.getBasedir()).thenReturn(new File("target/local-repo").getAbsolutePath());

        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setLocalRepository(localRepo);

        RemoteRepository centralRepo =
                new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2").build();

        DefaultRepositorySystemSession systemSession = repoSessionFactory.newRepositorySession(request);
        Mockito.when(mavenSession.getRepositorySession()).thenReturn(systemSession);
        Mockito.when(testMavenProject.getRemoteProjectRepositories())
                .thenReturn(Collections.singletonList(centralRepo));

        Mockito.when(mojoExecution.getPlugin()).thenReturn(new Plugin());
    }

    /** MCHECKSTYLE-137: the source directory may be a parent of the package directories. */
    @Basedir("/report-scenarios/MCHECKSTYLE-137")
    @InjectMojo(goal = "checkstyle", pom = "pom.xml")
    @Test
    public void testSourceDirectoryAboveSources(CheckstyleReport mojo) throws Exception {
        try (StandardErrorCapture err = new StandardErrorCapture()) {
            mojo.execute();

            // the result file only lists files with violations, so the debug output of the executor tells
            File sourceDirectory = new File(testMavenProject.getBasedir(), "src");
            assertTrue(
                    err.text().contains("Added 1 source files found in '" + sourceDirectory.getAbsolutePath() + "'"),
                    () -> "The source directory is not checked: " + err.text());
        }
        assertReportGenerated(mojo);
    }

    /** MCHECKSTYLE-173: a configuration that sets the charset of the Checker is accepted. */
    @Basedir("/report-scenarios/MCHECKSTYLE-173-with-charset")
    @InjectMojo(goal = "checkstyle", pom = "pom.xml")
    @Test
    public void testConfigurationWithCharset(CheckstyleReport mojo) throws Exception {
        mojo.execute();

        assertReportGenerated(mojo);
    }

    /** MCHECKSTYLE-173: a configuration without a charset gets the input encoding. */
    @Basedir("/report-scenarios/MCHECKSTYLE-173-without-charset")
    @InjectMojo(goal = "checkstyle", pom = "pom.xml")
    @Test
    public void testConfigurationWithoutCharset(CheckstyleReport mojo) throws Exception {
        mojo.execute();

        assertReportGenerated(mojo);
    }

    /** MCHECKSTYLE-193: resourceExcludes keeps a resource out of the report. */
    @Basedir("/report-scenarios/MCHECKSTYLE-193")
    @InjectMojo(goal = "checkstyle", pom = "pom.xml")
    @MojoParameter(name = "resourceExcludes", value = "**/test.properties")
    @Test
    public void testResourceExcludes(CheckstyleReport mojo) throws Exception {
        mojo.execute();

        assertFalse(resultFileMentions("test.properties"));
    }

    /** MCHECKSTYLE-193: includeResources=false keeps all resources out of the report. */
    @Basedir("/report-scenarios/MCHECKSTYLE-193")
    @InjectMojo(goal = "checkstyle", pom = "pom.xml")
    @MojoParameter(name = "includeResources", value = "false")
    @Test
    public void testIncludeResourcesFalse(CheckstyleReport mojo) throws Exception {
        mojo.execute();

        assertFalse(resultFileMentions("test.properties"));
    }

    /** MCHECKSTYLE-193: control for the two tests above, the resource is checked and fails the build. */
    @Basedir("/report-scenarios/MCHECKSTYLE-193")
    @InjectMojo(goal = "checkstyle", pom = "pom.xml")
    @Test
    public void testResourceIsCheckedByDefault(CheckstyleReport mojo) {
        MojoExecutionException failure = assertThrows(MojoExecutionException.class, mojo::execute);
        assertTrue(
                failure.getCause().getCause().getMessage().contains("There is 1 error reported by Checkstyle"),
                () -> "Unexpected failure: " + failure.getCause().getCause());
    }

    /** MCHECKSTYLE-222: a project with resources only gets a report. */
    @Basedir("/report-scenarios/MCHECKSTYLE-222-resources")
    @InjectMojo(goal = "checkstyle", pom = "pom.xml")
    @Test
    public void testResourcesOnly(CheckstyleReport mojo) throws Exception {
        mojo.execute();

        assertReportGenerated(mojo);
    }

    /** MCHECKSTYLE-222: a project with test resources only gets a report. */
    @Basedir("/report-scenarios/MCHECKSTYLE-222-testResources")
    @InjectMojo(goal = "checkstyle", pom = "pom.xml")
    @Test
    public void testTestResourcesOnly(CheckstyleReport mojo) throws Exception {
        mojo.execute();

        assertReportGenerated(mojo);
    }

    /** MCHECKSTYLE-253: Java 8 syntax is parsed. */
    @Basedir("/report-scenarios/MCHECKSTYLE-253-jdk8")
    @InjectMojo(goal = "checkstyle", pom = "pom.xml")
    @Test
    public void testJava8Syntax(CheckstyleReport mojo) throws Exception {
        mojo.execute();

        String result = read(mojo, "target/checkstyle-result.xml");
        assertTrue(result.contains("MyClass.java"), () -> "MyClass.java is checked: " + result);
        assertFalse(result.contains("expecting EOF, found &apos;return&apos;"), () -> "Parse error in: " + result);
    }

    /** MCHECKSTYLE-357: inline rules, the files of the execution are written. */
    @Basedir("/report-scenarios/MCHECKSTYLE-357")
    @InjectMojo(goal = "checkstyle", pom = "pom.xml")
    @Test
    public void testInlineRules(CheckstyleReport mojo) throws Exception {
        mojo.execute();

        assertReportGenerated(mojo);
        for (String file : new String[] {
            "checkstyle-cachefile", "checkstyle-checker.xml", "checkstyle-result.xml", "checkstyle-rules.xml"
        }) {
            File generated = new File(testMavenProject.getBasedir(), "target/" + file);
            assertTrue(generated.exists(), () -> "File exists: " + generated);
        }
    }

    /** MCHECKSTYLE-365: the report lists a rule once per file, with or without a custom message. */
    @Basedir("/report-scenarios/MCHECKSTYLE-365")
    @InjectMojo(goal = "checkstyle", pom = "pom.xml")
    @Test
    public void testRuleNamesWithCustomMessages(CheckstyleReport mojo) throws Exception {
        mojo.execute();

        String html = new String(
                Files.readAllBytes(new File(mojo.getReportOutputDirectory(), mojo.getOutputPath() + ".html").toPath()),
                StandardCharsets.UTF_8);
        // no custom message
        assertEquals(3, count(html, ">FileTabCharacter<"), () -> "Unexpected report: " + html);
        // custom message
        assertEquals(3, count(html, ">RegexpSingleline<"), () -> "Unexpected report: " + html);
    }

    /** checkstyle-goal: the files of the execution are written and the report declares its charset. */
    @Basedir("/report-scenarios/checkstyle-goal")
    @InjectMojo(goal = "checkstyle", pom = "pom.xml")
    @Test
    public void testGeneratedFiles(CheckstyleReport mojo) throws Exception {
        mojo.execute();

        for (String file : new String[] {
            "checkstyle-cachefile", "checkstyle-checker.xml", "checkstyle-header.txt", "checkstyle-result.xml"
        }) {
            File generated = new File(testMavenProject.getBasedir(), "target/" + file);
            assertTrue(generated.exists(), () -> "File exists: " + generated);
        }
        String html = new String(
                Files.readAllBytes(new File(mojo.getReportOutputDirectory(), mojo.getOutputPath() + ".html").toPath()),
                StandardCharsets.UTF_8);
        assertTrue(html.contains("<meta charset=\"UTF-8\" />"), () -> "Unexpected report: " + html);
    }

    private static int count(String text, String part) {
        int count = 0;
        for (int i = text.indexOf(part); i >= 0; i = text.indexOf(part, i + part.length())) {
            count++;
        }
        return count;
    }

    /** MCHECKSTYLE-172: a single module project does not get an aggregate report. */
    @Basedir("/report-scenarios/MCHECKSTYLE-172")
    @InjectMojo(goal = "checkstyle-aggregate", pom = "pom.xml")
    @Test
    public void testNoAggregateReportForSingleModule(CheckstyleAggregateReport mojo) throws Exception {
        testMavenProject.setExecutionRoot(true);
        setVariableValueToObject(mojo, "reactorProjects", Collections.singletonList(testMavenProject));

        assertFalse(mojo.canGenerateReport());
    }

    /** MCHECKSTYLE-172: control for the test above, the execution root of a reactor gets an aggregate report. */
    @Basedir("/report-scenarios/MCHECKSTYLE-172")
    @InjectMojo(goal = "checkstyle-aggregate", pom = "pom.xml")
    @Test
    public void testAggregateReportForReactor(CheckstyleAggregateReport mojo) throws Exception {
        testMavenProject.setExecutionRoot(true);
        setVariableValueToObject(mojo, "reactorProjects", Arrays.asList(testMavenProject, new MavenProject()));

        assertTrue(mojo.canGenerateReport());
    }

    /** A result file is only written when there are files to check. */
    private boolean resultFileMentions(String text) throws IOException {
        File result = new File(testMavenProject.getBasedir(), "target/checkstyle-result.xml");
        return result.exists()
                && new String(Files.readAllBytes(result.toPath()), StandardCharsets.UTF_8).contains(text);
    }

    private void assertReportGenerated(CheckstyleReport mojo) {
        File report = new File(mojo.getReportOutputDirectory(), mojo.getOutputPath() + ".html");
        assertTrue(report.exists(), () -> "Report exists: " + report);
    }

    private String read(CheckstyleReport mojo, String path) throws IOException {
        File file = new File(testMavenProject.getBasedir(), path);
        assertTrue(file.exists(), () -> "File exists: " + file);
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }
}
