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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ResourceBundle;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.doxia.tools.SiteTool;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Edwin Punzalan
 */
public class CheckstyleReportTest extends AbstractCheckstyleTestCase {

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
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

    @Override
    protected String getGoal() {
        return "checkstyle";
    }
}
