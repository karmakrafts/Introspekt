/*
 * Copyright 2025 Karma Krafts & associates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.karmakrafts.trakkit.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import javax.inject.Inject

/**
 * Gradle plugin for integrating the Trakkit compiler plugin with Kotlin projects.
 *
 * This plugin implements the [KotlinCompilerPluginSupportPlugin] interface to register
 * the Trakkit compiler plugin with the Kotlin compiler during project builds. It handles
 * the integration between Gradle and the Kotlin compiler plugin infrastructure.
 *
 * @property providerFactory The Gradle [ProviderFactory] used to create providers for plugin options.
 */
@Suppress("UNUSED")
open class TrakkitGradlePlugin @Inject constructor(
    private val providerFactory: ProviderFactory
) : KotlinCompilerPluginSupportPlugin {
    /**
     * Applies this plugin to the given project.
     *
     * This method is called when the plugin is applied to a Gradle project. It logs the
     * plugin version and calls the superclass implementation to register the compiler plugin.
     *
     * @param target The Gradle project to which this plugin is being applied.
     */
    override fun apply(target: Project) {
        target.logger.info("Trakkit Compiler Plugin ${BuildInfo.version}")
        super.apply(target) // Allow compiler plugin to be registered
    }

    /**
     * Provides the compiler plugin options for a specific Kotlin compilation.
     *
     * This method is called for each Kotlin compilation to provide plugin-specific options.
     * Currently, this plugin doesn't require any specific options, so it returns an empty list.
     *
     * @param kotlinCompilation The Kotlin compilation being processed.
     * @return A provider of compiler plugin options (empty in this implementation).
     */
    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        return providerFactory.provider { emptyList() }
    }

    /**
     * Returns the ID of the compiler plugin.
     *
     * This ID is used to identify the plugin in the Kotlin compiler.
     *
     * @return The compiler plugin ID from [BuildInfo.PLUGIN_NAME].
     */
    override fun getCompilerPluginId(): String = BuildInfo.PLUGIN_NAME

    /**
     * Returns the artifact information for the compiler plugin.
     *
     * This information is used to locate and load the compiler plugin JAR.
     *
     * @return The [SubpluginArtifact] instance from [BuildInfo.pluginArtifact].
     */
    override fun getPluginArtifact(): SubpluginArtifact = BuildInfo.pluginArtifact

    /**
     * Determines whether this plugin should be applied to the given Kotlin compilation.
     *
     * This plugin is applicable to all Kotlin compilations.
     *
     * @param kotlinCompilation The Kotlin compilation to check.
     * @return Always returns true, as this plugin is applicable to all compilations.
     */
    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true
}
