---
title: Frequently Asked Questions
---

<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

<a id="top"></a>

# Frequently Asked Questions

1. [Where is the `config/maven_checks.xml` predefined ruleset?](#Where_is_maven_checks.xml_predefined_ruleset)
1. [How are the Checkstyle properties set?](#How_are_the_Checkstyle_properties_set)
1. [How do I set a custom ruleset?](#How_do_I_set_a_custom_ruleset)
1. [How do I include the test directory in Checkstyle?](#How_do_I_include_the_test_directory_in_Checkstyle)
1. [What is the difference between checkstyle:checkstyle and checkstyle:check?](#What_is_the_difference_between_checkstyle.3Acheckstyle_and_checkstyle.3Acheck)

<a id="Where_is_maven_checks.xml_predefined_ruleset"></a>

### Where is the `config/maven_checks.xml` predefined ruleset?

Version 2.14 removed `config/maven_checks.xml` from the plugin.
The ruleset is now in [Apache Maven Shared Resources](/shared/maven-shared-resources/).
The plugin ships `sun_checks.xml` and `google_checks.xml`; the [Predefined Rulesets](./config/index.html) page lists the rulesets of each plugin version.

<a id="How_are_the_Checkstyle_properties_set"></a>

### How are the Checkstyle properties set?

Set them with the `propertyExpansion` parameter, or put them in a properties file and point the `propertiesLocation` parameter to it.
See [Custom Checkstyle Property Expansion Definitions](./examples/custom-property-expansion.html).

<a id="How_do_I_set_a_custom_ruleset"></a>

### How do I set a custom ruleset?

Set the `configLocation` parameter to a file, a URL, or a resource on the plugin classpath.
If you do not set it, the plugin uses the `sun_checks.xml` ruleset that ships with Checkstyle.
See [Custom Checkstyle Checker Configuration](./examples/custom-checker-config.html).

<a id="How_do_I_include_the_test_directory_in_Checkstyle"></a>

### How do I include the test directory in Checkstyle?

Set the `includeTestSourceDirectory` parameter to `true`.
It is `false` by default, for both the `check` goal and the reports.

<a id="What_is_the_difference_between_checkstyle:checkstyle_and_checkstyle:check"></a>
<a id="What_is_the_difference_between_checkstyle.3Acheckstyle_and_checkstyle.3Acheck"></a>

### What is the difference between checkstyle:checkstyle and checkstyle:check?

`checkstyle:checkstyle` is a reporting goal.
It writes an HTML report of Checkstyle violations, as part of the site or on its own.
It does not fail the build unless you set `failsOnError` to `true`.
`checkstyle:check` logs violations to the console and fails the build when there are violations.
