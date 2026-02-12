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
 * Defines supported variables from the OTel "Environment Variable Specification". Each var has a Java property equivalent
 * for the var strored in {@link #otelProperty}.
 *
 * @see <a href="https://opentelemetry.io/docs/specs/otel/configuration/sdk-environment-variables/">Environment Variable
 * Specification</a>
 * @since 4.0
 */
enum OpenTelemetryVar {

    /* General SDK Configuration */
    OTEL_SERVICE_NAME("service.name"),

    /* Exporter Selection */
    // TODO: support
//    OTEL_TRACES_EXPORTER("traces.exporter"),
//    OTEL_METRICS_EXPORTER("metrics.exporter"),
//    OTEL_LOGS_EXPORTER("logs.exporter"),

    /* OTLP Exporter */
    OTEL_EXPORTER_OTLP_ENDPOINT("exporter.otlp.endpoint"),
    // TODO: OTEL_EXPORTER_OTLP_TRACES_ENDPOINT OTEL_EXPORTER_OTLP_METRICS_ENDPOINT OTEL_EXPORTER_OTLP_LOGS_ENDPOINT
    OTEL_EXPORTER_OTLP_PROTOCOL("exporter.otlp.protocol"),
    // TODO: OTEL_EXPORTER_OTLP_TRACES_PROTOCOL, OTEL_EXPORTER_OTLP_METRICS_PROTOCOL, OTEL_EXPORTER_OTLP_LOGS_PROTOCOL
    OTEL_EXPORTER_OTLP_HEADERS("exporter.otlp.headers"),
    // TODO: OTEL_EXPORTER_OTLP_TRACES_HEADERS, OTEL_EXPORTER_OTLP_METRICS_HEADERS, OTEL_EXPORTER_OTLP_LOGS_HEADERS

    /* Metrics SDK Configuration */
    OTEL_METRIC_EXPORT_INTERVAL("metric.export.interval");

    public final String otelProperty;

    OpenTelemetryVar(String otelProperty) {
        this.otelProperty = otelProperty;
    }
}
