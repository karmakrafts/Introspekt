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
 * Represents information about a local variable in a function or code block.
 *
 * This class provides access to metadata about a specific local variable, including its
 * location in source code, qualified name, simple name, type, and mutability.
 * It extends [AnnotatedElementInfo] to include annotation information.
 */
@ConsistentCopyVisibility
data class LocalInfo private constructor( // @formatter:off
    override val location: SourceLocation,
    override val qualifiedName: String,
    override val name: String,

    /**
     * The type of this local variable.
     */
    val type: TypeInfo,

    /**
     * Indicates whether this local variable is mutable (var) or immutable (val).
     */
    val isMutable: Boolean,

    override val annotations: Map<TypeInfo, List<AnnotationUsageInfo>>
) : AnnotatedElementInfo { // @formatter:on
    companion object {
        private val cache: ConcurrentMutableMap<String, LocalInfo> = ConcurrentMutableMap()

        @IntrospektCompilerApi
        internal fun getOrCreate( // @formatter:off
            location: SourceLocation,
            qualifiedName: String,
            name: String,
            type: TypeInfo,
            isMutable: Boolean,
            annotations: Map<TypeInfo, List<AnnotationUsageInfo>>
        ): LocalInfo = cache.getOrPut(qualifiedName) {
            LocalInfo(
                location = location,
                qualifiedName = qualifiedName,
                name = name,
                type = type,
                isMutable = isMutable,
                annotations = annotations
            )
        } // @formatter:on
    }

    /**
     * Returns a formatted string representation of this local variable.
     *
     * The string includes the variable's mutability (var/val), name, and type.
     *
     * @param indent The number of tab characters to prepend to the string for indentation
     * @return A formatted string representation of this local variable
     */
    fun toFormattedString(indent: Int = 0): String {
        var result = "\t".repeat(indent)
        result += if (isMutable) "var " else "val "
        result += "$name: ${type.qualifiedName}"
        return result
    }
}
