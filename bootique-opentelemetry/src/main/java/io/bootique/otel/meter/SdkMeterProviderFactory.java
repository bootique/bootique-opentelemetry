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

import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;
import io.bootique.shutdown.ShutdownManager;
import io.bootique.value.Duration;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Objects;

/**
 * @since 4.0
 */
@BQConfig
public class SdkMeterProviderFactory {

    private final Resource resource;
    private final ShutdownManager shutdownManager;

    private Duration metricExportInterval;
    private List<MetricExporterFactory> metricExporters;


    @Inject
    public SdkMeterProviderFactory(Resource resource, ShutdownManager shutdownManager) {
        this.resource = resource;
        this.shutdownManager = shutdownManager;
    }

    @BQConfigProperty("Time interval between the start of two export attempts. The default is '1min'")
    public SdkMeterProviderFactory setMetricExportInterval(Duration metricExportInterval) {
        this.metricExportInterval = metricExportInterval;
        return this;
    }

    @BQConfigProperty
    public SdkMeterProviderFactory setMetricExporters(List<MetricExporterFactory> metricExporters) {
        this.metricExporters = metricExporters;
        return this;
    }

    public SdkMeterProvider create() {
        SdkMeterProviderBuilder builder = SdkMeterProvider
                .builder()
                .setResource(resource);

        // TODO:
        //   views
        //   clock
        //   exemplar filter

        createMetricReaders().forEach(builder::registerMetricReader);
        return shutdownManager.onShutdown(builder.build());
    }

    private List<MetricReader> createMetricReaders() {
        return createMetricExporters().stream().map(this::createMetricReader).toList();
    }

    private List<MetricExporter> createMetricExporters() {

        // unlike the agent whose default is "otlp", our default will be "console", so that the app could
        // work standalone out of the box. To suppress exporting, an explicit "none" exporter should be set

        // A single "none" exporter would suppress the default "console" exporter. Though unlike the agent, having a
        // "none" exporter mixed with others doesn't result in an exception. It will just be ignored

        List<MetricExporterFactory> exporters = this.metricExporters == null || this.metricExporters.isEmpty()
                ? List.of(new ConsoleMetricExporterFactory())
                : this.metricExporters;

        return exporters.stream()
                .map(MetricExporterFactory::create)
                .filter(Objects::nonNull)
                .toList();
    }

    private MetricReader createMetricReader(MetricExporter exporter) {
        return PeriodicMetricReader.builder(exporter)
                .setInterval(getMetricExportIntervalOrDefault())
                .build();
    }

    private java.time.Duration getMetricExportIntervalOrDefault() {
        return this.metricExportInterval != null
                ? this.metricExportInterval.getDuration()
                : java.time.Duration.ofMinutes(1);
    }
}
