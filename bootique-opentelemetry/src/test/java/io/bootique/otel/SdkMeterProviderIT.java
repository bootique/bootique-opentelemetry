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

import io.bootique.BQCoreModule;
import io.bootique.BQRuntime;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.LongCounter;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

@BQTest
public class SdkMeterProviderIT {

    @BQTestTool
    static final BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    @Test
    public void defaultExporter() {
        BQRuntime runtime = testFactory.app()
                .module(b -> BQCoreModule.extend(b)
                        .setProperty("bq.opentelemetry.meterProvider.metricExportInterval", "100ms"))
                .createRuntime();

        OpenTelemetry otel = runtime.getInstance(OpenTelemetry.class);
        LongCounter counter = otel.getMeter("test").counterBuilder("test-counter").build();
        counter.add(1);

        String output = captureStderr(() -> sleep(200));
        assertTrue(output.contains("test-counter"), "Expected metric name in console output, got: " + output);
    }

    @Test
    public void consoleExporter() {
        BQRuntime runtime = testFactory.app()
                .module(b -> BQCoreModule.extend(b)
                        .setProperty("bq.opentelemetry.meterProvider.metricExportInterval", "100ms")
                        .setProperty("bq.opentelemetry.meterProvider.metricExporters[0].type", "console"))
                .createRuntime();

        OpenTelemetry otel = runtime.getInstance(OpenTelemetry.class);
        LongCounter counter = otel.getMeter("test").counterBuilder("console-counter").build();
        counter.add(1);

        String output = captureStderr(() -> sleep(200));
        assertTrue(output.contains("console-counter"), "Expected metric name in console output, got: " + output);
    }

    @Test
    public void noopExporter() {
        BQRuntime runtime = testFactory.app()
                .module(b -> BQCoreModule.extend(b)
                        .setProperty("bq.opentelemetry.meterProvider.metricExportInterval", "100ms")
                        .setProperty("bq.opentelemetry.meterProvider.metricExporters[0].type", "noop"))
                .createRuntime();

        OpenTelemetry otel = runtime.getInstance(OpenTelemetry.class);
        LongCounter counter = otel.getMeter("test").counterBuilder("noop-counter").build();
        counter.add(1);

        String output = captureStderr(() -> sleep(200));
        assertFalse(output.contains("noop-counter"), "No export output expected with 'noop' exporter, got: " + output);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static String captureStderr(Runnable action) {
        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(capture));
        try {
            action.run();
        } finally {
            System.setErr(originalErr);
        }
        return capture.toString();
    }
}
