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
package io.bootique.otel.trace;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.bootique.otel.otlp.OtlpExporterEndpoint;
import io.bootique.shutdown.ShutdownManager;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporterBuilder;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporterBuilder;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import jakarta.inject.Inject;

import java.util.function.Supplier;

@JsonTypeName("otlp")
public class OtlpTracesExporterFactory implements TracesExporterFactory {

    private final OtlpExporterEndpoint exporterEndpoint;
    private final ShutdownManager shutdownManager;

    @Inject
    public OtlpTracesExporterFactory(OtlpExporterEndpoint exporterEndpoint, ShutdownManager shutdownManager) {
        this.exporterEndpoint = exporterEndpoint;
        this.shutdownManager = shutdownManager;
    }

    @Override
    public SpanExporterHolder create(Supplier<MeterProvider> meterProvider) {

        // TODO: own endpoint overrides

        // presumably we don't need to shut down the exporter, as SpanProcessor would do it for us
        SpanExporter exporter = shutdownManager.onShutdown(createExporter(meterProvider));

        return new SpanExporterHolder(() -> exporter, true);
    }

    private SpanExporter createExporter(Supplier<MeterProvider> meterProvider) {
        return switch (exporterEndpoint.protocol()) {

            // TODO: memoryMode (currently, the default is "reusable_data" which seems good enough)
            // TODO: clientTls (certificates)
            // TODO: compression
            // TODO: connectTimeout, timeout
            // TODO: retryPolicy
            // TODO: executorService

            case grpc -> {
                OtlpGrpcSpanExporterBuilder builder = OtlpGrpcSpanExporter.builder()
                        .setEndpoint(exporterEndpoint.tracesEndpointUrl())
                        .setMeterProvider(meterProvider);

                exporterEndpoint.headers().forEach(builder::addHeader);
                yield builder.build();
            }

            case http_protobuf -> {
                OtlpHttpSpanExporterBuilder builder = OtlpHttpSpanExporter.builder()
                        .setEndpoint(exporterEndpoint.tracesEndpointUrl())
                        .setMeterProvider(meterProvider);

                exporterEndpoint.headers().forEach(builder::addHeader);
                yield builder.build();
            }
        };
    }
}
