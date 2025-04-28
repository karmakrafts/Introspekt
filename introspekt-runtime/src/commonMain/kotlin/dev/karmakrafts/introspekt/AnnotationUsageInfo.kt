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

package dev.karmakrafts.introspekt

import kotlin.reflect.KClass

@ConsistentCopyVisibility
data class AnnotationUsageInfo @IntrospektCompilerApi internal constructor( // @formatter:off
    val location: SourceLocation,
    val type: KClass<out Annotation>,
    val values: Map<String, Any?>
) { // @formatter:on
    @Suppress("UNCHECKED_CAST")
    fun <V> getValue(name: String): V = values[name]!! as V

    @Suppress("UNCHECKED_CAST")
    fun <V> getValueOrNull(name: String): V? = values[name] as? V

    fun toFormattedString(indent: Int = 0): String {
        val indentString = "\t".repeat(indent)
        var result = "$indentString@${type.getQualifiedName()}"
        if (values.isNotEmpty()) {
            result += "($values)"
        }
        return result
    }
}
