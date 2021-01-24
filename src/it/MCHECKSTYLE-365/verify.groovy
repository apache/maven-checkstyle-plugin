
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
def htmlReportFile = new File(basedir, 'target/site/checkstyle.html');
assert htmlReportFile.exists();

/*
 * These two test cases will both return 3 if the bug isn't fixed
 * (1 for the violation, and 1 each for the 2 rules).
 *
 * If fixed, the count should be 2 (1 for the violation, and 1 for
 * the Rule summary)
 *
 * Note that the angle-brackets at either end are to count only
 * the user visible text as opposed to the occurances in the URLs.
 *
 * A more robust solution would be to parse the HTML, but that's
 * a lot more effort than required to actually verify the behaviour.
 *
 * TODO: The original fix of MCHECKSTYLE-365 does not take into account when 'error'
 * type doesn't exist and therefore breaks 'rules' aggregate reporting. As a result, counts
 * set back to 3 and code is reverted. A better fix needs to be implemented.
 */

// Case with no custom messages
def numberOfOccurancesOfFileTabCharacter = htmlReportFile.text.count(">FileTabCharacter<")
assert 3 == numberOfOccurancesOfFileTabCharacter;

// Case with custom messages
def numberOfOccurancesOfRegexpSingleline = htmlReportFile.text.count(">RegexpSingleline<");
assert 3 == numberOfOccurancesOfRegexpSingleline;

return true;