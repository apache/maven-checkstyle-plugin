package org.apache.maven.plugins.checkstyle;

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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.plugins.checkstyle.exec.CheckstyleExecutor;
import org.apache.maven.plugins.checkstyle.exec.CheckstyleExecutorException;
import org.apache.maven.plugins.checkstyle.exec.CheckstyleExecutorRequest;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.PathTool;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.MXParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.puppycrawl.tools.checkstyle.DefaultLogger;
import com.puppycrawl.tools.checkstyle.XMLLogger;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import com.puppycrawl.tools.checkstyle.api.AutomaticBean.OutputStreamOptions;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;

/**
 * Performs Checkstyle analysis and outputs violations or a count of violations
 * to the console, potentially failing the build.
 * It can also be configured to re-use an earlier analysis.
 *
 * @author <a href="mailto:joakim@erdfelt.net">Joakim Erdfelt</a>
 *
 */
@Mojo( name = "check", defaultPhase = LifecyclePhase.VERIFY, requiresDependencyResolution = ResolutionScope.NONE,
       threadSafe = true )
public class CheckstyleViolationCheckMojo
    extends AbstractMojo
{

    private static final String JAVA_FILES = "**\\/*.java";
    private static final String DEFAULT_CONFIG_LOCATION = "sun_checks.xml";

    /**
     * Specifies the path and filename to save the Checkstyle output. The format
     * of the output file is determined by the <code>outputFileFormat</code>
     * parameter.
     */
    @Parameter( property = "checkstyle.output.file", defaultValue = "${project.build.directory}/checkstyle-result.xml" )
    private File outputFile;

    /**
     * Specifies the format of the output to be used when writing to the output
     * file. Valid values are "<code>plain</code>" and "<code>xml</code>".
     */
    @Parameter( property = "checkstyle.output.format", defaultValue = "xml" )
    private String outputFileFormat;

    /**
     * Fail the build on a violation. The goal checks for the violations
     * after logging them (if {@link #logViolationsToConsole} is {@code true}).
     * Compare this to {@link #failsOnError} which fails the build immediately
     * before examining the output log.
     */
    @Parameter( property = "checkstyle.failOnViolation", defaultValue = "true" )
    private boolean failOnViolation;

    /**
     * The maximum number of allowed violations. The execution fails only if the
     * number of violations is above this limit.
     *
     * @since 2.3
     */
    @Parameter( property = "checkstyle.maxAllowedViolations", defaultValue = "0" )
    private int maxAllowedViolations;

    /**
     * The lowest severity level that is considered a violation.
     * Valid values are "<code>error</code>", "<code>warning</code>" and "<code>info</code>".
     *
     * @since 2.2
     */
    @Parameter( property = "checkstyle.violationSeverity", defaultValue = "error" )
    private String violationSeverity = "error";

    /**
     * Violations to ignore. This is a comma-separated list, each value being either
     * a rule name, a rule category or a java package name of rule class.
     *
     * @since 2.13
     */
    @Parameter( property = "checkstyle.violation.ignore" )
    private String violationIgnore;

    /**
     * Skip entire check.
     *
     * @since 2.2
     */
    @Parameter( property = "checkstyle.skip", defaultValue = "false" )
    private boolean skip;

    /**
     * Skip Checkstyle execution will only scan the outputFile.
     *
     * @since 2.5
     */
    @Parameter( property = "checkstyle.skipExec", defaultValue = "false" )
    private boolean skipExec;

    /**
     * Output the detected violations to the console.
     *
     * @since 2.3
     */
    @Parameter( property = "checkstyle.console", defaultValue = "true" )
    private boolean logViolationsToConsole;

    /**
     * Output the detected violation count to the console.
     *
     * @since 3.0.1
     */
    @Parameter( property = "checkstyle.logViolationCount", defaultValue = "true" )
    private boolean logViolationCountToConsole;

    /**
     * Specifies the location of the resources to be used for Checkstyle.
     *
     * @since 2.11
     */
    @Parameter( defaultValue = "${project.resources}", readonly = true )
    protected List<Resource> resources;
    
    /**
     * Specifies the location of the test resources to be used for Checkstyle.
     *
     * @since 2.16
     */
    @Parameter( defaultValue = "${project.testResources}", readonly = true )
    protected List<Resource> testResources;

    /**
     * <p>
     * Specifies the location of the XML configuration to use.
     * <p>
     * Potential values are a filesystem path, a URL, or a classpath resource.
     * This parameter expects that the contents of the location conform to the
     * xml format (Checkstyle <a
     * href="http://checkstyle.sourceforge.net/config.html#Modules">Checker
     * module</a>) configuration of rulesets.
     * <p>
     * This parameter is resolved as resource, URL, then file. If successfully
     * resolved, the contents of the configuration is copied into the
     * <code>${project.build.directory}/checkstyle-configuration.xml</code>
     * file before being passed to Checkstyle as a configuration.
     * <p>
     * There are 2 predefined rulesets.
     * <ul>
     * <li><code>sun_checks.xml</code>: Sun Checks.</li>
     * <li><code>google_checks.xml</code>: Google Checks.</li>
     * </ul>
     *
     * @since 2.5
     */
    @Parameter( property = "checkstyle.config.location", defaultValue = DEFAULT_CONFIG_LOCATION )
    private String configLocation;

    /**
     * <p>
     * Specifies the location of the properties file.
     * <p>
     * This parameter is resolved as URL, File then resource. If successfully
     * resolved, the contents of the properties location is copied into the
     * <code>${project.build.directory}/checkstyle-checker.properties</code>
     * file before being passed to Checkstyle for loading.
     * <p>
     * The contents of the <code>propertiesLocation</code> will be made
     * available to Checkstyle for specifying values for parameters within the
     * xml configuration (specified in the <code>configLocation</code>
     * parameter).
     *
     * @since 2.5
     */
    @Parameter( property = "checkstyle.properties.location" )
    private String propertiesLocation;

    /**
     * Allows for specifying raw property expansion information.
     */
    @Parameter
    private String propertyExpansion;

    /**
     * <p>
     * Specifies the location of the License file (a.k.a. the header file) that
     * can be used by Checkstyle to verify that source code has the correct
     * license header.
     * <p>
     * You need to use <code>${checkstyle.header.file}</code> in your Checkstyle xml
     * configuration to reference the name of this header file.
     * <p>
     * For instance:
     * <pre>
     * &lt;module name="RegexpHeader"&gt;
     *   &lt;property name="headerFile" value="${checkstyle.header.file}"/&gt;
     * &lt;/module&gt;
     * </pre>
     *
     * @since 2.0-beta-2
     */
    @Parameter( property = "checkstyle.header.file", defaultValue = "LICENSE.txt" )
    private String headerLocation;

    /**
     * Specifies the cache file used to speed up Checkstyle on successive runs.
     */
    @Parameter( defaultValue = "${project.build.directory}/checkstyle-cachefile" )
    private String cacheFile;

    /**
     * The key to be used in the properties for the suppressions file.
     *
     * @since 2.1
     */
    @Parameter( property = "checkstyle.suppression.expression", defaultValue = "checkstyle.suppressions.file" )
    private String suppressionsFileExpression;

    /**
     * <p>
     * Specifies the location of the suppressions XML file to use.
     * <p>
     * This parameter is resolved as resource, URL, then file. If successfully
     * resolved, the contents of the suppressions XML is copied into the
     * <code>${project.build.directory}/checkstyle-suppressions.xml</code> file
     * before being passed to Checkstyle for loading.
     * <p>
     * See <code>suppressionsFileExpression</code> for the property that will
     * be made available to your Checkstyle configuration.
     *
     * @since 2.0-beta-2
     */
    @Parameter( property = "checkstyle.suppressions.location" )
    private String suppressionsLocation;

    /**
     * The file encoding to use when reading the source files. If the property <code>project.build.sourceEncoding</code>
     * is not set, the platform default encoding is used. <strong>Note:</strong> This parameter always overrides the
     * property <code>charset</code> from Checkstyle's <code>TreeWalker</code> module.
     *
     * @since 2.2
     */
    @Parameter( property = "encoding", defaultValue = "${project.build.sourceEncoding}" )
    private String encoding;

    /**
     * @since 2.5
     */
    @Component( role = CheckstyleExecutor.class, hint = "default" )
    protected CheckstyleExecutor checkstyleExecutor;

    /**
     * Output errors to console.
     */
    @Parameter( property = "checkstyle.consoleOutput", defaultValue = "false" )
    private boolean consoleOutput;

    /**
     * The Maven Project Object.
     */
    @Parameter ( defaultValue = "${project}", readonly = true, required = true )
    protected MavenProject project;
    
    /**
     * The Plugin Descriptor
     */
    @Parameter( defaultValue = "${plugin}", readonly = true, required = true )
    private PluginDescriptor plugin;

    /**
     * If <code>null</code>, the Checkstyle plugin will display violations on stdout.
     * Otherwise, a text file will be created with the violations.
     */
    @Parameter
    private File useFile;

    /**
     * Specifies the names filter of the source files to be excluded for
     * Checkstyle.
     */
    @Parameter( property = "checkstyle.excludes" )
    private String excludes;

    /**
     * Specifies the names filter of the source files to be used for Checkstyle.
     */
    @Parameter( property = "checkstyle.includes", defaultValue = JAVA_FILES, required = true )
    private String includes;

    /**
     * Specifies the names filter of the files to be excluded for
     * Checkstyle when checking resources.
     * @since 2.11
     */
    @Parameter( property = "checkstyle.resourceExcludes" )
    private String resourceExcludes;

    /**
     * Specifies the names filter of the files to be used for Checkstyle when checking resources.
     * @since 2.11
     */
    @Parameter( property = "checkstyle.resourceIncludes", defaultValue = "**/*.properties", required = true )
    private String resourceIncludes;

    /**
     * If this is true, and Checkstyle reported any violations or errors,
     * the build fails immediately after running Checkstyle, before checking the log
     * for {@link #logViolationsToConsole}. If you want to use {@link #logViolationsToConsole},
     * use {@link #failOnViolation} instead of this.
     */
    @Parameter( defaultValue = "false" )
    private boolean failsOnError;

    /**
     * Specifies the location of the test source directory to be used for Checkstyle.
     *
     * @since 2.2
     * @deprecated instead use {@link #testSourceDirectories}. For version 3.0.0, this parameter is only defined to
     *             break the build if you use it!
     */
    @Deprecated
    @Parameter
    private File testSourceDirectory;

    /**
     * Specifies the location of the test source directories to be used for Checkstyle.
     * Default value is <code>${project.testCompileSourceRoots}</code>.
     * @since 2.13
     */
    // Compatibility with all Maven 3: default of 'project.testCompileSourceRoots' is done manually because of MNG-5440
    @Parameter
    private List<String> testSourceDirectories;

    /**
     * Include or not the test source directory to be used for Checkstyle.
     *
     * @since 2.2
     */
    @Parameter( defaultValue = "false" )
    private boolean includeTestSourceDirectory;

    /**
     * Specifies the location of the source directory to be used for Checkstyle.
     * 
     * @deprecated instead use {@link #sourceDirectories}. For version 3.0.0, this parameter is only defined to break
     *             the build if you use it!
     */
    @Deprecated
    @Parameter
    private File sourceDirectory;

    /**
     * Specifies the location of the source directories to be used for Checkstyle.
     * Default value is <code>${project.compileSourceRoots}</code>.
     * @since 2.13
     */
    // Compatibility with all Maven 3: default of 'project.compileSourceRoots' is done manually because of MNG-5440
    @Parameter
    private List<String> sourceDirectories;

    /**
     * Whether to apply Checkstyle to resource directories.
     * @since 2.11
     */
    @Parameter( property = "checkstyle.includeResources", defaultValue = "true", required = true )
    private boolean includeResources = true;

    /**
     * Whether to apply Checkstyle to test resource directories.
     * @since 2.11
     */
    @Parameter( property = "checkstyle.includeTestResources", defaultValue = "true", required = true )
    private boolean includeTestResources = true;

    /**
     * By using this property, you can specify the whole Checkstyle rules
     * inline directly inside this pom.
     *
     * <pre>
     * &lt;plugin&gt;
     *   ...
     *   &lt;configuration&gt;
     *     &lt;checkstyleRules&gt;
     *       &lt;module name="Checker"&gt;
     *         &lt;module name="FileTabCharacter"&gt;
     *           &lt;property name="eachLine" value="true" /&gt;
     *         &lt;/module&gt;
     *         &lt;module name="TreeWalker"&gt;
     *           &lt;module name="EmptyBlock"/&gt;
     *         &lt;/module&gt;
     *       &lt;/module&gt;
     *     &lt;/checkstyleRules&gt;
     *   &lt;/configuration&gt;
     *   ...
     * </pre>
     *
     * @since 2.12
     */
    @Parameter
    private PlexusConfiguration checkstyleRules;

    /**
     * Dump file for inlined Checkstyle rules. 
     */
    @Parameter( property = "checkstyle.output.rules.file",
                    defaultValue = "${project.build.directory}/checkstyle-rules.xml" )
    private File rulesFiles;

    /**
     * The header to use for the inline configuration.
     * Only used when you specify {@code checkstyleRules}.
     */
    @Parameter( defaultValue = "<?xml version=\"1.0\"?>\n"
            + "<!DOCTYPE module PUBLIC \"-//Checkstyle//DTD Checkstyle Configuration 1.3//EN\"\n"
            + "        \"https://checkstyle.org/dtds/configuration_1_3.dtd\">\n" )
    private String checkstyleRulesHeader;

    /**
     * Specifies whether modules with a configured severity of <code>ignore</code> should be omitted during Checkstyle
     * invocation.
     * 
     * @since 3.0.0
     */
    @Parameter( defaultValue = "false" )
    private boolean omitIgnoredModules;

    private ByteArrayOutputStream stringOutputStream;

    private File outputXmlFile;

    /** {@inheritDoc} */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        checkDeprecatedParameterUsage( sourceDirectory, "sourceDirectory", "sourceDirectories" );
        checkDeprecatedParameterUsage( testSourceDirectory, "testSourceDirectory", "testSourceDirectories" );
        if ( skip )
        {
            return;
        }

        outputXmlFile = outputFile;

        if ( !skipExec )
        {
            if ( checkstyleRules != null )
            {
                if ( !DEFAULT_CONFIG_LOCATION.equals( configLocation ) )
                {
                    throw new MojoExecutionException( "If you use inline configuration for rules, don't specify "
                        + "a configLocation" );
                }
                if ( checkstyleRules.getChildCount() > 1 )
                {
                    throw new MojoExecutionException( "Currently only one root module is supported" );
                }

                PlexusConfiguration checkerModule = checkstyleRules.getChild( 0 );

                try
                {
                    FileUtils.forceMkdir( rulesFiles.getParentFile() );
                    FileUtils.fileWrite( rulesFiles, checkstyleRulesHeader + checkerModule.toString() );
                }
                catch ( final IOException e )
                {
                    throw new MojoExecutionException( e.getMessage(), e );
                }
                configLocation = rulesFiles.getAbsolutePath();
            }

            ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();

            try
            {
                CheckstyleExecutorRequest request = new CheckstyleExecutorRequest();
                request.setConsoleListener( getConsoleListener() ).setConsoleOutput( consoleOutput )
                    .setExcludes( excludes ).setFailsOnError( failsOnError ).setIncludes( includes )
                    .setResourceIncludes( resourceIncludes )
                    .setResourceExcludes( resourceExcludes )
                    .setIncludeResources( includeResources )
                    .setIncludeTestResources( includeTestResources )
                    .setIncludeTestSourceDirectory( includeTestSourceDirectory ).setListener( getListener() )
                    .setProject( project ).setSourceDirectories( getSourceDirectories() )
                    .setResources( resources ).setTestResources( testResources )
                    .setStringOutputStream( stringOutputStream ).setSuppressionsLocation( suppressionsLocation )
                    .setTestSourceDirectories( getTestSourceDirectories() ).setConfigLocation( configLocation )
                    .setConfigurationArtifacts( collectArtifacts( "config" ) )
                    .setPropertyExpansion( propertyExpansion )
                    .setHeaderLocation( headerLocation ).setLicenseArtifacts( collectArtifacts( "license" ) )
                    .setCacheFile( cacheFile ).setSuppressionsFileExpression( suppressionsFileExpression )
                    .setEncoding( encoding ).setPropertiesLocation( propertiesLocation )
                    .setOmitIgnoredModules( omitIgnoredModules );
                checkstyleExecutor.executeCheckstyle( request );

            }
            catch ( CheckstyleException e )
            {
                throw new MojoExecutionException( "Failed during checkstyle configuration", e );
            }
            catch ( CheckstyleExecutorException e )
            {
                throw new MojoExecutionException( "Failed during checkstyle execution", e );
            }
            finally
            {
                //be sure to restore original context classloader
                Thread.currentThread().setContextClassLoader( currentClassLoader );
            }
        }

        if ( !"xml".equals( outputFileFormat ) && skipExec )
        {
            throw new MojoExecutionException( "Output format is '" + outputFileFormat
                + "', checkstyle:check requires format to be 'xml' when using skipExec." );
        }

        if ( !outputXmlFile.exists() )
        {
            getLog().info( "Unable to perform checkstyle:check, unable to find checkstyle:checkstyle outputFile." );
            return;
        }

        try ( Reader reader = new BufferedReader( ReaderFactory.newXmlReader( outputXmlFile ) ) )
        {
            XmlPullParser xpp = new MXParser();
            xpp.setInput( reader );

            final List<Violation> violationsList = getViolations( xpp );
            long violationCount = countViolations( violationsList );
            printViolations( violationsList );

            String msg = "You have " + violationCount + " Checkstyle violation"
                + ( ( violationCount > 1 || violationCount == 0 ) ? "s" : "" ) + ".";

            if ( violationCount > maxAllowedViolations )
            {
                if ( failOnViolation )
                {
                    if ( maxAllowedViolations > 0 )
                    {
                        msg += " The maximum number of allowed violations is " + maxAllowedViolations + ".";
                    }
                    throw new MojoFailureException( msg );
                }

                getLog().warn( "checkstyle:check violations detected but failOnViolation set to false" );
            }
            if ( logViolationCountToConsole )
            {
                if ( maxAllowedViolations > 0 )
                {
                  msg += " The maximum number of allowed violations is " + maxAllowedViolations + ".";
                }
                getLog().info( msg );
            }
        }
        catch ( IOException | XmlPullParserException e )
        {
            throw new MojoExecutionException( "Unable to read Checkstyle results xml: "
                + outputXmlFile.getAbsolutePath(), e );
        }
    }

    private void checkDeprecatedParameterUsage( Object parameter, String name, String replacement )
        throws MojoFailureException
    {
        if ( parameter != null )
        {
            throw new MojoFailureException( "You are using '" + name + "' which has been removed"
                + " from the maven-checkstyle-plugin. " + "Please use '" + replacement
                + "' and refer to the >>Major Version Upgrade to version 3.0.0<< " + "on the plugin site." );
        }
    }

    private List<Violation> getViolations( XmlPullParser xpp )
        throws XmlPullParserException, IOException
    {
        List<Violation> violations = new ArrayList<>();

        String basedir = project.getBasedir().getAbsolutePath();
        String file = "";

        for ( int eventType = xpp.getEventType(); eventType != XmlPullParser.END_DOCUMENT; eventType = xpp.next() )
        {
            if ( eventType != XmlPullParser.START_TAG )
            {
                continue;
            }
            else if ( "file".equals( xpp.getName() ) )
            {
                file = PathTool.getRelativeFilePath( basedir, xpp.getAttributeValue( "", "name" ) );
                continue;
            }
            else if ( ! "error".equals( xpp.getName() ) )
            {
                continue;
            }

            String severity = xpp.getAttributeValue( "", "severity" );
            String source = xpp.getAttributeValue( "", "source" );
            String line = xpp.getAttributeValue( "", "line" );
            /* Nullable */
            String column = xpp.getAttributeValue( "", "column" );
            String message = xpp.getAttributeValue( "", "message" );
            String rule = RuleUtil.getName( source );
            String category = RuleUtil.getCategory( source );

            Violation violation = new Violation(
                source,
                file,
                line,
                severity,
                message,
                rule,
                category
            );
            if ( column != null )
            {
                violation.setColumn( column );
            }

            violations.add( violation );
        }

        return violations;
    }

    private int countViolations( List<Violation> violations )
    {
        List<RuleUtil.Matcher> ignores = violationIgnore == null ? Collections.<RuleUtil.Matcher>emptyList()
            : RuleUtil.parseMatchers( violationIgnore.split( "," ) );

        int ignored = 0;
        int countedViolations = 0;

        for ( Violation violation : violations )
        {
            if ( ! isViolation( violation.getSeverity() ) )
            {
                continue;
            }

            if ( ignore( ignores, violation.getSource() ) )
            {
                ignored++;
                continue;
            }

            countedViolations++;
        }

        if ( ignored > 0 )
        {
            getLog().info( "Ignored " + ignored + " error" + ( ( ignored > 1L ) ? "s" : "" ) + ", " + countedViolations
                + " violation" + ( ( countedViolations > 1 ) ? "s" : "" ) + " remaining." );
        }

        return countedViolations;
    }

    private void printViolations( List<Violation> violations )
    {
        if ( ! logViolationsToConsole )
        {
            return;
        }

        List<RuleUtil.Matcher> ignores = violationIgnore == null ? Collections.<RuleUtil.Matcher>emptyList()
            : RuleUtil.parseMatchers( violationIgnore.split( "," ) );

        violations.stream()
            .filter( violation -> isViolation( violation.getSeverity() ) )
            .filter( violation -> !ignore( ignores, violation.getSource() ) )
            .forEach( violation ->
            {
                final String message = String.format( "%s:[%s%s] (%s) %s: %s",
                    violation.getFile(),
                    violation.getLine(),
                    ( Violation.NO_COLUMN.equals( violation.getColumn() ) ) ? "" : ( ',' + violation.getColumn() ),
                    violation.getCategory(),
                    violation.getRuleName(),
                    violation.getMessage() );
                log( violation.getSeverity(), message );
            } );
    }

    private void log( String severity, String message )
    {
        if ( "info".equals( severity ) )
        {
            getLog().info( message );
        }
        else if ( "warning".equals( severity ) )
        {
            getLog().warn( message );
        }
        else
        {
            getLog().error( message );
        }
    }

    /**
     * Checks if the given severity is considered a violation.
     *
     * @param severity The severity to check
     * @return <code>true</code> if the given severity is a violation, otherwise <code>false</code>
     */
    private boolean isViolation( String severity )
    {
        if ( "error".equals( severity ) )
        {
            return "error".equals( violationSeverity ) || "warning".equals( violationSeverity )
                || "info".equals( violationSeverity );
        }
        else if ( "warning".equals( severity ) )
        {
            return "warning".equals( violationSeverity ) || "info".equals( violationSeverity );
        }
        else if ( "info".equals( severity ) )
        {
            return "info".equals( violationSeverity );
        }
        else
        {
            return false;
        }
    }

    private boolean ignore( List<RuleUtil.Matcher> ignores, String source )
    {
        for ( RuleUtil.Matcher ignore : ignores )
        {
            if ( ignore.match( source ) )
            {
                return true;
            }
        }
        return false;
    }

    private DefaultLogger getConsoleListener()
        throws MojoExecutionException
    {
        DefaultLogger consoleListener;

        if ( useFile == null )
        {
            stringOutputStream = new ByteArrayOutputStream();
            consoleListener = new DefaultLogger( stringOutputStream, OutputStreamOptions.NONE );
        }
        else
        {
            OutputStream out = getOutputStream( useFile );

            consoleListener = new DefaultLogger( out, OutputStreamOptions.CLOSE );
        }

        return consoleListener;
    }

    private OutputStream getOutputStream( File file )
        throws MojoExecutionException
    {
        File parentFile = file.getAbsoluteFile().getParentFile();

        if ( !parentFile.exists() )
        {
            parentFile.mkdirs();
        }

        FileOutputStream fileOutputStream;
        try
        {
            fileOutputStream = new FileOutputStream( file );
        }
        catch ( FileNotFoundException e )
        {
            throw new MojoExecutionException( "Unable to create output stream: " + file, e );
        }
        return fileOutputStream;
    }

    private AuditListener getListener()
        throws MojoFailureException, MojoExecutionException
    {
        AuditListener listener = null;

        if ( StringUtils.isNotEmpty( outputFileFormat ) )
        {
            File resultFile = outputFile;

            OutputStream out = getOutputStream( resultFile );

            if ( "xml".equals( outputFileFormat ) )
            {
                listener = new XMLLogger( out, OutputStreamOptions.CLOSE );
            }
            else if ( "plain".equals( outputFileFormat ) )
            {
                try
                {
                    // Write a plain output file to the standard output file,
                    // and write an XML output file to the temp directory that can be used to count violations
                    outputXmlFile = File.createTempFile( "checkstyle-result", ".xml" );
                    outputXmlFile.deleteOnExit();
                    OutputStream xmlOut = getOutputStream( outputXmlFile );
                    CompositeAuditListener compoundListener = new CompositeAuditListener();
                    compoundListener.addListener( new XMLLogger( xmlOut, OutputStreamOptions.CLOSE ) );
                    compoundListener.addListener( new DefaultLogger( out, OutputStreamOptions.CLOSE ) );
                    listener = compoundListener;
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException( "Unable to create temporary file", e );
                }
            }
            else
            {
                throw new MojoFailureException( "Invalid output file format: (" + outputFileFormat
                    + "). Must be 'plain' or 'xml'." );
            }
        }

        return listener;
    }
    
    private List<Artifact> collectArtifacts( String hint )
    {
        List<Artifact> artifacts = new ArrayList<>();

        PluginManagement pluginManagement = project.getBuild().getPluginManagement();
        if ( pluginManagement != null )
        {
            artifacts.addAll( getCheckstylePluginDependenciesAsArtifacts( pluginManagement.getPluginsAsMap(), hint ) );
        }

        artifacts.addAll( getCheckstylePluginDependenciesAsArtifacts( project.getBuild().getPluginsAsMap(), hint ) );

        return artifacts;
    }

    private List<Artifact> getCheckstylePluginDependenciesAsArtifacts( Map<String, Plugin> plugins, String hint )
    {
        List<Artifact> artifacts = new ArrayList<>();
        
        Plugin checkstylePlugin = plugins.get( plugin.getGroupId() + ":" + plugin.getArtifactId() );
        if ( checkstylePlugin != null )
        {
            for ( Dependency dep : checkstylePlugin.getDependencies() )
            {
             // @todo if we can filter on hints, it should be done here...
                String depKey = dep.getGroupId() + ":" + dep.getArtifactId();
                artifacts.add( plugin.getArtifactMap().get( depKey ) );
            }
        }
        return artifacts;
    }
    
    private List<File> getSourceDirectories()
    {
        if ( sourceDirectories == null )
        {
            sourceDirectories = project.getCompileSourceRoots();
        }
        List<File> sourceDirs = new ArrayList<>( sourceDirectories.size() );
        for ( String sourceDir : sourceDirectories )
        {
            sourceDirs.add( FileUtils.resolveFile( project.getBasedir(), sourceDir ) );
        }
        return sourceDirs;
    }
    
    private List<File> getTestSourceDirectories()
    {
        if ( testSourceDirectories == null )
        {
            testSourceDirectories = project.getTestCompileSourceRoots();
        }
        List<File> testSourceDirs = new ArrayList<>( testSourceDirectories.size() );
        for ( String testSourceDir : testSourceDirectories )
        {
            testSourceDirs.add( FileUtils.resolveFile( project.getBasedir(), testSourceDir ) );
        }
        return testSourceDirs;
    }
    
}
