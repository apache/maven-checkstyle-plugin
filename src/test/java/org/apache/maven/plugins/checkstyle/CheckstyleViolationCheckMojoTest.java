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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.util.Arrays;

import org.apache.maven.model.Build;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;

/**
 * @author Edwin Punzalan
 *
 */
public class CheckstyleViolationCheckMojoTest
    extends AbstractMojoTestCase
{
    
    
    
    public void testDefaultConfig()
        throws Exception
    {
        File pluginXmlFile = new File( getBasedir(), "src/test/plugin-configs/check-plugin-config.xml" );

        CheckstyleViolationCheckMojo mojo = (CheckstyleViolationCheckMojo) lookupMojo( "check", pluginXmlFile );
        
        mojoSetup( mojo );
        
        assertNotNull( "Mojo found.", mojo );
        
        assertNotNull( "project null.", mojo.project );
        
        try
        {
            mojo.execute();

            fail( "Must throw an exception on violations" );
        }
        catch ( MojoFailureException e )
        {
            //expected
        }
    }

    public void testInvalidFormatWithSkipExec()
        throws Exception
    {
        File pluginXmlFile = new File( getBasedir(), "src/test/plugin-configs/check-plugin-config.xml" );

        Mojo mojo = lookupMojo( "check", pluginXmlFile );

        assertNotNull( "Mojo found.", mojo );

        mojoSetup( mojo );
        
        setVariableValueToObject( mojo, "outputFileFormat", "plain" );

        try
        {
            mojo.execute();

            fail( "Must throw an exception invalid format: plain" );
        }
        catch ( MojoExecutionException e )
        {
            //expected
        }
    }

    public void testNoOutputFile()
        throws Exception
    {
        File pluginXmlFile = new File( getBasedir(), "src/test/plugin-configs/check-plugin-config.xml" );

        Mojo mojo = lookupMojo( "check", pluginXmlFile );

        assertNotNull( "Mojo found.", mojo );

        mojoSetup( mojo );
        
        setVariableValueToObject( mojo, "outputFile", new File( "target/NoSuchFile.xml" ) );

        mojo.execute();
    }

    private void doTestPlainOutputFile( boolean failsOnError )
        throws Exception
    {
        File pluginXmlFile = new File( getBasedir(), "src/test/plugin-configs/check-plugin-plain-output.xml" );

        Mojo mojo = lookupMojo( "check", pluginXmlFile );

        assertNotNull( "Mojo found.", mojo );

        PluginDescriptor descriptorStub = new PluginDescriptor();
        descriptorStub.setGroupId( "org.apache.maven.plugins" );
        descriptorStub.setArtifactId( "maven-checkstyle-plugin" );
        setVariableValueToObject( mojo, "plugin", descriptorStub );

        setVariableValueToObject( mojo, "failsOnError", failsOnError );

        mojo.execute();
    }

    public void testPlainOutputFileFailOnError()
        throws Exception
    {
        try
        {
            doTestPlainOutputFile( true );

            fail( "Must fail on violations" );
        }
        catch ( MojoExecutionException e )
        {
            // expected
        }
    }

    public void testPlainOutputFile()
        throws Exception
    {
        doTestPlainOutputFile( false );
    }

    public void testNoFail()
        throws Exception
    {
        File pluginXmlFile = new File( getBasedir(), "src/test/plugin-configs/check-plugin-config.xml" );

        Mojo mojo = lookupMojo( "check", pluginXmlFile );

        assertNotNull( "Mojo found.", mojo );

        mojoSetup( mojo );
        
        setVariableValueToObject( mojo, "failOnViolation", Boolean.FALSE );

        mojo.execute();
    }
    
    protected void mojoSetup( Mojo mojo )
        throws Exception
    {
        // mojo setup

        setVariableValueToObject( mojo, "project", new MavenProjectStub()
        {

            public File getFile()
            {
                return new File( getBasedir(), "target/classes" );
            }

            public Build getBuild()
            {
                return new Build()
                {
                    private static final long serialVersionUID = -743084937617131258L;

                    public String getDirectory()
                    {
                        return getBasedir() + "/target/classes";
                    }
                };
            }

        } );

        setVariableValueToObject( mojo, "configLocation", "sun_checks.xml" );
        setVariableValueToObject( mojo, "cacheFile", getBasedir() + "/target/classes/checkstyle-cachefile" );
        setVariableValueToObject( mojo, "sourceDirectories", Arrays.asList( getBasedir() + "/src/test/plugin-configs/src" ));// new File( getBasedir() + "/target" ) );
        setVariableValueToObject( mojo, "encoding", "UTF-8" );
        setVariableValueToObject( mojo, "skipExec", Boolean.TRUE );

    }
}
