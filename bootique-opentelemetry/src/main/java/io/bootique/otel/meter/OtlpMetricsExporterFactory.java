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
package io.bootique.otel.meter;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.bootique.otel.otlp.OtlpExporterEndpoint;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporterBuilder;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporterBuilder;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import jakarta.inject.Inject;

@JsonTypeName("otlp")
public class OtlpMetricsExporterFactory implements MetricsExporterFactory {

    private final OtlpExporterEndpoint exporterEndpoint;

    @Inject
    public OtlpMetricsExporterFactory(OtlpExporterEndpoint exporterEndpoint) {
        this.exporterEndpoint = exporterEndpoint;
    }

    @Override
    public MetricExporter create() {

        // TODO: own endpoint overrides

        // No explicit shutdown. The exporter is closed by the parent MetricReader, which is in turn closed by
        // SdkMeterProvider

        return switch (exporterEndpoint.protocol()) {

            // TODO: memoryMode (currently, the default is "reusable_data" which seems good enough)
            // TODO: clientTls (certificates)
            // TODO: compression
            // TODO: connectTimeout, timeout
            // TODO: retryPolicy
            // TODO: executorService

            case grpc -> {
                OtlpGrpcMetricExporterBuilder builder = OtlpGrpcMetricExporter.builder()
                        .setEndpoint(exporterEndpoint.metricsEndpointUrl());

                exporterEndpoint.headers().forEach(builder::addHeader);
                yield builder.build();
            }

            case http_protobuf -> {
                OtlpHttpMetricExporterBuilder builder = OtlpHttpMetricExporter.builder()
                        .setEndpoint(exporterEndpoint.metricsEndpointUrl());

                exporterEndpoint.headers().forEach(builder::addHeader);
                yield builder.build();
            }
        };
    }
}
