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
import io.bootique.junit.BQTest;
import io.bootique.junit.BQTestFactory;
import io.bootique.junit.BQTestTool;
import io.bootique.otel.otlp.OtlpExporterEndpoint;
import io.bootique.otel.otlp.OtlpProtocol;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
