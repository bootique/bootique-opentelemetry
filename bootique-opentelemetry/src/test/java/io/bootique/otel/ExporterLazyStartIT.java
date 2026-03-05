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

import io.bootique.BQRuntime;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

@BQTest
public class ExporterLazyStartIT {

    @BQTestTool
    static final BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    @Test
    public void exportStartsOnOtelAccessWithTrace() {
        BQRuntime runtime = testFactory.app().createRuntime();

        // Access OpenTelemetry and add a trace — this should trigger the exporter
        OpenTelemetry otel = runtime.getInstance(OpenTelemetry.class);
        Span span = otel.getTracer("test").spanBuilder("lazy-start-span").startSpan();
        String output = captureStderr(span::end);

        assertTrue(output.contains("'lazy-start-span'"),
                () -> "Expected span export after OpenTelemetry access with trace, got: " + output);
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
