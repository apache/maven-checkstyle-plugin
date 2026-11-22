
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

// default xrefTestLocation
def defaultReport = new File( basedir, 'target/reports-default/checkstyle.html' )
assert defaultReport.exists()
assert defaultReport.text.contains( '<a href="./xref/com/example/App.html#L26">' )
assert defaultReport.text.contains( '<a href="./xref-test/com/example/AppTest.html#L23">' )

// custom xrefTestLocation
def customReport = new File( basedir, 'target/reports-custom/checkstyle.html' )
assert customReport.exists()
assert customReport.text.contains( '<a href="./xref/com/example/App.html#L26">' )
assert customReport.text.contains( '<a href="./custom-xref-test-location/com/example/AppTest.html#L23">' )
