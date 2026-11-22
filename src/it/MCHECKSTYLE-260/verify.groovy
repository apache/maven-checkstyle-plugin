
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
def buildLog = new File( basedir, 'build.log' ).text

// 3 errors would be in child-a/src/generated/java and child-b/src/generated/java
// but both locations shouldn't be checked
assert !buildLog.contains( "[INFO] There are 3 errors reported by Checkstyle" )

// the check ran in the parent (pom packaging, counted as one of the three) and in both children: child-a with
// sourceDirectories, child-b with testSourceDirectories
assert buildLog.count( "[INFO] You have 0 Checkstyle violations." ) == 3
