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

import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;
import io.bootique.shutdown.ShutdownManager;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.SdkLoggerProviderBuilder;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessorBuilder;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.resources.Resource;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * @since 4.0
 */
@BQConfig
public class SdkLoggerProviderFactory {

    private final Resource resource;
    private final ShutdownManager shutdownManager;

    private List<LogsExporterFactory> logExporters;

    @BQConfigProperty
    public SdkLoggerProviderFactory setLogExporters(List<LogsExporterFactory> logExporters) {
        this.logExporters = logExporters;
        return this;
    }

    @Inject
    public SdkLoggerProviderFactory(Resource resource, ShutdownManager shutdownManager) {
        this.resource = resource;
        this.shutdownManager = shutdownManager;
    }

    public SdkLoggerProvider create(MeterProvider meterProvider) {

        Supplier<MeterProvider> meterProviderSupplier = () -> meterProvider;

        SdkLoggerProviderBuilder builder = SdkLoggerProvider
                .builder()
                .setResource(resource)
                .setMeterProvider(meterProviderSupplier);

        // TODO: clock
        // TODO: log limits

        createProcessors(meterProviderSupplier).forEach(builder::addLogRecordProcessor);
        return shutdownManager.onShutdown(builder.build());
    }

    private List<LogRecordProcessor> createProcessors(Supplier<MeterProvider> meterProvider) {
        List<LogRecordProcessor> processors = new ArrayList<>(2);

        List<LogRecordExporterHolder> batchedExporters = exporterHolders()
                .stream()

                // add a simple processor for console exporter, feed the rest into a single batch processor
                .peek(e -> {
                    if (!e.shouldBatch()) {
                        processors.add(createSimpleProcessor(e, meterProvider));
                    }
                })
                .filter(LogRecordExporterHolder::shouldBatch)
                .toList();

        if (!batchedExporters.isEmpty()) {
            processors.add(createBatchProcessor(batchedExporters, meterProvider));
        }

        return processors;
    }

    private LogRecordProcessor createSimpleProcessor(LogRecordExporterHolder exporterSupplier, Supplier<MeterProvider> meterProvider) {
        // presumably we don't need to shut down the exporter, as SpanProcessor would do it for us
        LogRecordProcessor processor = SimpleLogRecordProcessor
                .builder(exporterSupplier.exporterSupplier().get())
                .setMeterProvider(meterProvider)
                .build();

        return shutdownManager.onShutdown(processor);
    }

    private BatchLogRecordProcessor createBatchProcessor(List<LogRecordExporterHolder> exporterSuppliers, Supplier<MeterProvider> meterProvider) {

        List<LogRecordExporter> exporters = exporterSuppliers.stream().map(s -> s.exporterSupplier().get()).toList();
        LogRecordExporter composite = MultiLogRecordExporter.create(exporters);

        // presumably we don't need to shut down the exporter, as BatchLogRecordProcessor would do it for us
        BatchLogRecordProcessorBuilder builder = BatchLogRecordProcessor
                .builder(composite)
                .setMeterProvider(meterProvider);

        // TODO:
        //  support props for the builder:
        //   schedule.delay
        //   max.queue.size
        //   max.export.batch.size
        //   export.timeout

        return shutdownManager.onShutdown(builder.build());
    }

    private List<LogRecordExporterHolder> exporterHolders() {
        // unlike the agent whose default is "otlp", our default will be "console", so that the app could
        // work standalone out of the box. To suppress exporting, an explicit "none" exporter should be set

        // A single "none" exporter would suppress the default "console" exporter. Though unlike the agent, having a
        // "none" exporter mixed with others doesn't result in an exception. It will just be ignored

        List<LogsExporterFactory> exporters = this.logExporters == null || this.logExporters.isEmpty()
                ? List.of(new ConsoleLogsExporterFactory())
                : this.logExporters;

        return exporters.stream()
                .map(LogsExporterFactory::create)
                .filter(Objects::nonNull)
                .toList();
    }
}
