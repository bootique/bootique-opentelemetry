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
package io.bootique.otel;

/**
 * Defines a subset of variables from the OTel "Environment Variable Specification" that is supported by the Bootique
 * integration. Each var has a Java property equivalent stored in {@link #otelProperty} and a Bootique configuration
 * path stored in {@link #configPath} (null if there is no direct Bootique config equivalent).
 *
 * @see <a href="https://opentelemetry.io/docs/specs/otel/configuration/sdk-environment-variables/">Environment Variable
 * Specification</a>
 * @since 4.0
 */
enum OpenTelemetryVar {

    /* General SDK Configuration */
    OTEL_SERVICE_NAME("service.name", "opentelemetry.serviceName"),

    /* Exporter Selection */
    OTEL_TRACES_EXPORTER("traces.exporter", "opentelemetry.tracerProvider.exporters[0].type"),
    OTEL_METRICS_EXPORTER("metrics.exporter", "opentelemetry.meterProvider.exporters[0].type"),
    OTEL_LOGS_EXPORTER("logs.exporter", "opentelemetry.loggerProvider.exporters[0].type"),

    /* OTLP Exporter */
    OTEL_EXPORTER_OTLP_ENDPOINT("exporter.otlp.endpoint", "opentelemetry.otlp.url"),
    // TODO: OTEL_EXPORTER_OTLP_TRACES_ENDPOINT OTEL_EXPORTER_OTLP_METRICS_ENDPOINT OTEL_EXPORTER_OTLP_LOGS_ENDPOINT
    OTEL_EXPORTER_OTLP_PROTOCOL("exporter.otlp.protocol", "opentelemetry.otlp.protocol"),
    // TODO: OTEL_EXPORTER_OTLP_TRACES_PROTOCOL, OTEL_EXPORTER_OTLP_METRICS_PROTOCOL, OTEL_EXPORTER_OTLP_LOGS_PROTOCOL
    OTEL_EXPORTER_OTLP_HEADERS("exporter.otlp.headers", "opentelemetry.otlp.headers"),
    // TODO: OTEL_EXPORTER_OTLP_TRACES_HEADERS, OTEL_EXPORTER_OTLP_METRICS_HEADERS, OTEL_EXPORTER_OTLP_LOGS_HEADERS

    /* Metrics SDK Configuration */
    OTEL_METRIC_EXPORT_INTERVAL("metric.export.interval", "opentelemetry.meterProvider.exportInterval"),

    /* Batch span processor */
    OTEL_BSP_SCHEDULE_DELAY("otel.bsp.schedule.delay", "opentelemetry.tracerProvider.scheduleDelay"),

    /* Batch log record processor */
    OTEL_BLRP_SCHEDULE_DELAY("otel.blrp.schedule.delay", "opentelemetry.loggerProvider.scheduleDelay");

    public final String otelProperty;

    /**
     * A dot-separated Bootique configuration path bound to this variable, or null if there is no direct equivalent.
     */
    public final String configPath;

    OpenTelemetryVar(String otelProperty, String configPath) {
        this.otelProperty = otelProperty;
        this.configPath = configPath;
    }
}
