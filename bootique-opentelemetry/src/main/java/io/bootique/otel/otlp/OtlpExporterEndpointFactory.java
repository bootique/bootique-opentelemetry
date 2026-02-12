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

import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;

import java.util.Map;

/**
 * @since 4.0
 */
@BQConfig("An optional shared OTLP configuration that supplies defaults for OTLP trace, metric, log exporters")
public class OtlpExporterEndpointFactory {

    private static final String PROTOCOL_GRPC = "grpc";
    private static final String PROTOCOL_HTTP_PROTOBUF = "http/protobuf";

    private static final String DEFAULT_GRPC_URL = "http://localhost:4317";
    private static final String DEFAULT_HTTP_URL = "http://localhost:4318";

    private String url;
    private String protocol;
    private Map<String, String> headers;

    @BQConfigProperty("""
            A URL of the base exporter. The default is "http://localhost:4318" for HTTP protocols
            and "http://localhost:4317" - for GRPC""")
    public OtlpExporterEndpointFactory setUrl(String url) {
        this.url = url;
        return this;
    }

    @BQConfigProperty("""
            OTLP protocol to use to export data. Should be one of "grpc", "http/protobuf", "http/json".
            If not specified, "http/protobuf" is assumed.""")
    public OtlpExporterEndpointFactory setProtocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    @BQConfigProperty("""
            Additional headers as a list of key-value pairs to add in outgoing gRPC or HTTP requests. E.g.:
            'api-key=key,other-config-value=value'""")
    public OtlpExporterEndpointFactory setHeaders(Map<String, String> headers) {
        this.headers = headers;
        return this;
    }

    public OtlpExporterEndpoint create() {

        // TODO: timeout (OTEL_EXPORTER_OTLP_TIMEOUT)

        OtlpProtocol protocol = switch (this.protocol) {
            case PROTOCOL_GRPC -> OtlpProtocol.grpc;
            case PROTOCOL_HTTP_PROTOBUF -> OtlpProtocol.http_protobuf;
            default -> throw new IllegalArgumentException(String.format(
                    "Unsupported OTLP protocol: '%s'. Must be one of '%s' or '%s'",
                    this.protocol,
                    PROTOCOL_GRPC,
                    PROTOCOL_HTTP_PROTOBUF));
        };

        String url = this.url != null
                ? this.url
                : (protocol == OtlpProtocol.grpc ? DEFAULT_GRPC_URL : DEFAULT_HTTP_URL);

        Map<String, String> headers = this.headers != null ? this.headers : Map.of();
        return new OtlpExporterEndpoint(url, protocol, headers);
    }
}
