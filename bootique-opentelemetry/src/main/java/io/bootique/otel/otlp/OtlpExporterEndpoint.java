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
package io.bootique.otel.otlp;

import java.util.Map;

/**
 * A descriptor of a base remote endpoint that exporters for all three types of OpenTelemetry signals (traces,
 * metrics, logs) can connect to.
 *
 * @see <a href="https://opentelemetry.io/docs/specs/otel/protocol/exporter/">OpenTelemetry Protocol Exporter</a>
 * @since 4.0
 */
public record OtlpExporterEndpoint(String endpointUrl, OtlpProtocol protocol, Map<String, String> headers) {

    public OtlpExporterEndpoint(String endpointUrl, OtlpProtocol protocol, Map<String, String> headers) {
        this.endpointUrl = endpointUrl.endsWith("/") ? endpointUrl : endpointUrl + "/";
        this.protocol = protocol;
        this.headers = headers;
    }

    // URL structures are built per
    // https://opentelemetry.io/docs/specs/otel/protocol/exporter/#endpoint-urls-for-otlphttp

    public String tracesEndpointUrl() {
        return endpointUrl + "v1/traces";
    }

    public String metricsEndpointUrl() {
        return endpointUrl + "v1/metrics";
    }

    public String logsEndpointUrl() {
        return endpointUrl + "v1/logs";
    }
}
