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

import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;
import io.bootique.shutdown.ShutdownManager;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessorBuilder;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @since 4.0
 */
@BQConfig
public class SdkTracerProviderFactory {

    private final Resource resource;
    private final ShutdownManager shutdownManager;

    private List<SpanExporterFactory> traceExporters;

    @Inject
    public SdkTracerProviderFactory(Resource resource, ShutdownManager shutdownManager) {
        this.resource = resource;
        this.shutdownManager = shutdownManager;
    }

    @BQConfigProperty
    public SdkTracerProviderFactory setTraceExporters(List<SpanExporterFactory> traceExporters) {
        this.traceExporters = traceExporters;
        return this;
    }

    public SdkTracerProvider create(MeterProvider meterProvider) {

        SdkTracerProviderBuilder builder = SdkTracerProvider
                .builder()
                .setResource(resource)
                .setMeterProvider(() -> meterProvider);

        // TODO: span limits
        // TODO: sampler

        createSpanProcessors().forEach(builder::addSpanProcessor);

        return shutdownManager.onShutdown(builder.build());
    }

    private List<SpanProcessor> createSpanProcessors() {

        List<SpanProcessor> processors = new ArrayList<>(2);

        List<SpanExporterHolder> batchedExporters = exporterSuppliers()
                .stream()

                // add a simple processor for console exporter, feed the rest into a single batch processor
                .peek(e -> {
                    if (!e.shouldBatch()) {
                        processors.add(createSimpleSpanProcessor(e));
                    }
                })
                .filter(SpanExporterHolder::shouldBatch)
                .toList();

        if (!batchedExporters.isEmpty()) {
            processors.add(createBatchSpanProcessor(batchedExporters));
        }

        return processors;
    }

    private List<SpanExporterHolder> exporterSuppliers() {

        // unlike the agent whose default is "otlp", our default will be "console", so that the app could
        // work standalone out of the box. To suppress exporting, an explicit "none" exporter should be set

        // A single "none" exporter would suppress the default "console" exporter. Though unlike the agent, having a
        // "none" exporter mixed with others doesn't result in an exception. It will just be ignored

        List<SpanExporterFactory> exporters = this.traceExporters == null || this.traceExporters.isEmpty()
                ? List.of(new ConsoleSpanExporterFactory())
                : this.traceExporters;

        return exporters.stream()
                .map(SpanExporterFactory::create)
                .filter(Objects::nonNull)
                .toList();
    }

    private SpanProcessor createSimpleSpanProcessor(SpanExporterHolder exporterSupplier) {
        // TODO: .setMeterProvider(() -> meterProvider)

        // presumably we don't need to shut down the exporter, as SpanProcessor would do it for us
        SpanProcessor processor = SimpleSpanProcessor.builder(exporterSupplier.spanExporter().get()).build();
        return shutdownManager.onShutdown(processor);
    }

    private BatchSpanProcessor createBatchSpanProcessor(List<SpanExporterHolder> exporterSuppliers) {

        List<SpanExporter> exporters = exporterSuppliers.stream().map(s -> s.spanExporter().get()).toList();
        SpanExporter composite = SpanExporter.composite(exporters);

        // as of OTel 1.59.0 unlike SimpleSpanProcessor, the batch processor will not shut down the exporters, so we
        // need to do it ourselves
        BatchSpanProcessorBuilder builder = BatchSpanProcessor.builder(shutdownManager.onShutdown(composite));

        // TODO:
        //  support .setMeterProvider(..)
        //  support props for the builder:
        //   otel.bsp.schedule.delay
        //   otel.bsp.max.queue.size
        //   otel.bsp.max.export.batch.size
        //   otel.bsp.export.timeout

        return shutdownManager.onShutdown(builder.build());
    }
}
