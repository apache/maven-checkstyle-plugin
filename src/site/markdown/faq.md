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

<a name="top"></a>

# Frequently Asked Questions

1. [Where is `config/maven_checks.xml` predefined ruleset?](#Where_is_maven_checks.xml_predefined_ruleset)
1. [How are the Checkstyle properties set?](#How_are_the_Checkstyle_properties_set)
1. [How do I set a custom ruleset?](#How_do_I_set_a_custom_ruleset)
1. [How do I include the test directory in Checkstyle?](#How_do_I_include_the_test_directory_in_Checkstyle)
1. [What is the difference between checkstyle:checkstyle and checkstyle:check?](#What_is_the_difference_between_checkstyle.3Acheckstyle_and_checkstyle.3Acheck)

<a name="Where_is_maven_checks.xml_predefined_ruleset"></a>

### Where is `config/maven_checks.xml` predefined ruleset?

Starting with maven-checkstyle-plugin version 2.14, `config/maven_checks.xml`
predefined ruleset is not part of the plugin any more but has moved to
[Apache Maven Shared Resources](/shared/maven-shared-resources/).

<a name="How_are_the_Checkstyle_properties_set"></a>

### How are the Checkstyle properties set?

You can set the Checkstyle properties to be used in the plugin configuration of your
POM through the `propertiesLocation` parameter. The properties file will be resolved
by the plugin based on its value.

<a name="How_do_I_set_a_custom_ruleset"></a>

### How do I set a custom ruleset?

You can set a custom ruleset through the `configLocation` plugin parameter. If no
value is specified, the plugin will use a default ruleset, which is the
`sun_checks.xml`, that is bundled with the plugin.

<a name="How_do_I_include_the_test_directory_in_Checkstyle"></a>

### How do I include the test directory in Checkstyle?

You can include the test directory in the Checkstyle report by setting the
`includeTestSourceDirectory` plugin parameter to `true`.

<a name="What_is_the_difference_between_checkstyle.3Acheckstyle_and_checkstyle.3Acheck"></a>

### What is the difference between checkstyle:checkstyle and checkstyle:check?

The checkstyle:checkstyle goal is a **reporting** goal that adds a report of
Checkstyle violations to the output of the maven-site-plugin. The checkstyle:check
goal is an ordinary goal that reports violations to the console and/or fails the
build when there are violations.
