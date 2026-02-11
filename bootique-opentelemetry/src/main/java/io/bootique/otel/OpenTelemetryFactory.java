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

import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;
import io.bootique.otel.meter.SdkMeterProviderFactory;
import io.bootique.otel.trace.SdkTracerProviderFactory;
import io.bootique.shutdown.ShutdownManager;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import jakarta.inject.Inject;

/**
 * @since 4.0
 */
@BQConfig
public class OpenTelemetryFactory {

    private final Resource resource;
    private final ShutdownManager shutdownManager;

    private SdkMeterProviderFactory meterProvider;
    private SdkTracerProviderFactory tracerProvider;

    @Inject
    public OpenTelemetryFactory(Resource resource, ShutdownManager shutdownManager) {
        this.resource = resource;
        this.shutdownManager = shutdownManager;
    }

    @BQConfigProperty
    public OpenTelemetryFactory setMeterProvider(SdkMeterProviderFactory meterProvider) {
        this.meterProvider = meterProvider;
        return this;
    }

    @BQConfigProperty
    public OpenTelemetryFactory setTracerProvider(SdkTracerProviderFactory tracerProvider) {
        this.tracerProvider = tracerProvider;
        return this;
    }

    public OpenTelemetry create() {
        SdkMeterProvider meterProvider = meterProviderOrDefault().create();
        SdkTracerProvider tracerProvider = traceProviderOrDefault().create(meterProvider);

        return OpenTelemetrySdk
                .builder()
                .setTracerProvider(tracerProvider)
                .setMeterProvider(meterProvider)
                .build();
    }

    private SdkMeterProviderFactory meterProviderOrDefault() {
        return meterProvider != null
                ? meterProvider
                : new SdkMeterProviderFactory(resource, shutdownManager);
    }

    private SdkTracerProviderFactory traceProviderOrDefault() {
        return tracerProvider != null
                ? tracerProvider
                : new SdkTracerProviderFactory(resource, shutdownManager);
    }
}
