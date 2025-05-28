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

package dev.karmakrafts.introspekt.element

import co.touchlab.stately.collections.ConcurrentMutableMap
import dev.karmakrafts.introspekt.IntrospektCompilerApi
import dev.karmakrafts.introspekt.util.SourceLocation

/**
 * Represents information about a parameter in a function or constructor.
 *
 * This class provides access to metadata about a specific parameter, including its
 * location in source code, qualified name, simple name, type, and annotations.
 * It extends [AnnotatedElementInfo] to include annotation information.
 */
@ConsistentCopyVisibility
data class ParameterInfo private constructor( // @formatter:off
    override val location: SourceLocation,
    override val qualifiedName: String,
    override val name: String,

    /**
     * The type information for this parameter.
     */
    val type: TypeInfo,
    override val annotations: Map<TypeInfo, List<AnnotationUsageInfo>>
) : AnnotatedElementInfo { // @formatter:on
    companion object {
        private val cache: ConcurrentMutableMap<String, ParameterInfo> = ConcurrentMutableMap()

        /**
         * Creates or retrieves a cached [ParameterInfo] instance with the specified properties.
         *
         * This method is for internal use by the Introspekt compiler plugin.
         *
         * @param location The source code location of the parameter
         * @param qualifiedName The fully qualified name of the parameter
         * @param name The simple name of the parameter
         * @param type The type information for the parameter
         * @param annotations The annotations applied to the parameter
         * @return A [ParameterInfo] instance with the specified properties
         */
        @IntrospektCompilerApi
        internal fun getOrCreate(
            location: SourceLocation,
            qualifiedName: String,
            name: String,
            type: TypeInfo,
            annotations: Map<TypeInfo, List<AnnotationUsageInfo>>
        ): ParameterInfo = cache.getOrPut(qualifiedName) {
            ParameterInfo( // @formatter:off
                location = location,
                qualifiedName = qualifiedName,
                name = name,
                type = type,
                annotations = annotations
            ) // @formatter:on
        }
    }
}
