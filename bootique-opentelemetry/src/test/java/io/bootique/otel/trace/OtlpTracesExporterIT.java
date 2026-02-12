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

import io.bootique.BQCoreModule;
import io.bootique.BQRuntime;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@BQTest
public class OtlpTracesExporterIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(OtlpTracesExporterIT.class);

    @BQTestTool
    private static final BQTestFactory testFactory = new BQTestFactory().autoLoadModules();
    private static GenericContainer<?> otelCollector;
    private static int lastReadLineCount = 0;

    @BeforeAll
    static void setupCollector() {
        otelCollector = new GenericContainer<>("otel/opentelemetry-collector-contrib:0.95.0")
                .withCopyFileToContainer(
                        MountableFile.forClasspathResource("otel-collector-config.yaml"),
                        "/etc/otel-collector-config.yaml")
                .withCommand("--config=/etc/otel-collector-config.yaml")
                .withExposedPorts(4317, 4318)
                .waitingFor(Wait.forLogMessage(".*Everything is ready.*", 1)
                        .withStartupTimeout(Duration.ofSeconds(30)));

        otelCollector.start();
    }

    @AfterAll
    static void teardownCollector() {
        if (otelCollector != null) {
            otelCollector.stop();
        }
    }

    @Test
    public void testGrpcExport() throws InterruptedException {

        BQRuntime runtime = testFactory.app()
                .module(b -> BQCoreModule.extend(b)
                        .setProperty("bq.opentelemetry.tracerProvider.traceExporters[0].type", "otlp")
                        .setProperty("bq.opentelemetry.otlp.protocol", "grpc")
                        .setProperty("bq.opentelemetry.otlp.url",
                                "http://localhost:" + otelCollector.getMappedPort(4317)))
                .createRuntime();


        OpenTelemetry otel = runtime.getInstance(OpenTelemetry.class);
        Span testSpan = otel.getTracer("test-tracer")
                .spanBuilder("test-span-grpc")
                .setAttribute("test.key", "test-value")
                .startSpan();
        testSpan.end();

        List<SpanInfo> spans = readExportedSpans(8_000L);

        assertFalse(spans.isEmpty(), "Expected at least one span to be exported");

        SpanInfo span = spans.stream()
                .filter(s -> "test-span-grpc".equals(s.name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("test-span-grpc not found"));

        assertTrue(span.attributes.containsKey("test.key"), "test.key attribute should be present");
        assertTrue(span.attributes.get("test.key").contains("test-value"), "test.key should have value 'test-value'");
    }

    @Test
    public void testHttpProtobufExport() throws InterruptedException {
        // no need for BQTestFactory, we'll be doing manual shutdown,
        BQRuntime runtime = testFactory.app()
                .module(b -> BQCoreModule.extend(b)
                        .setProperty("bq.opentelemetry.tracerProvider.traceExporters[0].type", "otlp")
                        .setProperty("bq.opentelemetry.otlp.protocol", "http/protobuf")
                        .setProperty("bq.opentelemetry.otlp.url",
                                "http://localhost:" + otelCollector.getMappedPort(4318)))
                .createRuntime();


        OpenTelemetry otel = runtime.getInstance(OpenTelemetry.class);
        Span testSpan = otel.getTracer("test-tracer")
                .spanBuilder("test-span-http")
                .setAttribute("http.test", "http-value")
                .startSpan();
        testSpan.end();

        List<SpanInfo> spans = readExportedSpans(8_000L);

        assertFalse(spans.isEmpty(), "Expected at least one span to be exported");

        SpanInfo span = spans.stream()
                .filter(s -> "test-span-http".equals(s.name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("test-span-http not found"));

        assertTrue(span.attributes.containsKey("http.test"), "http.test attribute should be present");
        assertTrue(span.attributes.get("http.test").contains("http-value"), "http.test should have value 'http-value'");
    }

    private List<SpanInfo> readExportedSpans(long timeoutMs) throws InterruptedException {

        long sleep = timeoutMs < 500 ? timeoutMs : 500;
        long tries = timeoutMs / sleep + (timeoutMs % sleep > 0 ? 1 : 0);

        for (int i = 0; i < tries; i++) {
            Thread.sleep(sleep);

            if (i > 0) {
                LOGGER.info("reading container spans, attempt {}", i + 1);
            }

            List<SpanInfo> spans = readExportedSpans();
            if (!spans.isEmpty()) {
                return spans;
            }
        }

        return List.of();
    }

    private List<SpanInfo> readExportedSpans() {
        String logs = otelCollector.getLogs();
        List<SpanInfo> spans = new ArrayList<>();

        String[] lines = logs.split("\n");
        SpanInfo currentSpan = null;

        // Skip lines we've already seen in previous test runs
        for (int i = lastReadLineCount; i < lines.length; i++) {
            String line = lines[i];

            if (line.contains("Span #")) {
                if (currentSpan != null) {
                    spans.add(currentSpan);
                }
                currentSpan = new SpanInfo();
            } else if (currentSpan != null) {
                if (line.contains("Name           :")) {
                    currentSpan.name = line.split(":", 2)[1].trim();
                } else if (line.contains("->") && line.contains(":")) {
                    // Parse attributes like "     -> test.key: Str(test-value)"
                    String attrLine = line.substring(line.indexOf("->") + 2).trim();
                    int colonIdx = attrLine.indexOf(":");
                    if (colonIdx > 0) {
                        String key = attrLine.substring(0, colonIdx).trim();
                        String value = attrLine.substring(colonIdx + 1).trim();
                        currentSpan.attributes.put(key, value);
                    }
                }
            }
        }

        if (currentSpan != null) {
            spans.add(currentSpan);
        }

        lastReadLineCount = lines.length;

        return spans;
    }

    private static class SpanInfo {
        String name;
        final Map<String, String> attributes = new HashMap<>();
    }
}
