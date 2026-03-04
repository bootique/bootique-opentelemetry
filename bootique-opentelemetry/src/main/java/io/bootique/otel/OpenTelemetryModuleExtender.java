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
import io.bootique.BQCoreModuleExtender;
import io.bootique.ModuleExtender;
import io.bootique.command.Command;
import io.bootique.command.CommandDecorator;
import io.bootique.di.Binder;

/**
 * @since 4.0
 */
public class OpenTelemetryModuleExtender extends ModuleExtender<OpenTelemetryModuleExtender> {

    OpenTelemetryModuleExtender(Binder binder) {
        super(binder);
    }

    @Override
    public OpenTelemetryModuleExtender initAllExtensions() {
        return this;
    }

    /**
     * Declares all known OpenTelemetry environment variables, binding each to its corresponding Bootique configuration
     * property. Variables without a direct Bootique config equivalent are skipped.
     *
     * @return this extender instance.
     */
    public OpenTelemetryModuleExtender declareOtelVars() {
        BQCoreModuleExtender coreExtender = BQCoreModule.extend(binder);
        for (OpenTelemetryVar v : OpenTelemetryVar.values()) {
            if (v.configPath != null) {
                coreExtender.declareVar(v.configPath, v.name());
            }
        }
        return this;
    }

    public OpenTelemetryModuleExtender enableOpenTelemetryFor(Class<? extends Command> commandType) {
        BQCoreModule.extend(binder).decorateCommand(commandType, CommandDecorator.alsoRun(OpenTelemetryCommand.class));
        return this;
    }
}
