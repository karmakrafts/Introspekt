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

package dev.karmakrafts.trakkit

import kotlin.reflect.KClass

data class ClassInfo(
    val location: SourceLocation,
    val type: KClass<*>,
    val typeParameterNames: List<String>,
    val annotations: Map<KClass<out Annotation>, AnnotationInfo>,
    val functions: List<FunctionInfo>
) {
    companion object {
        @TrakkitIntrinsic(TrakkitIntrinsic.CI_CURRENT)
        fun current(): ClassInfo = throw TrakkitPluginNotAppliedException()
    }

    fun toFormattedString(): String {
        var result =
            if (annotations.isEmpty()) "" else "${annotations.values.joinToString("\n") { it.toFormattedString() }}\n"
        result += "class $type${if (typeParameterNames.isEmpty()) "" else "<${typeParameterNames.joinToString(", ")}>"}\n"
        result += "\tat $location"
        if (functions.isNotEmpty()) {
            result += "\n${functions.joinToString("\n") { "\t${it.toFormattedString()}" }}"
        }
        return result
    }
}