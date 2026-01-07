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
package org.apache.maven.plugins.checkstyle;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Consumer;

import com.puppycrawl.tools.checkstyle.DefaultLogger;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import com.puppycrawl.tools.checkstyle.api.AutomaticBean.OutputStreamOptions;
import org.apache.maven.plugin.logging.Log;

/**
 * An implementation of {@link AuditListener} that redirects output to the specified {@code log}
 */
public class MavenConsoleLogger implements AuditListener {
    private final Log log;

    public MavenConsoleLogger(Log log) {
        this.log = log;
    }

    @Override
    public void auditStarted(AuditEvent event) {
        recordMessage(delegate -> delegate.auditStarted(event));
    }

    @Override
    public void auditFinished(AuditEvent event) {
        recordMessage(delegate -> delegate.auditFinished(event));
    }

    @Override
    public void fileStarted(AuditEvent event) {
        recordMessage(delegate -> delegate.fileStarted(event));
    }

    @Override
    public void fileFinished(AuditEvent event) {
        recordMessage(delegate -> delegate.fileFinished(event));
    }

    @Override
    public void addError(AuditEvent event) {
        recordMessage(delegate -> delegate.addError(event));
    }

    @Override
    public void addException(AuditEvent event, Throwable throwable) {
        recordMessage(delegate -> delegate.addException(event, throwable));
    }

    private void recordMessage(Consumer<AuditListener> consumer) {
        if (log.isInfoEnabled()) {
            // Uses DefaultLogger for consistency
            // Not possible to access formatting directly, so instead log out to a buffer and read that back in
            try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
                consumer.accept(new DefaultLogger(stream, OutputStreamOptions.NONE));

                if (stream.size() != 0) {
                    log.info(stream.toString().trim());
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
