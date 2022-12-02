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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ResourceBundle;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.doxia.tools.SiteTool;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.codehaus.plexus.util.FileUtils;

/**
 * @author Edwin Punzalan
 *
 */
public class CheckstyleReportTest extends AbstractCheckstyleTestCase {
    public void testNoSource() throws Exception {
        File generatedReport = generateReport(getGoal(), "no-source-plugin-config.xml");
        assertFalse(FileUtils.fileExists(generatedReport.getAbsolutePath()));
    }

    public void testMinConfiguration() throws Exception {
        generateReport("min-plugin-config.xml");
    }

    public void testCustomConfiguration() throws Exception {
        generateReport("custom-plugin-config.xml");
    }

    public void testUseFile() throws Exception {
        generateReport("useFile-plugin-config.xml");
    }

    public void testNoRulesSummary() throws Exception {
        generateReport("no-rules-plugin-config.xml");
    }

    public void testNoSeveritySummary() throws Exception {
        generateReport("no-severity-plugin-config.xml");
    }

    public void testNoFilesSummary() throws Exception {
        generateReport("no-files-plugin-config.xml");
    }

    public void testFailOnError() {
        try {
            generateReport("fail-on-error-plugin-config.xml");

            fail("Must throw exception on errors");
        } catch (Exception e) {
            // expected
        }
    }

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

    public void testTestSourceDirectory() throws Exception {
        generateReport("test-source-directory-plugin-config.xml");
    }

    /**
     * Read the contents of the specified file object into a string
     *
     * @param file the file to be read
     * @return a String object that contains the contents of the file
     * @throws java.io.IOException
     */
    private String readFile(File file) throws IOException {
        String strTmp;
        StringBuilder str = new StringBuilder((int) file.length());
        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            while ((strTmp = in.readLine()) != null) {
                str.append(' ');
                str.append(strTmp);
            }
        }

        return str.toString();
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
        assertTrue(FileUtils.fileExists(generatedReport.getAbsolutePath()));

        File outputFile = (File) getVariableValueFromObject(mojo, "outputFile");
        assertNotNull("Test output file", outputFile);
        assertTrue("Test output file exists", outputFile.exists());

        String cacheFile = (String) getVariableValueFromObject(mojo, "cacheFile");
        if (cacheFile != null) {
            assertTrue("Test cache file exists", new File(cacheFile).exists());
        }

        File outputDir = mojo.getReportOutputDirectory();

        File useFile = (File) getVariableValueFromObject(mojo, "useFile");
        if (useFile != null) {
            assertTrue("Test useFile exists", useFile.exists());
        }

        String str = readFile(generatedReport);

        boolean searchHeaderFound = str.contains(getHtmlHeader(bundle.getString("report.checkstyle.rules")));
        Boolean rules = (Boolean) getVariableValueFromObject(mojo, "enableRulesSummary");
        if (rules) {
            assertTrue("Test for Rules Summary", searchHeaderFound);
        } else {
            assertFalse("Test for Rules Summary", searchHeaderFound);
        }

        searchHeaderFound = str.contains(getHtmlHeader(bundle.getString("report.checkstyle.summary")));
        Boolean severity = (Boolean) getVariableValueFromObject(mojo, "enableSeveritySummary");
        if (severity) {
            assertTrue("Test for Severity Summary", searchHeaderFound);
        } else {
            assertFalse("Test for Severity Summary", searchHeaderFound);
        }

        searchHeaderFound = str.contains(getHtmlHeader(bundle.getString("report.checkstyle.files")));
        Boolean files = (Boolean) getVariableValueFromObject(mojo, "enableFilesSummary");
        if (files) {
            assertTrue("Test for Files Summary", searchHeaderFound);
        } else {
            assertFalse("Test for Files Summary", searchHeaderFound);
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
