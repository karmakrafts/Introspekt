/*
 * Copyright 2025 Karma Krafts
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

import dev.karmakrafts.introspekt.IntrospektCompilerApi
import dev.karmakrafts.introspekt.util.SourceLocation

/**
 * Represents information about an annotation usage in code.
 *
 * This class provides access to metadata about a specific annotation usage, including its
 * location in source code, the annotation type, and the values provided for the annotation's parameters.
 */
@ConsistentCopyVisibility
data class AnnotationUsageInfo @IntrospektCompilerApi internal constructor( // @formatter:off
    /**
     * The source code location where this annotation is applied.
     */
    val location: SourceLocation,

    /**
     * The type information for the annotation class.
     */
    val type: TypeInfo,

    /**
     * A map of annotation parameter names to their values.
     *
     * The keys are the parameter names as strings, and the values are the parameter values,
     * which can be of various types depending on the annotation parameter type.
     */
    val values: Map<String, Any?>
) { // @formatter:on
    /**
     * Gets the value of the annotation parameter with the specified name.
     *
     * @param V The expected type of the parameter value
     * @param name The name of the annotation parameter
     * @return The value of the parameter cast to type V
     * @throws NullPointerException if the parameter with the specified name does not exist
     * @throws ClassCastException if the parameter value cannot be cast to type V
     */
    @Suppress("UNCHECKED_CAST")
    fun <V> getValue(name: String): V = values[name]!! as V

    /**
     * Gets the value of the annotation parameter with the specified name, or null if it doesn't exist
     * or cannot be cast to the expected type.
     *
     * @param V The expected type of the parameter value
     * @param name The name of the annotation parameter
     * @return The value of the parameter cast to type V, or null if the parameter doesn't exist
     *         or cannot be cast to type V
     */
    @Suppress("UNCHECKED_CAST")
    fun <V> getValueOrNull(name: String): V? = values[name] as? V

    /**
     * Returns a formatted string representation of this annotation usage.
     *
     * The string includes the annotation type name prefixed with '@' and, if the annotation
     * has parameters, the parameter values in parentheses.
     *
     * @param indent The number of tab characters to prepend to the string for indentation
     * @return A formatted string representation of this annotation usage
     */
    fun toFormattedString(indent: Int = 0): String {
        val indentString = "\t".repeat(indent)
        var result = "$indentString@${type.qualifiedName}"
        if (values.isNotEmpty()) {
            result += "($values)"
        }
        return result
    }
}
