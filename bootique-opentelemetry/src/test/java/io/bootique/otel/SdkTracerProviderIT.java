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
import io.bootique.meta.application.ApplicationMetadata;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

@BQTest
public class SdkTracerProviderIT {

    @BQTestTool
    static final BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    @Test
    public void defaultExporter() {
        BQRuntime runtime = testFactory.app().createRuntime();
        OpenTelemetry otel = runtime.getInstance(OpenTelemetry.class);
        ApplicationMetadata md = runtime.getInstance(ApplicationMetadata.class);

        Span span = otel.getTracer("test").spanBuilder("test-span").startSpan();
        try {
            assertInstanceOf(ReadableSpan.class, span);
            SpanData spanData = ((ReadableSpan) span).toSpanData();

            assertEquals(md.getName(), spanData.getResource().getAttribute(AttributeKey.stringKey("service.name")));
            assertEquals("opentelemetry", spanData.getResource().getAttribute(AttributeKey.stringKey("telemetry.sdk.name")));
        } finally {
            String output = captureStderr(span::end);
            assertTrue(output.contains("'test-span'"), "Expected span name in console output, got: " + output);
            assertTrue(output.contains("[tracer: test:]"), "Expected tracer name in console output, got: " + output);
        }
    }

    @Test
    public void consoleExporter() {
        BQRuntime runtime = testFactory.app()
                .module(b -> BQCoreModule.extend(b).setProperty("bq.opentelemetry.tracerProvider.traceExporters[0].type", "console"))
                .createRuntime();
        OpenTelemetry otel = runtime.getInstance(OpenTelemetry.class);

        Span span = otel.getTracer("test").spanBuilder("console-span").startSpan();
        String output = captureStderr(span::end);
        assertTrue(output.contains("'console-span'"), "Expected span name in console output, got: " + output);
    }

    @Test
    public void noneExporter() {
        BQRuntime runtime = testFactory.app()
                .module(b -> BQCoreModule.extend(b).setProperty("bq.opentelemetry.tracerProvider.traceExporters[0].type", "none"))
                .createRuntime();
        OpenTelemetry otel = runtime.getInstance(OpenTelemetry.class);

        Span span = otel.getTracer("test").spanBuilder("none-span").startSpan();
        assertInstanceOf(ReadableSpan.class, span);
        String output = captureStderr(span::end);
        assertTrue(output.isEmpty(), "No export output expected with 'none' exporter, got: " + output);
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
