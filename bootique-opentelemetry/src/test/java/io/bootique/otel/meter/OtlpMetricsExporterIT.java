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

import io.bootique.BQCoreModule;
import io.bootique.BQRuntime;
import io.bootique.junit.BQTest;
import io.bootique.junit.BQTestFactory;
import io.bootique.junit.BQTestTool;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.LongCounter;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

@BQTest
@Testcontainers
public class OtlpMetricsExporterIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(OtlpMetricsExporterIT.class);

    @Container
    private static final GenericContainer<?> otelCollector = new GenericContainer<>("otel/opentelemetry-collector-contrib:0.95.0")
            .withCopyFileToContainer(MountableFile.forClasspathResource("otel-collector-config.yaml"), "/etc/otel-collector-config.yaml")
            .withCommand("--config=/etc/otel-collector-config.yaml")
            .withExposedPorts(4317, 4318)
            .waitingFor(Wait.forLogMessage(".*Everything is ready.*", 1).withStartupTimeout(Duration.ofSeconds(30)));

    @BQTestTool
    private static final BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    @Test
    public void grpcExport() throws InterruptedException {

        BQRuntime runtime = testFactory.app()
                .module(b -> BQCoreModule.extend(b)
                        .setProperty("bq.opentelemetry.meterProvider.exporters[0].type", "otlp")
                        .setProperty("bq.opentelemetry.meterProvider.exportInterval", "200ms")
                        .setProperty("bq.opentelemetry.otlp.protocol", "grpc")
                        .setProperty("bq.opentelemetry.otlp.url",
                                "http://localhost:" + otelCollector.getMappedPort(4317)))
                .createRuntime();

        OpenTelemetry otel = runtime.getInstance(OpenTelemetry.class);
        LongCounter counter = otel.getMeter("test-meter").counterBuilder("test-counter-grpc").build();
        counter.add(5);

        assertTrue(readExportedMetrics(5_000L, "test-counter-grpc"), "Expected 'test-counter-grpc' to be exported");
    }

    @Test
    public void httpProtobufExport() throws InterruptedException {

        BQRuntime runtime = testFactory.app()
                .module(b -> BQCoreModule.extend(b)
                        .setProperty("bq.opentelemetry.meterProvider.exporters[0].type", "otlp")
                        .setProperty("bq.opentelemetry.meterProvider.exportInterval", "200ms")
                        .setProperty("bq.opentelemetry.otlp.protocol", "http/protobuf")
                        .setProperty("bq.opentelemetry.otlp.url",
                                "http://localhost:" + otelCollector.getMappedPort(4318)))
                .createRuntime();

        OpenTelemetry otel = runtime.getInstance(OpenTelemetry.class);
        LongCounter counter = otel.getMeter("test-meter").counterBuilder("test-counter-http").build();
        counter.add(3);

        assertTrue(readExportedMetrics(5_000L, "test-counter-http"), "Expected 'test-counter-http' to be exported");
    }

    private boolean readExportedMetrics(long timeoutMs, String metricName) throws InterruptedException {

        long sleep = timeoutMs < 200 ? timeoutMs : 200;
        long tries = timeoutMs / sleep + (timeoutMs % sleep > 0 ? 1 : 0);

        for (int i = 0; i < tries; i++) {
            Thread.sleep(sleep);

            if (i > 0) {
                LOGGER.info("reading container metrics, attempt {}", i + 1);
            }

            if (otelCollector.getLogs().contains(metricName)) {
                return true;
            }
        }

        return false;
    }
}
