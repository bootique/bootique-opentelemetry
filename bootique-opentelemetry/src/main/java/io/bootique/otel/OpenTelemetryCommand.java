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

import io.bootique.cli.Cli;
import io.bootique.command.CommandOutcome;
import io.bootique.command.CommandWithMetadata;
import io.bootique.meta.application.CommandMetadata;
import io.opentelemetry.api.OpenTelemetry;
import jakarta.inject.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A command that starts OpenTelemetry subsystem. It is declared as a hidden command. To activate OpenTelemetry, you'd
 * use {@link io.bootique.command.CommandDecorator} to attach it to some other command.
 *
 * @since 4.0
 */
public class OpenTelemetryCommand extends CommandWithMetadata {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenTelemetryCommand.class);

    private static CommandMetadata createMetadata() {
        return CommandMetadata.builder(OpenTelemetryCommand.class)
                .description("Starts OpenTelemetry subsystem")
                // there are no practical scenarios when OpenTelemetryCommand needs to run standalone,
                // so hide it from the app CLI.
                .hidden()
                .build();
    }

    private final Provider<OpenTelemetry> openTelemetryProvider;

    public OpenTelemetryCommand(Provider<OpenTelemetry> openTelemetryProvider) {
        super(createMetadata());
        this.openTelemetryProvider = openTelemetryProvider;
    }


    @Override
    public CommandOutcome run(Cli cli) {
        openTelemetryProvider.get();
        LOGGER.info("OpenTelemetry started");
        return CommandOutcome.succeededAndForkedToBackground();
    }
}
