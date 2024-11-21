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
import javax.inject.Named;

import java.io.File;
import java.util.List;

import org.apache.maven.model.Resource;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.plugins.checkstyle.exec.CheckstyleExecutor;
import org.apache.maven.plugins.checkstyle.exec.CheckstyleExecutorRequest;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.resource.ResourceManager;

/**
 * A reporting task that performs Checkstyle analysis and generates an HTML
 * report on any violations that Checkstyle finds.
 *
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 *
 */
@Mojo(name = "checkstyle", requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
public class CheckstyleReport extends AbstractCheckstyleReport {

    @Inject
    public CheckstyleReport(
            ResourceManager locator, @Named("default") CheckstyleExecutor checkstyleExecutor, I18N i18n) {
        super(locator, checkstyleExecutor, i18n);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected CheckstyleExecutorRequest createRequest() throws MavenReportException {
        CheckstyleExecutorRequest request = new CheckstyleExecutorRequest();
        request.setConsoleListener(getConsoleListener())
                .setConsoleOutput(consoleOutput)
                .setExcludes(excludes)
                .setFailsOnError(failsOnError)
                .setIncludes(includes)
                .setResourceIncludes(resourceIncludes)
                .setResourceExcludes(resourceExcludes)
                .setIncludeResources(includeResources)
                .setIncludeTestResources(includeTestResources)
                .setIncludeTestSourceDirectory(includeTestSourceDirectory)
                .setListener(getListener())
                .setProject(project)
                .setSourceDirectories(getSourceDirectories())
                .setResources(resources)
                .setTestResources(testResources)
                .setSuppressionsLocation(suppressionsLocation)
                .setTestSourceDirectories(getTestSourceDirectories())
                .setPropertyExpansion(propertyExpansion)
                .setHeaderLocation(headerLocation)
                .setCacheFile(cacheFile)
                .setSuppressionsFileExpression(suppressionsFileExpression)
                .setEncoding(getInputEncoding())
                .setPropertiesLocation(propertiesLocation);
        return request;
    }

    /** {@inheritDoc} */
    @Override
    public String getOutputName() {
        return "checkstyle";
    }

    /** {@inheritDoc} */
    @Override
    public boolean canGenerateReport() {
        if (skip) {
            return false;
        }

        // TODO: would be good to scan the files here
        for (File sourceDirectory : getSourceDirectories()) {
            if (sourceDirectory.exists()) {
                return true;
            }
        }

        if (includeTestSourceDirectory) {
            for (File testSourceDirectory : getTestSourceDirectories()) {
                if (testSourceDirectory.exists()) {
                    return true;
                }
            }
        }

        return ((includeResources && hasResources(resources)) || (includeTestResources && hasResources(testResources)));
    }

    /**
     * Check if any of the resources exist.
     * @param resources The resources to check
     * @return <code>true</code> if the resource directory exist
     */
    private boolean hasResources(List<Resource> resources) {
        for (Resource resource : resources) {
            if (new File(resource.getDirectory()).exists()) {
                return true;
            }
        }
        return false;
    }
}
