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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.SeverityLevel;
import com.puppycrawl.tools.checkstyle.api.Violation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Edwin Punzalan
 *
 */
public class CheckstyleResultsTest {
    private CheckstyleResults results = new CheckstyleResults();

    @Test
    public void testEmptyResults() {
        assertEquals(0, results.getFiles().size(), "test total files");

        assertEquals(0, results.getFileCount(), "test file count");

        assertEquals(0, results.getFileViolations("filename").size(), "test zero file violations");

        assertEquals(0, results.getSeverityCount(SeverityLevel.INFO), "test INFO severity count");

        assertEquals(0, results.getSeverityCount(SeverityLevel.WARNING), "test WARNING severity count");

        assertEquals(0, results.getSeverityCount(SeverityLevel.ERROR), "test ERROR severity count");

        assertEquals(0, results.getSeverityCount(SeverityLevel.IGNORE), "test IGNORE severity count");
    }

    @Test
    public void testResults() {
        Map<String, List<AuditEvent>> files = new HashMap<>();

        Violation message = new Violation(0, 0, "", "", null, SeverityLevel.INFO, null, getClass(), null);
        AuditEvent event = new AuditEvent(this, "file1", message);
        files.put("file1", Collections.singletonList(event));

        message = new Violation(0, 0, "", "", null, SeverityLevel.WARNING, null, getClass(), null);
        List<AuditEvent> events = new ArrayList<>();
        events.add(new AuditEvent(this, "file2", message));
        events.add(new AuditEvent(this, "file2", message));
        files.put("file2", events);

        message = new Violation(0, 0, "", "", null, SeverityLevel.ERROR, null, getClass(), null);
        events = new ArrayList<>();
        events.add(new AuditEvent(this, "file3", message));
        events.add(new AuditEvent(this, "file3", message));
        events.add(new AuditEvent(this, "file3", message));
        files.put("file3", events);

        message = new Violation(0, 0, "", "", null, SeverityLevel.IGNORE, null, getClass(), null);
        events = new ArrayList<>();
        events.add(new AuditEvent(this, "file4", message));
        events.add(new AuditEvent(this, "file4", message));
        events.add(new AuditEvent(this, "file4", message));
        events.add(new AuditEvent(this, "file4", message));
        files.put("file4", events);

        results.setFiles(files);

        assertEquals(4, results.getFiles().size(), "test total files");
        assertEquals(4, results.getFileCount(), "test file count");

        assertEquals(0, results.getSeverityCount("file0", SeverityLevel.INFO), "test file severities");
        assertEquals(0, results.getSeverityCount("file0", SeverityLevel.WARNING), "test file severities");
        assertEquals(0, results.getSeverityCount("file0", SeverityLevel.ERROR), "test file severities");
        assertEquals(0, results.getSeverityCount("file0", SeverityLevel.IGNORE), "test file severities");

        assertEquals(1, results.getFileViolations("file1").size(), "test file violations");
        assertEquals(1, results.getSeverityCount("file1", SeverityLevel.INFO), "test file severities");
        assertEquals(0, results.getSeverityCount("file1", SeverityLevel.WARNING), "test file severities");
        assertEquals(0, results.getSeverityCount("file1", SeverityLevel.ERROR), "test file severities");
        assertEquals(0, results.getSeverityCount("file1", SeverityLevel.IGNORE), "test file severities");

        assertEquals(2, results.getFileViolations("file2").size(), "test file violations");
        assertEquals(0, results.getSeverityCount("file2", SeverityLevel.INFO), "test file severities");
        assertEquals(2, results.getSeverityCount("file2", SeverityLevel.WARNING), "test file severities");
        assertEquals(0, results.getSeverityCount("file2", SeverityLevel.ERROR), "test file severities");
        assertEquals(0, results.getSeverityCount("file2", SeverityLevel.IGNORE), "test file severities");

        assertEquals(3, results.getFileViolations("file3").size(), "test file violations");
        assertEquals(0, results.getSeverityCount("file3", SeverityLevel.INFO), "test file severities");
        assertEquals(0, results.getSeverityCount("file3", SeverityLevel.WARNING), "test file severities");
        assertEquals(3, results.getSeverityCount("file3", SeverityLevel.ERROR), "test file severities");
        assertEquals(0, results.getSeverityCount("file3", SeverityLevel.IGNORE), "test file severities");

        assertEquals(4, results.getFileViolations("file4").size(), "test file violations");
        assertEquals(0, results.getSeverityCount("file4", SeverityLevel.INFO), "test file severities");
        assertEquals(0, results.getSeverityCount("file4", SeverityLevel.WARNING), "test file severities");
        assertEquals(0, results.getSeverityCount("file4", SeverityLevel.ERROR), "test file severities");
        assertEquals(4, results.getSeverityCount("file4", SeverityLevel.IGNORE), "test file severities");

        assertEquals(1, results.getSeverityCount(SeverityLevel.INFO), "test INFO severity count");
        assertEquals(2, results.getSeverityCount(SeverityLevel.WARNING), "test WARNING severity count");
        assertEquals(3, results.getSeverityCount(SeverityLevel.ERROR), "test ERROR severity count");
        assertEquals(4, results.getSeverityCount(SeverityLevel.IGNORE), "test IGNORE severity count");

        results.setFileViolations("file", Collections.emptyList());
        assertEquals(0, results.getFileViolations("file").size(), "test file violations");
    }
}
