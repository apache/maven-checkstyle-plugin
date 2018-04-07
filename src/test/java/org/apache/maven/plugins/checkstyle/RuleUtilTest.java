package org.apache.maven.plugins.checkstyle;

import java.util.List;

import junit.framework.TestCase;

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

public class RuleUtilTest
    extends TestCase
{
    private static final String CHECKSTYLE_PACKAGE = "com.puppycrawl.tools.checkstyle.checks";

    public void testGetName()
    {
        assertEquals( "FinalParameters", RuleUtil.getName( CHECKSTYLE_PACKAGE + ".FinalParameters" ) );
        assertEquals( "FinalParameters", RuleUtil.getName( CHECKSTYLE_PACKAGE + ".FinalParametersCheck" ) );
        assertNull( RuleUtil.getName( (String) null ) );
    }

    public void testGetCategory()
    {
        assertEquals( "misc", RuleUtil.getCategory( CHECKSTYLE_PACKAGE + ".FinalParametersCheck" ) );
        assertEquals( "test", RuleUtil.getCategory( CHECKSTYLE_PACKAGE + ".test.FinalParametersCheck" ) );
        assertEquals( "extension", RuleUtil.getCategory( "test.FinalParametersCheck" ) );
        assertEquals( "extension", RuleUtil.getCategory( "copyright" ) );
        assertNull( RuleUtil.getCategory( (String) null ) );
    }

    public void testMatcher()
    {
        String[] specs = ( "misc, test, extension, Header, " + CHECKSTYLE_PACKAGE + ".test2" ).split( "," );
        String[] eventSrcNames =
            new String[] { CHECKSTYLE_PACKAGE + ".FinalParametersCheck",
                CHECKSTYLE_PACKAGE + ".test.FinalParametersCheck", "test.FinalParametersCheck",
                CHECKSTYLE_PACKAGE + ".whitespace.HeaderCheck", CHECKSTYLE_PACKAGE + ".test2.FinalParametersCheck" };

        List<RuleUtil.Matcher> matchers = RuleUtil.parseMatchers( specs );

        for ( int i = 0; i < matchers.size(); i++ )
        {
            String spec = specs[i];
            RuleUtil.Matcher matcher = matchers.get( i );
            for ( int j = 0; j < matchers.size(); j++ )
            {
                String eventSrcName = eventSrcNames[j];
                assertEquals( spec + " should" + ( ( i == j ) ? " " : " not " ) + "match " + eventSrcName, i == j,
                              matcher.match( eventSrcName ) );
            }
        }
    }

    public void testMatcherWithBlankStrings()
    {
        String[] specs = ( "   ,,foo, " ).split( "," );

        List<RuleUtil.Matcher> matchers = RuleUtil.parseMatchers( specs );

        assertEquals( 1, matchers.size() );
        assertTrue( matchers.get( 0 ).match( CHECKSTYLE_PACKAGE + ".foo.SomeCheck" ) );
    }
}
