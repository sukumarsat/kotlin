/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.configuration

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.test.directives.DirectivesContainer
import org.jetbrains.kotlin.test.directives.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.util.StringUtils.joinToArrayString

class JsEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override val directivesContainer: DirectivesContainer
        get() = JsEnvironmentConfigurationDirectives

    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        val moduleKinds = module.directives[JsEnvironmentConfigurationDirectives.moduleKind]
        val moduleKind = when (moduleKinds.size) {
            0 -> ModuleKind.PLAIN
            1 -> moduleKinds.single()
            else -> error("Too many module kinds passed ${moduleKinds.joinToArrayString()}")
        }
        configuration.put(JSConfigurationKeys.MODULE_KIND, moduleKind)

        configuration.put(JSConfigurationKeys.LIBRARIES, JsConfig.JS_STDLIB)
    }
}

object JsEnvironmentConfigurationDirectives : SimpleDirectivesContainer() {
    val moduleKind = enumDirective<ModuleKind>("MODULE_KIND", "Specifies kind of js module")
}
