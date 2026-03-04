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

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.bootique.otel.otlp.OtlpExporterEndpoint;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporterBuilder;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporterBuilder;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import jakarta.inject.Inject;

@JsonTypeName("otlp")
public class OtlpLogsExporterFactory implements LogsExporterFactory {

    private final OtlpExporterEndpoint exporterEndpoint;

    @Inject
    public OtlpLogsExporterFactory(OtlpExporterEndpoint exporterEndpoint) {
        this.exporterEndpoint = exporterEndpoint;
    }

    @Override
    public LogRecordExporterHolder create() {

        // TODO: own endpoint overrides

        // No explicit shutdown. The exporter is closed by the parent BatchLogRecordProcessor, which is in turn
        // closed by SdkLoggerProvider
        return new LogRecordExporterHolder(this::createExporter, true);
    }

    private LogRecordExporter createExporter() {
        return switch (exporterEndpoint.protocol()) {

            // TODO: memoryMode (currently, the default is "reusable_data" which seems good enough)
            // TODO: clientTls (certificates)
            // TODO: compression
            // TODO: connectTimeout, timeout
            // TODO: retryPolicy
            // TODO: executorService

            case grpc -> {
                OtlpGrpcLogRecordExporterBuilder builder = OtlpGrpcLogRecordExporter.builder()
                        .setEndpoint(exporterEndpoint.logsEndpointUrl());

                exporterEndpoint.headers().forEach(builder::addHeader);
                yield builder.build();
            }

            case http_protobuf -> {
                OtlpHttpLogRecordExporterBuilder builder = OtlpHttpLogRecordExporter.builder()
                        .setEndpoint(exporterEndpoint.logsEndpointUrl());

                exporterEndpoint.headers().forEach(builder::addHeader);
                yield builder.build();
            }
        };
    }
}
