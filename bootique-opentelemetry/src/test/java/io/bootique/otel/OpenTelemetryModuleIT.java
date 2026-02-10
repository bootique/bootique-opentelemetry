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
import io.bootique.meta.application.ApplicationMetadata;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.resources.Resource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@BQTest
public class OpenTelemetryModuleIT {

    @BQTestTool
    static final BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    @Test
    public void resource() {
        BQRuntime runtime = testFactory.app().createRuntime();
        ApplicationMetadata md = runtime.getInstance(ApplicationMetadata.class);
        Resource resource = runtime.getInstance(Resource.class);

        assertEquals(md.getName(), resource.getAttribute(AttributeKey.stringKey("service.name")));
        assertEquals("opentelemetry", resource.getAttribute(AttributeKey.stringKey("telemetry.sdk.name")));
        assertEquals("java", resource.getAttribute(AttributeKey.stringKey("telemetry.sdk.language")));
    }

    @Test
    public void openTelemetry() {
        assertFalse(GlobalOpenTelemetry.isSet(), "OpenTelemetry agent should not be active");
        OpenTelemetry otel = testFactory.app().createRuntime().getInstance(OpenTelemetry.class);
        assertNotNull(otel);
        assertFalse(GlobalOpenTelemetry.isSet(), "GlobalOpenTelemetry should not be set");
    }
}
