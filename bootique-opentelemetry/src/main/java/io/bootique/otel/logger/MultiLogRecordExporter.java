/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.bootique.otel.logger;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

// Copied from OTel verbatim
// TODO: as of 1.59.0, neither MultiLogRecordExporter or their factory methods are public, so have to copy this here
class MultiLogRecordExporter implements LogRecordExporter {
    private static final Logger logger = Logger.getLogger(MultiLogRecordExporter.class.getName());

    private final LogRecordExporter[] logRecordExporters;

    private MultiLogRecordExporter(LogRecordExporter[] logRecordExporters) {
        this.logRecordExporters = logRecordExporters;
    }

    static LogRecordExporter create(List<LogRecordExporter> logRecordExporters) {
        return new MultiLogRecordExporter(logRecordExporters.toArray(new LogRecordExporter[0]));
    }

    @Override
    public CompletableResultCode export(Collection<LogRecordData> logs) {
        List<CompletableResultCode> results = new ArrayList<>(logRecordExporters.length);
        for (LogRecordExporter logRecordExporter : logRecordExporters) {
            CompletableResultCode exportResult;
            try {
                exportResult = logRecordExporter.export(logs);
            } catch (RuntimeException e) {
                // If an exception was thrown by the exporter
                logger.log(Level.WARNING, "Exception thrown by the export.", e);
                results.add(CompletableResultCode.ofFailure());
                continue;
            }
            results.add(exportResult);
        }
        return CompletableResultCode.ofAll(results);
    }

    @Override
    public CompletableResultCode flush() {
        List<CompletableResultCode> results = new ArrayList<>(logRecordExporters.length);
        for (LogRecordExporter logRecordExporter : logRecordExporters) {
            CompletableResultCode flushResult;
            try {
                flushResult = logRecordExporter.flush();
            } catch (RuntimeException e) {
                // If an exception was thrown by the exporter
                logger.log(Level.WARNING, "Exception thrown by the flush.", e);
                results.add(CompletableResultCode.ofFailure());
                continue;
            }
            results.add(flushResult);
        }
        return CompletableResultCode.ofAll(results);
    }

    @Override
    public CompletableResultCode shutdown() {
        List<CompletableResultCode> results = new ArrayList<>(logRecordExporters.length);
        for (LogRecordExporter logRecordExporter : logRecordExporters) {
            CompletableResultCode shutdownResult;
            try {
                shutdownResult = logRecordExporter.shutdown();
            } catch (RuntimeException e) {
                // If an exception was thrown by the exporter
                logger.log(Level.WARNING, "Exception thrown by the shutdown.", e);
                results.add(CompletableResultCode.ofFailure());
                continue;
            }
            results.add(shutdownResult);
        }
        return CompletableResultCode.ofAll(results);
    }

    @Override
    public String toString() {
        return "MultiLogRecordExporter{"
                + "logRecordExporters="
                + Arrays.toString(logRecordExporters)
                + '}';
    }
}
