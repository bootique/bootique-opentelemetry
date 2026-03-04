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

import io.bootique.BQCoreModule;
import io.bootique.BQRuntime;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import io.opentelemetry.api.OpenTelemetry;
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
public class OtlpLogsExporterIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(OtlpLogsExporterIT.class);

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
                        .setProperty("bq.opentelemetry.loggerProvider.exporters[0].type", "otlp")
                        .setProperty("bq.opentelemetry.otlp.protocol", "grpc")
                        .setProperty("bq.opentelemetry.otlp.url",
                                "http://localhost:" + otelCollector.getMappedPort(4317)))
                .createRuntime();

        OpenTelemetry otel = runtime.getInstance(OpenTelemetry.class);
        otel.getLogsBridge().get("test-logger")
                .logRecordBuilder()
                .setBody("test-log-body-grpc")
                .emit();

        assertTrue(readExportedLogs(8_000L, "test-log-body-grpc"), "Expected 'test-log-body-grpc' to be exported");
    }

    @Test
    public void httpProtobufExport() throws InterruptedException {

        BQRuntime runtime = testFactory.app()
                .module(b -> BQCoreModule.extend(b)
                        .setProperty("bq.opentelemetry.loggerProvider.exporters[0].type", "otlp")
                        .setProperty("bq.opentelemetry.otlp.protocol", "http/protobuf")
                        .setProperty("bq.opentelemetry.otlp.url",
                                "http://localhost:" + otelCollector.getMappedPort(4318)))
                .createRuntime();

        OpenTelemetry otel = runtime.getInstance(OpenTelemetry.class);
        otel.getLogsBridge().get("test-logger")
                .logRecordBuilder()
                .setBody("test-log-body-http")
                .emit();

        assertTrue(readExportedLogs(8_000L, "test-log-body-http"), "Expected 'test-log-body-http' to be exported");
    }

    private boolean readExportedLogs(long timeoutMs, String logBody) throws InterruptedException {

        long sleep = timeoutMs < 500 ? timeoutMs : 500;
        long tries = timeoutMs / sleep + (timeoutMs % sleep > 0 ? 1 : 0);

        for (int i = 0; i < tries; i++) {
            Thread.sleep(sleep);

            if (i > 0) {
                LOGGER.info("reading container logs, attempt {}", i + 1);
            }

            if (otelCollector.getLogs().contains(logBody)) {
                return true;
            }
        }

        return false;
    }
}
