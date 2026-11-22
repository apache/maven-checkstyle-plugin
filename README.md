<!---
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->

# Apache Maven Checkstyle Plugin

[![Apache License, Version 2.0, January 2004](https://img.shields.io/github/license/apache/maven.svg?label=License)](https://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/org.apache.maven.plugins/maven-checkstyle-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/org.apache.maven.plugins/maven-checkstyle-plugin)
[![Reproducible Builds](https://img.shields.io/endpoint?url=https://raw.githubusercontent.com/jvm-repo-rebuild/reproducible-central/master/content/org/apache/maven/plugins/maven-checkstyle-plugin/badge.json)](https://github.com/jvm-repo-rebuild/reproducible-central/blob/master/content/org/apache/maven/plugins/maven-checkstyle-plugin/README.md)
[![Verify](https://github.com/apache/maven-checkstyle-plugin/actions/workflows/maven-verify.yml/badge.svg)](https://github.com/apache/maven-checkstyle-plugin/actions/workflows/maven-verify.yml)

Runs [Checkstyle](https://checkstyle.org/) on a Maven project. The `check` goal
logs violations and fails the build; the `checkstyle` and `checkstyle-aggregate`
goals write an HTML report, on their own or as part of the project site.

```shell
mvn checkstyle:check
mvn checkstyle:checkstyle
```

The plugin requires Maven 3.9.0 and Java 8, and runs Checkstyle 9.3 by default.
Projects can [use a newer Checkstyle](https://maven.apache.org/plugins/maven-checkstyle-plugin/examples/upgrading-checkstyle.html)
by adding it as a plugin dependency.

- Documentation: <https://maven.apache.org/plugins/maven-checkstyle-plugin/>
- Issues: <https://github.com/apache/maven-checkstyle-plugin/issues>
- Security reports: see [SECURITY.md](SECURITY.md); never a public issue
- Agents and automated tools: see [AGENTS.md](AGENTS.md)

## Build

```shell
mvn verify
```

Run the integration tests in `src/it` with the `run-its` profile:

```shell
mvn -Prun-its verify
```

Build the plugin site, including the goal pages, with:

```shell
mvn install -DskipTests
mvn site -Preporting
```

## Contributing

Open a pull request against `master`. Discuss larger changes on the
[developer mailing list](https://maven.apache.org/mailing-lists.html) first. See the
[guide to helping with Maven](https://maven.apache.org/guides/development/guide-helping.html)
for the general process.
