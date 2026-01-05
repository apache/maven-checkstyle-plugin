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

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.plugins.checkstyle.exec.CheckstyleExecutor;
import org.apache.maven.plugins.checkstyle.exec.CheckstyleExecutorRequest;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.resource.ResourceManager;

/**
 * A reporting task that performs Checkstyle analysis and generates an aggregate
 * HTML report on the violations that Checkstyle finds in a multi-module reactor
 * build.
 */
@Mojo(
        name = "checkstyle-aggregate",
        aggregator = true,
        requiresDependencyResolution = ResolutionScope.COMPILE,
        threadSafe = true)
public class CheckstyleAggregateReport extends AbstractCheckstyleReport {

    @Inject
    public CheckstyleAggregateReport(
            ResourceManager locator, @Named("default") CheckstyleExecutor checkstyleExecutor, I18N i18n) {
        super(locator, checkstyleExecutor, i18n);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected CheckstyleExecutorRequest createRequest() throws MavenReportException {
        CheckstyleExecutorRequest request = new CheckstyleExecutorRequest();
        request.setAggregate(true)
                .setReactorProjects(reactorProjects)
                .setConsoleListener(getConsoleListener())
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

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOutputName() {
        return "checkstyle-aggregate";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canGenerateReport() {
        // TODO: would be good to scan the files here
        return !skip && project.isExecutionRoot() && reactorProjects.size() > 1;
    }
}
