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
package org.apache.maven.plugins.checkstyle.exec;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that the files selected for checking honor the standard default excludes (for example files below a
 * {@code CVS} or {@code .svn} directory), in addition to any user supplied excludes.
 */
class DefaultCheckstyleExecutorTest {

    @TempDir
    File sourceDirectory;

    @Test
    void testDefaultExcludesAreApplied() throws Exception {
        File included = new File(sourceDirectory, "Included.java");
        writeFile(included, "public class Included {}");

        File cvs = new File(sourceDirectory, "CVS");
        assertTrue(cvs.mkdirs(), "Unable to create " + cvs);
        File excluded = new File(cvs, "Excluded.java");
        writeFile(excluded, "public class Excluded {}");

        List<String> names = getFilesToProcess(null).stream().map(File::getName).collect(Collectors.toList());

        assertTrue(names.contains("Included.java"), "The regular source file should be checked: " + names);
        assertFalse(
                names.contains("Excluded.java"),
                "A file below a default-excluded directory (CVS) must not be checked: " + names);
    }

    @Test
    void testUserExcludesAreCombinedWithDefaultExcludes() throws Exception {
        File excludedByUser = new File(sourceDirectory, "ExcludedByUser.java");
        writeFile(excludedByUser, "public class ExcludedByUser {}");

        File included = new File(sourceDirectory, "Included.java");
        writeFile(included, "public class Included {}");

        File cvs = new File(sourceDirectory, "CVS");
        assertTrue(cvs.mkdirs(), "Unable to create " + cvs);
        File excludedByDefault = new File(cvs, "ExcludedByDefault.java");
        writeFile(excludedByDefault, "public class ExcludedByDefault {}");

        List<String> names = getFilesToProcess("**/ExcludedByUser.java").stream()
                .map(File::getName)
                .collect(Collectors.toList());

        assertTrue(names.contains("Included.java"), "The regular source file should be checked: " + names);
        assertFalse(names.contains("ExcludedByUser.java"), "The user excluded file must not be checked: " + names);
        assertFalse(
                names.contains("ExcludedByDefault.java"),
                "A file below a default-excluded directory (CVS) must not be checked: " + names);
    }

    private static void writeFile(File file, String content) throws IOException {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(content);
        }
    }

    /**
     * Calls {@code getFilesToProcess}, where the default excludes are applied.
     */
    private List<File> getFilesToProcess(String excludes) throws Exception {
        CheckstyleExecutorRequest request = new CheckstyleExecutorRequest();
        request.setIncludes("**/*.java");
        request.setExcludes(excludes);
        request.setSourceDirectories(Collections.singletonList(sourceDirectory));

        DefaultCheckstyleExecutor executor = new DefaultCheckstyleExecutor(null, null);
        return executor.getFilesToProcess(request);
    }
}
