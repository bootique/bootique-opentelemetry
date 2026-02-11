/*
 * Licensed to ObjectStyle LLC under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ObjectStyle LLC licenses
 * this file to you under the Apache License, Version 2.0 (the
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
package io.bootique.otel.logger;

import io.opentelemetry.api.common.Value;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * @since 4.0
 */
// Based on OTel SystemOutLogRecordExporter
// TODO: as of 1.59.0, OTel has no Logger based exporter (unlike e.g., metrics exporters). It only has
//  a STDOUT exporter. So we provide our own.
class LoggerLogRecordExporter implements LogRecordExporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggerLogRecordExporter.class);

    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ISO_DATE_TIME;

    private final AtomicBoolean isShutdown = new AtomicBoolean();

    @Override
    public CompletableResultCode export(Collection<LogRecordData> logs) {
        if (isShutdown.get()) {
            return CompletableResultCode.ofFailure();
        }

        if(LOGGER.isInfoEnabled()) {
            StringBuilder stringBuilder = new StringBuilder(60);

            for (LogRecordData log : logs) {
                stringBuilder.setLength(0);
                formatLog(stringBuilder, log);
                LOGGER.info(stringBuilder.toString());
            }
        }

        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }

    static void formatLog(StringBuilder stringBuilder, LogRecordData log) {
        InstrumentationScopeInfo instrumentationScopeInfo = log.getInstrumentationScopeInfo();
        Value<?> body = log.getBodyValue();
        stringBuilder
                .append(
                        ISO_FORMAT.format(
                                Instant.ofEpochMilli(NANOSECONDS.toMillis(log.getTimestampEpochNanos()))
                                        .atZone(ZoneOffset.UTC)))
                .append(" ")
                .append(log.getSeverity())
                .append(" '")
                .append(body == null ? "" : body.asString())
                .append("' : ")
                .append(log.getSpanContext().getTraceId())
                .append(" ")
                .append(log.getSpanContext().getSpanId())
                .append(" [scopeInfo: ")
                .append(instrumentationScopeInfo.getName())
                .append(":")
                .append(
                        instrumentationScopeInfo.getVersion() == null
                                ? ""
                                : instrumentationScopeInfo.getVersion())
                .append("] ")
                .append(log.getAttributes());
    }

    @Override
    public CompletableResultCode shutdown() {
        if (!isShutdown.compareAndSet(false, true)) {
            System.out.println("Calling shutdown() multiple times.");
            return CompletableResultCode.ofSuccess();
        }
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public String toString() {
        return "LoggerLogRecordExporter{}";
    }
}
