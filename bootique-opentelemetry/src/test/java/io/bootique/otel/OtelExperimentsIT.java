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

import io.bootique.junit5.BQTest;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.logging.LoggingMetricExporter;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.junit.jupiter.api.Test;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.time.Duration;
import java.util.logging.LogManager;

@BQTest
// TODO: not a real test, just OTel experiments; remove when we are done
public class OtelExperimentsIT {

    // reconfigure JUL used by LoggingMetricExporter and friends
    static {
        LogManager.getLogManager().reset();
        SLF4JBridgeHandler.install();
    }

    @Test
    public void experimental() throws InterruptedException {
        MetricReader periodicReader = PeriodicMetricReader
                .builder(LoggingMetricExporter.create())
                .setInterval(Duration.ofMillis(500L))
                .build();

        SdkMeterProvider meterProvider = SdkMeterProvider
                .builder()
                .registerMetricReader(periodicReader)
                .build();

        SdkTracerProvider tracerProvider = SdkTracerProvider
                .builder()
                .addSpanProcessor(SimpleSpanProcessor.create(LoggingSpanExporter.create()))
                .build();

        OpenTelemetry otel = OpenTelemetrySdk
                .builder()
                .setMeterProvider(meterProvider)
                .setTracerProvider(tracerProvider)
                .build();

        Tracer tracer = otel.getTracer("experimental");
        LongCounter counter = otel.getMeter("experimental").counterBuilder("work_done").build();

        for (int i = 0; i < 5; i++) {
            Span span = tracer.spanBuilder("doWork").startSpan();
            try {
                Thread.sleep(500L);
                counter.add(1);
            } finally {
                span.end();
            }
        }

        Thread.sleep(1000L);
    }
}
