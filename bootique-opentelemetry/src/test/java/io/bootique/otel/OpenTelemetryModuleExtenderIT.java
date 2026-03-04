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
import io.bootique.cli.Cli;
import io.bootique.command.CommandOutcome;
import io.bootique.command.CommandWithMetadata;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import io.bootique.meta.application.CommandMetadata;
import io.bootique.otel.otlp.OtlpExporterEndpoint;
import io.bootique.otel.otlp.OtlpProtocol;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

@BQTest
public class OpenTelemetryModuleExtenderIT {

    @BQTestTool
    static final BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    @Test
    public void declareOtelVars() {
        BQRuntime runtime = testFactory.app()
                .module(b -> {
                    OpenTelemetryModule.extend(b).declareOtelVars();
                    BQCoreModule.extend(b).setVar("OTEL_EXPORTER_OTLP_PROTOCOL", "grpc");
                })
                .createRuntime();

        OtlpExporterEndpoint endpoint = runtime.getInstance(OtlpExporterEndpoint.class);
        assertEquals(OtlpProtocol.grpc, endpoint.protocol());
    }

    @Test
    public void enableOpenTelemetryFor_notEnabled() throws InterruptedException {
        BQRuntime runtime = testFactory.app("--test")
                .module(b -> BQCoreModule.extend(b).addCommand(TestCommand.class))
                .createRuntime();

        ByteArrayOutputStream capturedErr = new ByteArrayOutputStream();
        PrintStream origErr = System.err;
        System.setErr(new PrintStream(capturedErr));
        try {
            CommandOutcome outcome = runtime.run();
            assertTrue(outcome.isSuccess());
            Thread.sleep(500);
            assertFalse(capturedErr.toString().contains("OpenTelemetry started"));
        } finally {
            System.setErr(origErr);
        }
    }

    @Test
    public void enableOpenTelemetryFor() throws InterruptedException {
        BQRuntime runtime = testFactory.app("--test")
                .module(b -> {
                    BQCoreModule.extend(b).addCommand(TestCommand.class);
                    OpenTelemetryModule.extend(b).enableOpenTelemetryFor(TestCommand.class);
                })
                .createRuntime();

        ByteArrayOutputStream capturedErr = new ByteArrayOutputStream();
        PrintStream origErr = System.err;
        System.setErr(new PrintStream(capturedErr));
        try {
            CommandOutcome outcome = runtime.run();
            assertTrue(outcome.isSuccess());
            assertFalse(outcome.forkedToBackground());

            // OpenTelemetryCommand runs asynchronously via "alsoRun" - poll for its startup log
            long deadline = System.currentTimeMillis() + 3_000;
            while (!capturedErr.toString().contains("OpenTelemetry started")) {
                if (System.currentTimeMillis() >= deadline) {
                    fail("Timed out waiting for 'OpenTelemetry started' log");
                }
                Thread.sleep(100);
            }
        } finally {
            System.setErr(origErr);
        }
    }

    static class TestCommand extends CommandWithMetadata {
        public TestCommand() {
            super(CommandMetadata.builder("test").build());
        }

        @Override
        public CommandOutcome run(Cli cli) {
            return CommandOutcome.succeeded();
        }
    }
}
