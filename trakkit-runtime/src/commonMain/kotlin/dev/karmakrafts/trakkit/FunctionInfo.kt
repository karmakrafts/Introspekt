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
import kotlin.reflect.KFunction

data class FunctionInfo(
    val location: SourceLocation,
    val name: String,
    val typeParameterNames: List<String>,
    val returnType: KClass<*>,
    val parameterTypes: List<KClass<*>>,
    val parameterNames: List<String>,
    val annotations: Map<KClass<out Annotation>, AnnotationInfo>,
) {
    companion object {
        @TrakkitIntrinsic(TrakkitIntrinsic.FI_CURRENT)
        fun current(): FunctionInfo = throw TrakkitPluginNotAppliedException()

        @TrakkitIntrinsic(TrakkitIntrinsic.FI_OF)
        fun of(function: KFunction<*>): FunctionInfo = throw TrakkitPluginNotAppliedException()
    }

    fun toFormattedString(): String {
        var result =
            if (annotations.isEmpty()) "" else "${annotations.values.joinToString("\n") { it.toFormattedString() }}\n"
        result += "fun "
        if (typeParameterNames.isNotEmpty()) {
            result += "<${typeParameterNames.joinToString(", ")}> "
        }
        val parameters = if (parameterNames.isEmpty()) ""
        else parameterNames.mapIndexed { index, name ->
            "$name: ${parameterTypes[index].getQualifiedName()}"
        }.joinToString(", ")
        val escapedName = if (' ' in name) "`$name`" else name
        result += "$escapedName($parameters): ${returnType.getQualifiedName()}\n"
        result += "\tat $location"
        return result
    }
}