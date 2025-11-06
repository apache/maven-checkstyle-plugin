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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.ResourceBundle;

import org.apache.maven.api.plugin.testing.Basedir;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.doxia.tools.SiteTool;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.internal.aether.DefaultRepositorySystemSessionFactory;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.apache.maven.api.plugin.testing.MojoExtension.getVariableValueFromObject;
import static org.codehaus.plexus.testing.PlexusExtension.getBasedir;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Edwin Punzalan
 */
@MojoTest
public class CheckstyleReportTest {

    /**
     * The project to test.
     */
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
        // prepare realistic repository session
        ArtifactRepository localRepo = Mockito.mock(ArtifactRepository.class);
        Mockito.when(localRepo.getBasedir()).thenReturn(new File(getBasedir(), "target/local-repo").getAbsolutePath());

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

    @InjectMojo(goal = "checkstyle", pom = "src/test/resources/plugin-configs/no-source-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @Test
    public void testNoSource(CheckstyleReport mojo) throws Exception {
        mojo.execute();

        File outputDir = mojo.getReportOutputDirectory();
        String filename = mojo.getOutputName() + ".html";
        File generatedReport = new File(outputDir, filename);
        assertFalse(new File(generatedReport.getAbsolutePath()).exists());
    }

    // We need to change the basedir to point to test repositor with out site.xml file
    // without it test will use real project site.xml without skin configuration
    @Basedir("/plugin-configs")
    @InjectMojo(goal = "checkstyle", pom = "min-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @Test
    public void testMinConfiguration(CheckstyleReport mojo) throws Exception {
        ResourceBundle bundle =
                ResourceBundle.getBundle("checkstyle-report", SiteTool.DEFAULT_LOCALE, getClassLoader());
        //
        //        LegacySupport legacySupport = lookup(LegacySupport.class);
        //        legacySupport.setSession(newMavenSession(new MavenProjectStub()));
        //        DefaultRepositorySystemSession repoSession =
        //                (DefaultRepositorySystemSession) legacySupport.getRepositorySession();
        //        repoSession.setLocalRepositoryManager(new SimpleLocalRepositoryManagerFactory()
        //                .newInstance(repoSession, new LocalRepository(artifactStubFactory.getWorkingDir())));
        //
        //        List<MavenProject> reactorProjects =
        //                mojo.getReactorProjects() != null ? mojo.getReactorProjects() : Collections.emptyList();
        //
        //        setVariableValueToObject(mojo, "session", legacySupport.getSession());
        //        setVariableValueToObject(mojo, "repoSession", legacySupport.getRepositorySession());
        //        setVariableValueToObject(mojo, "reactorProjects", reactorProjects);
        //        setVariableValueToObject(
        //                mojo, "remoteProjectRepositories", mojo.getProject().getRemoteProjectRepositories());
        //        setVariableValueToObject(

        mojo.execute();

        //        ProjectBuilder builder = lookup(ProjectBuilder.class);
        //
        //        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest();
        //        buildingRequest.setRepositorySession(lookup(LegacySupport.class).getRepositorySession());
        //
        //        testMavenProject = builder.build(pluginXmlFile, buildingRequest).getProject();

        File outputDir = mojo.getReportOutputDirectory();
        String filename = mojo.getOutputName() + ".html";

        File generatedReport = new File(outputDir, filename);
        assertTrue(new File(generatedReport.getAbsolutePath()).exists());

        File outputFile = (File) getVariableValueFromObject(mojo, "outputFile");
        assertNotNull(outputFile, "Test output file");
        assertTrue(outputFile.exists(), "Test output file exists");

        String cacheFile = (String) getVariableValueFromObject(mojo, "cacheFile");
        if (cacheFile != null) {
            assertTrue(new File(cacheFile).exists(), "Test cache file exists");
        }

        File useFile = (File) getVariableValueFromObject(mojo, "useFile");
        if (useFile != null) {
            assertTrue(useFile.exists(), "Test useFile exists");
        }

        String str = new String(Files.readAllBytes(generatedReport.toPath()), StandardCharsets.UTF_8);

        boolean searchHeaderFound = str.contains(getHtmlHeader(bundle.getString("report.checkstyle.rules")));
        Boolean rules = (Boolean) getVariableValueFromObject(mojo, "enableRulesSummary");
        if (rules) {
            assertTrue(searchHeaderFound, "Test for Rules Summary");
        } else {
            assertFalse(searchHeaderFound, "Test for Rules Summary");
        }

        searchHeaderFound = str.contains(getHtmlHeader(bundle.getString("report.checkstyle.summary")));
        Boolean severity = (Boolean) getVariableValueFromObject(mojo, "enableSeveritySummary");
        if (severity) {
            assertTrue(searchHeaderFound, "Test for Severity Summary");
        } else {
            assertFalse(searchHeaderFound, "Test for Severity Summary");
        }

        searchHeaderFound = str.contains(getHtmlHeader(bundle.getString("report.checkstyle.files")));
        Boolean files = (Boolean) getVariableValueFromObject(mojo, "enableFilesSummary");
        if (files) {
            assertTrue(searchHeaderFound, "Test for Files Summary");
        } else {
            assertFalse(searchHeaderFound, "Test for Files Summary");
        }
    }

    private ClassLoader getClassLoader() {
        return this.getClass().getClassLoader();
    }

    // We need to change the basedir to point to test repositor with out site.xml file
    // without it test will use real project site.xml without skin configuration
    @Basedir("/plugin-configs")
    @InjectMojo(goal = "checkstyle", pom = "custom-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
        @Test
        public void testCustomConfiguration(CheckstyleReport mojo) throws Exception {
            ResourceBundle bundle =
                    ResourceBundle.getBundle("checkstyle-report", SiteTool.DEFAULT_LOCALE, this.getClassLoader());

        mojo.execute();

        File outputDir = mojo.getReportOutputDirectory();
        String filename = mojo.getOutputName() + ".html";

        File file = new File(outputDir, filename);

        File generatedReport = file;
            assertTrue(new File(generatedReport.getAbsolutePath()).exists());

            File outputFile = (File) getVariableValueFromObject(mojo, "outputFile");
            assertNotNull(outputFile, "Test output file");
            assertTrue(outputFile.exists(), "Test output file exists");

            String cacheFile = (String) getVariableValueFromObject(mojo, "cacheFile");
            if (cacheFile != null) {
                assertTrue(new File(cacheFile).exists(), "Test cache file exists");
            }

            File useFile = (File) getVariableValueFromObject(mojo, "useFile");
            if (useFile != null) {
                assertTrue(useFile.exists(), "Test useFile exists");
            }

            String str = new String(Files.readAllBytes(generatedReport.toPath()), StandardCharsets.UTF_8);

            boolean searchHeaderFound = str.contains(getHtmlHeader(bundle.getString("report.checkstyle.rules")));
            Boolean rules = (Boolean) getVariableValueFromObject(mojo, "enableRulesSummary");
            if (rules) {
                assertTrue(searchHeaderFound, "Test for Rules Summary");
            } else {
                assertFalse(searchHeaderFound, "Test for Rules Summary");
            }

            searchHeaderFound = str.contains(getHtmlHeader(bundle.getString("report.checkstyle.summary")));
            Boolean severity = (Boolean) getVariableValueFromObject(mojo, "enableSeveritySummary");
            if (severity) {
                assertTrue(searchHeaderFound, "Test for Severity Summary");
            } else {
                assertFalse(searchHeaderFound, "Test for Severity Summary");
            }

            searchHeaderFound = str.contains(getHtmlHeader(bundle.getString("report.checkstyle.files")));
            Boolean files = (Boolean) getVariableValueFromObject(mojo, "enableFilesSummary");
            if (files) {
                assertTrue(searchHeaderFound, "Test for Files Summary");
            } else {
                assertFalse(searchHeaderFound, "Test for Files Summary");
            }
        }
    // We need to change the basedir to point to test repositor with out site.xml file
    // without it test will use real project site.xml without skin configuration
    @Basedir("/plugin-configs")
    @InjectMojo(goal = "checkstyle", pom = "useFile-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
            @Test
        public void testUseFile(CheckstyleReport mojo) throws Exception {
            ResourceBundle bundle =
                    ResourceBundle.getBundle("checkstyle-report", SiteTool.DEFAULT_LOCALE, this.getClassLoader());


            mojo.execute();


            File outputDir = mojo.getReportOutputDirectory();
            String filename = mojo.getOutputName() + ".html";

        File generatedReport = new File(outputDir, filename);
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

    // We need to change the basedir to point to test repositor with out site.xml file
    // without it test will use real project site.xml without skin configuration
    @Basedir("/plugin-configs")
    @InjectMojo(goal = "checkstyle", pom = "no-rules-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
        @Test
        public void testNoRulesSummary(CheckstyleReport mojo) throws Exception {
            ResourceBundle bundle =
                    ResourceBundle.getBundle("checkstyle-report", SiteTool.DEFAULT_LOCALE, this.getClassLoader());

            mojo.execute();


            File outputDir = mojo.getReportOutputDirectory();
            String filename = mojo.getOutputName() + ".html";

        File generatedReport = new File(outputDir, filename);
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
//
//        @Test
//        public void testNoSeveritySummary(CheckstyleReport mojo1) throws Exception {
//            File pluginXmlFile =
//                    new File(getBasedir(), "src/test/resources/plugin-configs/" + "no-severity-plugin-config.xml");
//            ResourceBundle bundle =
//                    ResourceBundle.getBundle("checkstyle-report", SiteTool.DEFAULT_LOCALE, this.getClassLoader());
//            Assertions.assertNotNull(mojo1, "Mojo not found.");
//
//            LegacySupport legacySupport = lookup(LegacySupport.class);
//            legacySupport.setSession(newMavenSession(new MavenProjectStub()));
//            DefaultRepositorySystemSession repoSession =
//                    (DefaultRepositorySystemSession) legacySupport.getRepositorySession();
//            repoSession.setLocalRepositoryManager(new SimpleLocalRepositoryManagerFactory()
//                    .newInstance(repoSession, new LocalRepository(artifactStubFactory.getWorkingDir())));
//
//            List<MavenProject> reactorProjects =
//                    mojo1.getReactorProjects() != null ? mojo1.getReactorProjects() : Collections.emptyList();
//
//            setVariableValueToObject(mojo1, "mojoExecution", getMockMojoExecution());
//            setVariableValueToObject(mojo1, "session", legacySupport.getSession());
//            setVariableValueToObject(mojo1, "repoSession", legacySupport.getRepositorySession());
//            setVariableValueToObject(mojo1, "reactorProjects", reactorProjects);
//            setVariableValueToObject(
//                    mojo1, "remoteProjectRepositories", mojo1.getProject().getRemoteProjectRepositories());
//            setVariableValueToObject(
//                    mojo1, "siteDirectory", new File(mojo1.getProject().getBasedir(), "src/site"));
//            CheckstyleReport mojo = mojo1;
//
//            PluginDescriptor descriptorStub = new PluginDescriptor();
//            descriptorStub.setGroupId("org.apache.maven.plugins");
//            descriptorStub.setArtifactId("maven-checkstyle-plugin");
//            setVariableValueToObject(mojo, "plugin", descriptorStub);
//
//            mojo.execute();
//
//            ProjectBuilder builder = lookup(ProjectBuilder.class);
//
//            ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest();
//            buildingRequest.setRepositorySession(lookup(LegacySupport.class).getRepositorySession());
//
//            testMavenProject = builder.build(pluginXmlFile, buildingRequest).getProject();
//
//            File outputDir = mojo.getReportOutputDirectory();
//            String filename = mojo.getOutputName() + ".html";
//
//            File file = new File(outputDir, filename);
//
//            File generatedReport = file;
//            assertTrue(new File(generatedReport.getAbsolutePath()).exists());
//
//            File outputFile = (File) getVariableValueFromObject(mojo, "outputFile");
//            Assertions.assertNotNull(outputFile, "Test output file");
//            Assertions.assertTrue(outputFile.exists(), "Test output file exists");
//
//            String cacheFile = (String) getVariableValueFromObject(mojo, "cacheFile");
//            if (cacheFile != null) {
//                Assertions.assertTrue(new File(cacheFile).exists(), "Test cache file exists");
//            }
//
//            File useFile = (File) getVariableValueFromObject(mojo, "useFile");
//            if (useFile != null) {
//                Assertions.assertTrue(useFile.exists(), "Test useFile exists");
//            }
//
//            String str = new String(Files.readAllBytes(generatedReport.toPath()), StandardCharsets.UTF_8);
//
//            boolean searchHeaderFound = str.contains(getHtmlHeader(bundle.getString("report.checkstyle.rules")));
//            Boolean rules = (Boolean) getVariableValueFromObject(mojo, "enableRulesSummary");
//            if (rules) {
//                Assertions.assertTrue(searchHeaderFound, "Test for Rules Summary");
//            } else {
//                Assertions.assertFalse(searchHeaderFound, "Test for Rules Summary");
//            }
//
//            searchHeaderFound = str.contains(getHtmlHeader(bundle.getString("report.checkstyle.summary")));
//            Boolean severity = (Boolean) getVariableValueFromObject(mojo, "enableSeveritySummary");
//            if (severity) {
//                Assertions.assertTrue(searchHeaderFound, "Test for Severity Summary");
//            } else {
//                Assertions.assertFalse(searchHeaderFound, "Test for Severity Summary");
//            }
//
//            searchHeaderFound = str.contains(getHtmlHeader(bundle.getString("report.checkstyle.files")));
//            Boolean files = (Boolean) getVariableValueFromObject(mojo, "enableFilesSummary");
//            if (files) {
//                Assertions.assertTrue(searchHeaderFound, "Test for Files Summary");
//            } else {
//                Assertions.assertFalse(searchHeaderFound, "Test for Files Summary");
//            }
//        }
//
//        @Test
//        public void testNoFilesSummary(CheckstyleReport mojo1) throws Exception {
//            File pluginXmlFile =
//                    new File(getBasedir(), "src/test/resources/plugin-configs/" + "no-files-plugin-config.xml");
//            ResourceBundle bundle =
//                    ResourceBundle.getBundle("checkstyle-report", SiteTool.DEFAULT_LOCALE, this.getClassLoader());
//            Assertions.assertNotNull(mojo1, "Mojo not found.");
//
//            LegacySupport legacySupport = lookup(LegacySupport.class);
//            legacySupport.setSession(newMavenSession(new MavenProjectStub()));
//            DefaultRepositorySystemSession repoSession =
//                    (DefaultRepositorySystemSession) legacySupport.getRepositorySession();
//            repoSession.setLocalRepositoryManager(new SimpleLocalRepositoryManagerFactory()
//                    .newInstance(repoSession, new LocalRepository(artifactStubFactory.getWorkingDir())));
//
//            List<MavenProject> reactorProjects =
//                    mojo1.getReactorProjects() != null ? mojo1.getReactorProjects() : Collections.emptyList();
//
//            setVariableValueToObject(mojo1, "mojoExecution", getMockMojoExecution());
//            setVariableValueToObject(mojo1, "session", legacySupport.getSession());
//            setVariableValueToObject(mojo1, "repoSession", legacySupport.getRepositorySession());
//            setVariableValueToObject(mojo1, "reactorProjects", reactorProjects);
//            setVariableValueToObject(
//                    mojo1, "remoteProjectRepositories", mojo1.getProject().getRemoteProjectRepositories());
//            setVariableValueToObject(
//                    mojo1, "siteDirectory", new File(mojo1.getProject().getBasedir(), "src/site"));
//
//            PluginDescriptor descriptorStub = new PluginDescriptor();
//            descriptorStub.setGroupId("org.apache.maven.plugins");
//            descriptorStub.setArtifactId("maven-checkstyle-plugin");
//            setVariableValueToObject(mojo1, "plugin", descriptorStub);
//
//            mojo1.execute();
//
//            ProjectBuilder builder = lookup(ProjectBuilder.class);
//
//            ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest();
//            buildingRequest.setRepositorySession(lookup(LegacySupport.class).getRepositorySession());
//
//            testMavenProject = builder.build(pluginXmlFile, buildingRequest).getProject();
//
//            File outputDir = mojo1.getReportOutputDirectory();
//            String filename = mojo1.getOutputName() + ".html";
//
//            File file = new File(outputDir, filename);
//
//            File generatedReport = file;
//            assertTrue(new File(generatedReport.getAbsolutePath()).exists());
//
//            File outputFile = (File) getVariableValueFromObject(mojo1, "outputFile");
//            Assertions.assertNotNull(outputFile, "Test output file");
//            Assertions.assertTrue(outputFile.exists(), "Test output file exists");
//
//            String cacheFile = (String) getVariableValueFromObject(mojo1, "cacheFile");
//            if (cacheFile != null) {
//                Assertions.assertTrue(new File(cacheFile).exists(), "Test cache file exists");
//            }
//
//            File useFile = (File) getVariableValueFromObject(mojo1, "useFile");
//            if (useFile != null) {
//                Assertions.assertTrue(useFile.exists(), "Test useFile exists");
//            }
//
//            String str = new String(Files.readAllBytes(generatedReport.toPath()), StandardCharsets.UTF_8);
//
//            boolean searchHeaderFound = str.contains(getHtmlHeader(bundle.getString("report.checkstyle.rules")));
//            Boolean rules = (Boolean) getVariableValueFromObject(mojo1, "enableRulesSummary");
//            if (rules) {
//                Assertions.assertTrue(searchHeaderFound, "Test for Rules Summary");
//            } else {
//                Assertions.assertFalse(searchHeaderFound, "Test for Rules Summary");
//            }
//
//            searchHeaderFound = str.contains(getHtmlHeader(bundle.getString("report.checkstyle.summary")));
//            Boolean severity = (Boolean) getVariableValueFromObject(mojo1, "enableSeveritySummary");
//            if (severity) {
//                Assertions.assertTrue(searchHeaderFound, "Test for Severity Summary");
//            } else {
//                Assertions.assertFalse(searchHeaderFound, "Test for Severity Summary");
//            }
//
//            searchHeaderFound = str.contains(getHtmlHeader(bundle.getString("report.checkstyle.files")));
//            Boolean files = (Boolean) getVariableValueFromObject(mojo1, "enableFilesSummary");
//            if (files) {
//                Assertions.assertTrue(searchHeaderFound, "Test for Files Summary");
//            } else {
//                Assertions.assertFalse(searchHeaderFound, "Test for Files Summary");
//            }
//        }
//
//        @Test
//        public void testFailOnError() {
//            try {
//                File pluginXmlFile =
//                        new File(getBasedir(), "src/test/resources/plugin-configs/" +
//     "fail-on-error-plugin-config.xml");
//                ResourceBundle bundle =
//                        ResourceBundle.getBundle("checkstyle-report", SiteTool.DEFAULT_LOCALE, this.getClassLoader());
//                Assertions.assertNotNull(mojo, "Mojo not found.");
//
//                LegacySupport legacySupport = lookup(LegacySupport.class);
//                legacySupport.setSession(newMavenSession(new MavenProjectStub()));
//                DefaultRepositorySystemSession repoSession =
//                        (DefaultRepositorySystemSession) legacySupport.getRepositorySession();
//                repoSession.setLocalRepositoryManager(new SimpleLocalRepositoryManagerFactory()
//                        .newInstance(repoSession, new LocalRepository(artifactStubFactory.getWorkingDir())));
//
//                List<MavenProject> reactorProjects =
//                        mojo.getReactorProjects() != null ? mojo.getReactorProjects() : Collections.emptyList();
//
//                setVariableValueToObject(mojo, "mojoExecution", getMockMojoExecution());
//                setVariableValueToObject(mojo, "session", legacySupport.getSession());
//                setVariableValueToObject(mojo, "repoSession", legacySupport.getRepositorySession());
//                setVariableValueToObject(mojo, "reactorProjects", reactorProjects);
//                setVariableValueToObject(
//                        mojo, "remoteProjectRepositories", mojo.getProject().getRemoteProjectRepositories());
//                setVariableValueToObject(
//                        mojo, "siteDirectory", new File(mojo.getProject().getBasedir(), "src/site"));
//
//                PluginDescriptor descriptorStub = new PluginDescriptor();
//                descriptorStub.setGroupId("org.apache.maven.plugins");
//                descriptorStub.setArtifactId("maven-checkstyle-plugin");
//                setVariableValueToObject(mojo, "plugin", descriptorStub);
//
//                mojo.execute();
//
//                ProjectBuilder builder = lookup(ProjectBuilder.class);
//
//                ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest();
//                buildingRequest.setRepositorySession(lookup(LegacySupport.class).getRepositorySession());
//
//                testMavenProject = builder.build(pluginXmlFile, buildingRequest).getProject();
//
//                File outputDir = mojo.getReportOutputDirectory();
//                String filename = mojo.getOutputName() + ".html";
//
//                File file = new File(outputDir, filename);
//
//                File generatedReport = file;
//                assertTrue(new File(generatedReport.getAbsolutePath()).exists());
//
//                File outputFile = (File) getVariableValueFromObject(mojo, "outputFile");
//                Assertions.assertNotNull(outputFile, "Test output file");
//                Assertions.assertTrue(outputFile.exists(), "Test output file exists");
//
//                String cacheFile = (String) getVariableValueFromObject(mojo, "cacheFile");
//                if (cacheFile != null) {
//                    Assertions.assertTrue(new File(cacheFile).exists(), "Test cache file exists");
//                }
//
//                File useFile = (File) getVariableValueFromObject(mojo, "useFile");
//                if (useFile != null) {
//                    Assertions.assertTrue(useFile.exists(), "Test useFile exists");
//                }
//
//                String str = new String(Files.readAllBytes(generatedReport.toPath()), StandardCharsets.UTF_8);
//
//                boolean searchHeaderFound = str.contains(getHtmlHeader(bundle.getString("report.checkstyle.rules")));
//                Boolean rules = (Boolean) getVariableValueFromObject(mojo, "enableRulesSummary");
//                if (rules) {
//                    Assertions.assertTrue(searchHeaderFound, "Test for Rules Summary");
//                } else {
//                    Assertions.assertFalse(searchHeaderFound, "Test for Rules Summary");
//                }
//
//                searchHeaderFound = str.contains(getHtmlHeader(bundle.getString("report.checkstyle.summary")));
//                Boolean severity = (Boolean) getVariableValueFromObject(mojo, "enableSeveritySummary");
//                if (severity) {
//                    Assertions.assertTrue(searchHeaderFound, "Test for Severity Summary");
//                } else {
//                    Assertions.assertFalse(searchHeaderFound, "Test for Severity Summary");
//                }
//
//                searchHeaderFound = str.contains(getHtmlHeader(bundle.getString("report.checkstyle.files")));
//                Boolean files = (Boolean) getVariableValueFromObject(mojo, "enableFilesSummary");
//                if (files) {
//                    Assertions.assertTrue(searchHeaderFound, "Test for Files Summary");
//                } else {
//                    Assertions.assertFalse(searchHeaderFound, "Test for Files Summary");
//                }
//
//                fail("Must throw exception on errors");
//            } catch (Exception e) {
//                assertNotNull(e.getMessage());
//            }
//        }
//
//        @Test
//        public void testDependencyResolutionException() {
//            try {
//                File pluginXmlFile = new File(
//                        getBasedir(), "src/test/resources/plugin-configs/" +
//     "dep-resolution-exception-plugin-config.xml");
//                ResourceBundle bundle =
//                        ResourceBundle.getBundle("checkstyle-report", SiteTool.DEFAULT_LOCALE, this.getClassLoader());
//                Assertions.assertNotNull(mojo, "Mojo not found.");
//
//                LegacySupport legacySupport = lookup(LegacySupport.class);
//                legacySupport.setSession(newMavenSession(new MavenProjectStub()));
//                DefaultRepositorySystemSession repoSession =
//                        (DefaultRepositorySystemSession) legacySupport.getRepositorySession();
//                repoSession.setLocalRepositoryManager(new SimpleLocalRepositoryManagerFactory()
//                        .newInstance(repoSession, new LocalRepository(artifactStubFactory.getWorkingDir())));
//
//                List<MavenProject> reactorProjects =
//                        mojo.getReactorProjects() != null ? mojo.getReactorProjects() : Collections.emptyList();
//
//                setVariableValueToObject(mojo, "mojoExecution", getMockMojoExecution());
//                setVariableValueToObject(mojo, "session", legacySupport.getSession());
//                setVariableValueToObject(mojo, "repoSession", legacySupport.getRepositorySession());
//                setVariableValueToObject(mojo, "reactorProjects", reactorProjects);
//                setVariableValueToObject(
//                        mojo, "remoteProjectRepositories", mojo.getProject().getRemoteProjectRepositories());
//                setVariableValueToObject(
//                        mojo, "siteDirectory", new File(mojo.getProject().getBasedir(), "src/site"));
//
//                PluginDescriptor descriptorStub = new PluginDescriptor();
//                descriptorStub.setGroupId("org.apache.maven.plugins");
//                descriptorStub.setArtifactId("maven-checkstyle-plugin");
//                setVariableValueToObject(mojo, "plugin", descriptorStub);
//
//                mojo.execute();
//
//                ProjectBuilder builder = lookup(ProjectBuilder.class);
//
//                ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest();
//                buildingRequest.setRepositorySession(lookup(LegacySupport.class).getRepositorySession());
//
//                testMavenProject = builder.build(pluginXmlFile, buildingRequest).getProject();
//
//                File outputDir = mojo.getReportOutputDirectory();
//                String filename = mojo.getOutputName() + ".html";
//
//                File file = new File(outputDir, filename);
//
//                File generatedReport = file;
//                assertTrue(new File(generatedReport.getAbsolutePath()).exists());
//
//                File outputFile = (File) getVariableValueFromObject(mojo, "outputFile");
//                Assertions.assertNotNull(outputFile, "Test output file");
//                Assertions.assertTrue(outputFile.exists(), "Test output file exists");
//
//                String cacheFile = (String) getVariableValueFromObject(mojo, "cacheFile");
//                if (cacheFile != null) {
//                    Assertions.assertTrue(new File(cacheFile).exists(), "Test cache file exists");
//                }
//
//                File useFile = (File) getVariableValueFromObject(mojo, "useFile");
//                if (useFile != null) {
//                    Assertions.assertTrue(useFile.exists(), "Test useFile exists");
//                }
//
//                String str = new String(Files.readAllBytes(generatedReport.toPath()), StandardCharsets.UTF_8);
//
//                boolean searchHeaderFound = str.contains(getHtmlHeader(bundle.getString("report.checkstyle.rules")));
//                Boolean rules = (Boolean) getVariableValueFromObject(mojo, "enableRulesSummary");
//                if (rules) {
//                    Assertions.assertTrue(searchHeaderFound, "Test for Rules Summary");
//                } else {
//                    Assertions.assertFalse(searchHeaderFound, "Test for Rules Summary");
//                }
//
//                searchHeaderFound = str.contains(getHtmlHeader(bundle.getString("report.checkstyle.summary")));
//                Boolean severity = (Boolean) getVariableValueFromObject(mojo, "enableSeveritySummary");
//                if (severity) {
//                    Assertions.assertTrue(searchHeaderFound, "Test for Severity Summary");
//                } else {
//                    Assertions.assertFalse(searchHeaderFound, "Test for Severity Summary");
//                }
//
//                searchHeaderFound = str.contains(getHtmlHeader(bundle.getString("report.checkstyle.files")));
//                Boolean files = (Boolean) getVariableValueFromObject(mojo, "enableFilesSummary");
//                if (files) {
//                    Assertions.assertTrue(searchHeaderFound, "Test for Files Summary");
//                } else {
//                    Assertions.assertFalse(searchHeaderFound, "Test for Files Summary");
//                }
//
//                fail("Must throw exception on errors");
//            } catch (Exception e) {
//                if (!(e.getCause().getCause().getCause() instanceof DependencyResolutionRequiredException)) {
//                    e.printStackTrace();
//                    fail("Must throw exception DependencyResolutionRequiredException on errors and not "
//                            + e.getClass().getName() + ", " + e.getMessage());
//                }
//            }
//        }
//
//        @Test
//        public void testTestSourceDirectory(CheckstyleReport mojo) throws Exception {
//            File pluginXmlFile = new File(
//                    getBasedir(), "src/test/resources/plugin-configs/" + "test-source-directory-plugin-config.xml");
//            ResourceBundle bundle =
//                    ResourceBundle.getBundle("checkstyle-report", SiteTool.DEFAULT_LOCALE, this.getClassLoader());
//            Assertions.assertNotNull(mojo, "Mojo not found.");
//
//            LegacySupport legacySupport = lookup(LegacySupport.class);
//            legacySupport.setSession(newMavenSession(new MavenProjectStub()));
//            DefaultRepositorySystemSession repoSession =
//                    (DefaultRepositorySystemSession) legacySupport.getRepositorySession();
//            repoSession.setLocalRepositoryManager(new SimpleLocalRepositoryManagerFactory()
//                    .newInstance(repoSession, new LocalRepository(artifactStubFactory.getWorkingDir())));
//
//            List<MavenProject> reactorProjects =
//                    mojo.getReactorProjects() != null ? mojo.getReactorProjects() : Collections.emptyList();
//
//            setVariableValueToObject(mojo, "mojoExecution", getMockMojoExecution());
//            setVariableValueToObject(mojo, "session", legacySupport.getSession());
//            setVariableValueToObject(mojo, "repoSession", legacySupport.getRepositorySession());
//            setVariableValueToObject(mojo, "reactorProjects", reactorProjects);
//            setVariableValueToObject(
//                    mojo, "remoteProjectRepositories", mojo.getProject().getRemoteProjectRepositories());
//            setVariableValueToObject(
//                    mojo, "siteDirectory", new File(mojo.getProject().getBasedir(), "src/site"));
//
//            PluginDescriptor descriptorStub = new PluginDescriptor();
//            descriptorStub.setGroupId("org.apache.maven.plugins");
//            descriptorStub.setArtifactId("maven-checkstyle-plugin");
//            setVariableValueToObject(mojo, "plugin", descriptorStub);
//
//            mojo.execute();
//
//            ProjectBuilder builder = lookup(ProjectBuilder.class);
//
//            ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest();
//            buildingRequest.setRepositorySession(lookup(LegacySupport.class).getRepositorySession());
//
//            testMavenProject = builder.build(pluginXmlFile, buildingRequest).getProject();
//
//            File outputDir = mojo.getReportOutputDirectory();
//            String filename = mojo.getOutputName() + ".html";
//
//            File file = new File(outputDir, filename);
//
//            File generatedReport = file;
//            assertTrue(new File(generatedReport.getAbsolutePath()).exists());
//
//            File outputFile = (File) getVariableValueFromObject(mojo, "outputFile");
//            Assertions.assertNotNull(outputFile, "Test output file");
//            Assertions.assertTrue(outputFile.exists(), "Test output file exists");
//
//            String cacheFile = (String) getVariableValueFromObject(mojo, "cacheFile");
//            if (cacheFile != null) {
//                Assertions.assertTrue(new File(cacheFile).exists(), "Test cache file exists");
//            }
//
//            File useFile = (File) getVariableValueFromObject(mojo, "useFile");
//            if (useFile != null) {
//                Assertions.assertTrue(useFile.exists(), "Test useFile exists");
//            }
//
//            String str = new String(Files.readAllBytes(generatedReport.toPath()), StandardCharsets.UTF_8);
//
//            boolean searchHeaderFound = str.contains(getHtmlHeader(bundle.getString("report.checkstyle.rules")));
//            Boolean rules = (Boolean) getVariableValueFromObject(mojo, "enableRulesSummary");
//            if (rules) {
//                Assertions.assertTrue(searchHeaderFound, "Test for Rules Summary");
//            } else {
//                Assertions.assertFalse(searchHeaderFound, "Test for Rules Summary");
//            }
//
//            searchHeaderFound = str.contains(getHtmlHeader(bundle.getString("report.checkstyle.summary")));
//            Boolean severity = (Boolean) getVariableValueFromObject(mojo, "enableSeveritySummary");
//            if (severity) {
//                Assertions.assertTrue(searchHeaderFound, "Test for Severity Summary");
//            } else {
//                Assertions.assertFalse(searchHeaderFound, "Test for Severity Summary");
//            }
//
//            searchHeaderFound = str.contains(getHtmlHeader(bundle.getString("report.checkstyle.files")));
//            Boolean files = (Boolean) getVariableValueFromObject(mojo, "enableFilesSummary");
//            if (files) {
//                Assertions.assertTrue(searchHeaderFound, "Test for Files Summary");
//            } else {
//                Assertions.assertFalse(searchHeaderFound, "Test for Files Summary");
//            }
//        }

    private static String getHtmlHeader(String s) {
        return ">" + s + "</h2>";
    }

}
