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
import java.util.HashMap;
import java.util.Map;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.SeverityLevel;
import com.puppycrawl.tools.checkstyle.api.Violation;
import junit.framework.TestCase;

/**
 * @author Edwin Punzalan
 *
 */
public class CheckstyleReportListenerTest extends TestCase {
    private Map<SeverityLevel, CheckstyleCheckerListener> listenerMap = new HashMap<>();

    /** {@inheritDoc} */
    @Override
    protected void setUp() throws Exception {
        CheckstyleCheckerListener listener = new CheckstyleCheckerListener(new File("/source/path"));
        listener.setSeverityLevelFilter(SeverityLevel.INFO);
        listenerMap.put(listener.getSeverityLevelFilter(), listener);

        listener = new CheckstyleCheckerListener(new File("/source/path"));
        listener.setSeverityLevelFilter(SeverityLevel.WARNING);
        listenerMap.put(listener.getSeverityLevelFilter(), listener);

        listener = new CheckstyleCheckerListener(new File("/source/path"));
        listener.setSeverityLevelFilter(SeverityLevel.ERROR);
        listenerMap.put(listener.getSeverityLevelFilter(), listener);

        listener = new CheckstyleCheckerListener(new File("/source/path"));
        listener.setSeverityLevelFilter(SeverityLevel.IGNORE);
        listenerMap.put(listener.getSeverityLevelFilter(), listener);
    }

    public void testListeners() {
        fireAuditStarted(null);

        AuditEvent event = new AuditEvent(this, "/source/path/file1", null);
        fireFileStarted(event);
        Violation message = new Violation(0, 0, "", "", null, SeverityLevel.INFO, null, getClass(), null);
        fireAddError(new AuditEvent(this, "/source/path/file1", message));
        fireFileFinished(event);

        event = new AuditEvent(this, "/source/path/file2", null);
        fireFileStarted(event);
        message = new Violation(0, 0, "", "", null, SeverityLevel.WARNING, null, getClass(), null);
        fireAddError(new AuditEvent(this, "/source/path/file2", message));
        fireAddError(new AuditEvent(this, "/source/path/file2", message));
        fireFileFinished(event);

        event = new AuditEvent(this, "/source/path/file3", null);
        fireFileStarted(event);
        message = new Violation(0, 0, "", "", null, SeverityLevel.ERROR, null, getClass(), null);
        fireAddError(new AuditEvent(this, "/source/path/file3", message));
        fireAddError(new AuditEvent(this, "/source/path/file3", message));
        fireAddError(new AuditEvent(this, "/source/path/file3", message));
        fireFileFinished(event);

        event = new AuditEvent(this, "/source/path/file4", null);
        fireFileStarted(event);
        message = new Violation(0, 0, "", "", null, SeverityLevel.IGNORE, null, getClass(), null);
        fireAddError(new AuditEvent(this, "/source/path/file4", message));
        fireAddError(new AuditEvent(this, "/source/path/file4", message));
        fireAddError(new AuditEvent(this, "/source/path/file4", message));
        fireAddError(new AuditEvent(this, "/source/path/file4", message));
        fireFileFinished(event);

        fireAuditFinished(null);

        CheckstyleCheckerListener listener = listenerMap.get(SeverityLevel.INFO);
        CheckstyleResults results = listener.getResults();
        assertEquals("Test total files", 4, results.getFiles().size());
        assertEquals("Test file count", 4, results.getFileCount());
        assertEquals(
                "test file violations", 1, results.getFileViolations("file1").size());
        assertEquals("test file severities", 1, results.getSeverityCount("file1", SeverityLevel.INFO));
        assertEquals("test file severities", 0, results.getSeverityCount("file1", SeverityLevel.WARNING));
        assertEquals("test file severities", 0, results.getSeverityCount("file1", SeverityLevel.ERROR));
        assertEquals("test file severities", 0, results.getSeverityCount("file1", SeverityLevel.IGNORE));

        listener = listenerMap.get(SeverityLevel.WARNING);
        results = listener.getResults();
        assertEquals("Test total files", 4, results.getFiles().size());
        assertEquals("Test file count", 4, results.getFileCount());
        assertEquals(
                "test file violations", 2, results.getFileViolations("file2").size());
        assertEquals("test file severities", 0, results.getSeverityCount("file2", SeverityLevel.INFO));
        assertEquals("test file severities", 2, results.getSeverityCount("file2", SeverityLevel.WARNING));
        assertEquals("test file severities", 0, results.getSeverityCount("file2", SeverityLevel.ERROR));
        assertEquals("test file severities", 0, results.getSeverityCount("file2", SeverityLevel.IGNORE));

        listener = listenerMap.get(SeverityLevel.ERROR);
        results = listener.getResults();
        assertEquals("Test total files", 4, results.getFiles().size());
        assertEquals("Test file count", 4, results.getFileCount());
        assertEquals(
                "test file violations", 3, results.getFileViolations("file3").size());
        assertEquals("test file severities", 0, results.getSeverityCount("file3", SeverityLevel.INFO));
        assertEquals("test file severities", 0, results.getSeverityCount("file3", SeverityLevel.WARNING));
        assertEquals("test file severities", 3, results.getSeverityCount("file3", SeverityLevel.ERROR));
        assertEquals("test file severities", 0, results.getSeverityCount("file3", SeverityLevel.IGNORE));

        listener = listenerMap.get(SeverityLevel.IGNORE);
        results = listener.getResults();
        assertEquals("Test total files", 4, results.getFiles().size());
        assertEquals("Test file count", 4, results.getFileCount());
        assertEquals(
                "test file violations", 0, results.getFileViolations("file4").size());
        assertEquals("test file severities", 0, results.getSeverityCount("file4", SeverityLevel.INFO));
        assertEquals("test file severities", 0, results.getSeverityCount("file4", SeverityLevel.WARNING));
        assertEquals("test file severities", 0, results.getSeverityCount("file4", SeverityLevel.ERROR));
        assertEquals("test file severities", 0, results.getSeverityCount("file4", SeverityLevel.IGNORE));
    }

    private void fireAuditStarted(AuditEvent event) {
        for (CheckstyleCheckerListener listener : listenerMap.values()) {
            listener.auditStarted(event);
        }
    }

    private void fireAuditFinished(AuditEvent event) {
        for (CheckstyleCheckerListener listener : listenerMap.values()) {
            listener.auditFinished(event);
        }
    }

    private void fireFileStarted(AuditEvent event) {
        for (CheckstyleCheckerListener listener : listenerMap.values()) {
            listener.fileStarted(event);
        }
    }

    private void fireFileFinished(AuditEvent event) {
        for (CheckstyleCheckerListener listener : listenerMap.values()) {
            listener.fileFinished(event);
        }
    }

    private void fireAddError(AuditEvent event) {
        for (CheckstyleCheckerListener listener : listenerMap.values()) {
            listener.addError(event);
        }
    }
}
