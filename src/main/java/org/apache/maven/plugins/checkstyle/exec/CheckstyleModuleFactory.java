package org.apache.maven.plugins.checkstyle.exec;

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

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.puppycrawl.tools.checkstyle.PackageObjectFactory;
import com.puppycrawl.tools.checkstyle.api.AbstractViolationReporter;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;

/**
 * Extension of the package object factory, which remembers the check class name from a given name.
 *
 * @author gboue
 */
public class CheckstyleModuleFactory
    extends PackageObjectFactory
{

    private Map<String, String> moduleNameMap = new ConcurrentHashMap<>();

    public CheckstyleModuleFactory( Set<String> packageNames, ClassLoader moduleClassLoader )
    {
        super( packageNames, moduleClassLoader );
    }

    @Override
    public Object createModule( String name )
        throws CheckstyleException
    {
        Object module = super.createModule( name );
        if ( module instanceof AbstractViolationReporter )
        {
            moduleNameMap.put( name, module.getClass().getName() );
        }
        return module;
    }

    public Map<String, String> getModuleNameMap()
    {
        return Collections.unmodifiableMap( moduleNameMap );
    }

}
