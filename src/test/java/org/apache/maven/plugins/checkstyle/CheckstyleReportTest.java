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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.doxia.tools.SiteTool;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.ArtifactStubFactory;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.internal.impl.SimpleLocalRepositoryManagerFactory;
import org.eclipse.aether.repository.LocalRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Edwin Punzalan
 */
public class CheckstyleReportTest extends AbstractMojoTestCase {

    private ArtifactStubFactory artifactStubFactory;
    /**
     * The project to test.
     */
    private MavenProject testMavenProject;

    @BeforeEach
    public void setUp() throws Exception {
        // required for mojo lookups to work
        super.setUp();

        artifactStubFactory = new DependencyArtifactStubFactory(getTestFile("target"), true, false);
        artifactStubFactory.getWorkingDir().mkdirs();
    }

    @Test
    public void testNoSource() throws Exception {
        File generatedReport = generateReport(getGoal(), "no-source-plugin-config.xml");
        assertFalse(new File(generatedReport.getAbsolutePath()).exists());
    }

    @Test
    public void testMinConfiguration() throws Exception {
        generateReport("min-plugin-config.xml");
    }

    @Test
    public void testCustomConfiguration() throws Exception {
        generateReport("custom-plugin-config.xml");
    }

    @Test
    public void testUseFile() throws Exception {
        generateReport("useFile-plugin-config.xml");
    }

    @Test
    public void testNoRulesSummary() throws Exception {
        generateReport("no-rules-plugin-config.xml");
    }

    @Test
    public void testNoSeveritySummary() throws Exception {
        generateReport("no-severity-plugin-config.xml");
    }

    @Test
    public void testNoFilesSummary() throws Exception {
        generateReport("no-files-plugin-config.xml");
    }

    @Test
    public void testFailOnError() {
        try {
            generateReport("fail-on-error-plugin-config.xml");

            fail("Must throw exception on errors");
        } catch (Exception e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testDependencyResolutionException() {
        try {
            generateReport("dep-resolution-exception-plugin-config.xml");

            fail("Must throw exception on errors");
        } catch (Exception e) {
            if (!(e.getCause().getCause().getCause() instanceof DependencyResolutionRequiredException)) {
                e.printStackTrace();
                fail("Must throw exception DependencyResolutionRequiredException on errors and not "
                        + e.getClass().getName() + ", " + e.getMessage());
            }
        }
    }

    @Test
    public void testTestSourceDirectory() throws Exception {
        generateReport("test-source-directory-plugin-config.xml");
    }

    private void generateReport(String pluginXml) throws Exception {
        File pluginXmlFile = new File(getBasedir(), "src/test/resources/plugin-configs/" + pluginXml);
        ResourceBundle bundle =
                ResourceBundle.getBundle("checkstyle-report", SiteTool.DEFAULT_LOCALE, this.getClassLoader());

        CheckstyleReport mojo = createReportMojo(getGoal(), pluginXmlFile);

        PluginDescriptor descriptorStub = new PluginDescriptor();
        descriptorStub.setGroupId("org.apache.maven.plugins");
        descriptorStub.setArtifactId("maven-checkstyle-plugin");
        setVariableValueToObject(mojo, "plugin", descriptorStub);

        File generatedReport = generateReport(mojo, pluginXmlFile);
        assertTrue(new File(generatedReport.getAbsolutePath()).exists());

        File outputFile = (File) getVariableValueFromObject(mojo, "outputFile");
        Assertions.assertNotNull(outputFile, "Test output file");
        Assertions.assertTrue(outputFile.exists(), "Test output file exists");

        String cacheFile = (String) getVariableValueFromObject(mojo, "cacheFile");
        if (cacheFile != null) {
            Assertions.assertTrue(new File(cacheFile).exists(), "Test cache file exists");
        }

        File useFile = (File) getVariableValueFromObject(mojo, "useFile");
        if (useFile != null) {
            Assertions.assertTrue(useFile.exists(), "Test useFile exists");
        }

        String str = new String(Files.readAllBytes(generatedReport.toPath()), StandardCharsets.UTF_8);

        boolean searchHeaderFound = str.contains(getHtmlHeader(bundle.getString("report.checkstyle.rules")));
        Boolean rules = (Boolean) getVariableValueFromObject(mojo, "enableRulesSummary");
        if (rules) {
            Assertions.assertTrue(searchHeaderFound, "Test for Rules Summary");
        } else {
            Assertions.assertFalse(searchHeaderFound, "Test for Rules Summary");
        }

        searchHeaderFound = str.contains(getHtmlHeader(bundle.getString("report.checkstyle.summary")));
        Boolean severity = (Boolean) getVariableValueFromObject(mojo, "enableSeveritySummary");
        if (severity) {
            Assertions.assertTrue(searchHeaderFound, "Test for Severity Summary");
        } else {
            Assertions.assertFalse(searchHeaderFound, "Test for Severity Summary");
        }

        searchHeaderFound = str.contains(getHtmlHeader(bundle.getString("report.checkstyle.files")));
        Boolean files = (Boolean) getVariableValueFromObject(mojo, "enableFilesSummary");
        if (files) {
            Assertions.assertTrue(searchHeaderFound, "Test for Files Summary");
        } else {
            Assertions.assertFalse(searchHeaderFound, "Test for Files Summary");
        }
    }

    private static String getHtmlHeader(String s) {
        return ">" + s + "</h2>";
    }

    protected String getGoal() {
        return "checkstyle";
    }

    /**
     * Get the current Maven project
     *
     * @return the maven project
     */
    protected MavenProject getTestMavenProject() {
        return testMavenProject;
    }

    /**
     * Get the generated report as file in the test maven project.
     *
     * @param name the name of the report
     * @return the generated report as file
     * @throws IOException if the return file doesn't exist
     */
    protected File getGeneratedReport(String name) throws IOException {
        String outputDirectory = getBasedir() + "/target/test/test-harness/"
                + getTestMavenProject().getArtifactId();

        File report = new File(outputDirectory, name);
        if (!report.exists()) {
            throw new IOException("File not found. Attempted: " + report);
        }

        return report;
    }

    /**
     * Generate the report and return the generated file
     *
     * @param goal the mojo goal.
     * @param pluginXml the name of the XML file in "src/test/resources/plugin-configs/"
     * @return the generated HTML file
     * @throws Exception if any
     */
    protected File generateReport(String goal, String pluginXml) throws Exception {
        File pluginXmlFile = new File(getBasedir(), "src/test/resources/plugin-configs/" + pluginXml);
        CheckstyleReport mojo = createReportMojo(goal, pluginXmlFile);
        return generateReport(mojo, pluginXmlFile);
    }

    protected CheckstyleReport createReportMojo(String goal, File pluginXmlFile) throws Exception {
        CheckstyleReport mojo = (CheckstyleReport) lookupMojo(goal, pluginXmlFile);
        Assertions.assertNotNull(mojo, "Mojo not found.");

        LegacySupport legacySupport = lookup(LegacySupport.class);
        legacySupport.setSession(newMavenSession(new MavenProjectStub()));
        DefaultRepositorySystemSession repoSession =
                (DefaultRepositorySystemSession) legacySupport.getRepositorySession();
        repoSession.setLocalRepositoryManager(new SimpleLocalRepositoryManagerFactory()
                .newInstance(repoSession, new LocalRepository(artifactStubFactory.getWorkingDir())));

        List<MavenProject> reactorProjects =
                mojo.getReactorProjects() != null ? mojo.getReactorProjects() : Collections.emptyList();

        setVariableValueToObject(mojo, "mojoExecution", getMockMojoExecution());
        setVariableValueToObject(mojo, "session", legacySupport.getSession());
        setVariableValueToObject(mojo, "repoSession", legacySupport.getRepositorySession());
        setVariableValueToObject(mojo, "reactorProjects", reactorProjects);
        setVariableValueToObject(
                mojo, "remoteProjectRepositories", mojo.getProject().getRemoteProjectRepositories());
        setVariableValueToObject(
                mojo, "siteDirectory", new File(mojo.getProject().getBasedir(), "src/site"));
        return mojo;
    }

    protected File generateReport(CheckstyleReport mojo, File pluginXmlFile) throws Exception {
        mojo.execute();

        ProjectBuilder builder = lookup(ProjectBuilder.class);

        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest();
        buildingRequest.setRepositorySession(lookup(LegacySupport.class).getRepositorySession());

        testMavenProject = builder.build(pluginXmlFile, buildingRequest).getProject();

        File outputDir = mojo.getReportOutputDirectory();
        String filename = mojo.getOutputName() + ".html";

        File file = new File(outputDir, filename);

        return file;
    }

    /**
     * Read the contents of the specified file object into a string
     */
    protected String readFile(File checkstyleTestDir, String fileName) throws IOException {
        return new String(Files.readAllBytes(checkstyleTestDir.toPath().resolve(fileName)));
    }

    private MojoExecution getMockMojoExecution() {
        MojoDescriptor md = new MojoDescriptor();
        md.setGoal(getGoal());

        MojoExecution me = new MojoExecution(md);

        PluginDescriptor pd = new PluginDescriptor();
        Plugin p = new Plugin();
        p.setGroupId("org.apache.maven.plugins");
        p.setArtifactId("maven-checkstyle-plugin");
        pd.setPlugin(p);
        md.setPluginDescriptor(pd);

        return me;
    }
}
