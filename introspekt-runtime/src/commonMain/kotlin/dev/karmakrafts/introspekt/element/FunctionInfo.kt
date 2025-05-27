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
import dev.karmakrafts.introspekt.IntrospektIntrinsic
import dev.karmakrafts.introspekt.IntrospektPluginNotAppliedException
import dev.karmakrafts.introspekt.util.ModalityModifier
import dev.karmakrafts.introspekt.util.SourceLocation
import dev.karmakrafts.introspekt.util.VisibilityModifier
import kotlin.reflect.KFunction

data class FunctionInfo(
    override val location: SourceLocation,
    override val qualifiedName: String,
    override val name: String,
    val typeParameterNames: List<String>,
    val returnType: TypeInfo,
    val parameterTypes: List<TypeInfo>,
    val parameterNames: List<String>,
    val visibility: VisibilityModifier,
    val modality: ModalityModifier,
    val locals: List<LocalInfo>,
    override val isExpect: Boolean,
    override val annotations: Map<TypeInfo, List<AnnotationUsageInfo>>
) : AnnotatedElementInfo, ExpectableElementInfo {
    companion object {
        private val cache: ConcurrentMutableMap<Int, FunctionInfo> = ConcurrentMutableMap()

        @IntrospektIntrinsic(IntrospektIntrinsic.Type.FI_CURRENT)
        fun current(): FunctionInfo = throw IntrospektPluginNotAppliedException()

        @IntrospektIntrinsic(IntrospektIntrinsic.Type.FI_OF)
        fun of(function: KFunction<*>): FunctionInfo = throw IntrospektPluginNotAppliedException()

        private fun getCacheKey( // @formatter:off
            qualifiedName: String,
            returnType: TypeInfo,
            parameterTypes: List<TypeInfo>
        ): Int { // @formatter:on
            var result = qualifiedName.hashCode()
            result = 31 * result + returnType.hashCode()
            result = 31 * result + parameterTypes.hashCode()
            return result
        }

        @IntrospektCompilerApi
        internal fun getOrCreate(
            location: SourceLocation,
            qualifiedName: String,
            name: String,
            typeParameterNames: List<String>,
            returnType: TypeInfo,
            parameterTypes: List<TypeInfo>,
            parameterNames: List<String>,
            visibility: VisibilityModifier,
            modality: ModalityModifier,
            locals: List<LocalInfo>,
            isExpect: Boolean,
            annotations: Map<TypeInfo, List<AnnotationUsageInfo>>
        ): FunctionInfo {
            return cache.getOrPut(getCacheKey(qualifiedName, returnType, parameterTypes)) {
                FunctionInfo(
                    location = location,
                    qualifiedName = qualifiedName,
                    name = name,
                    typeParameterNames = typeParameterNames,
                    returnType = returnType,
                    parameterTypes = parameterTypes,
                    parameterNames = parameterNames,
                    visibility = visibility,
                    modality = modality,
                    locals = locals,
                    isExpect = isExpect,
                    annotations = annotations
                )
            }
        }
    }

    fun toFormattedString(indent: Int = 0): String {
        // Annotations
        var result = if (annotations.isEmpty()) ""
        else "${annotations.values.flatten().joinToString("\n") { it.toFormattedString(indent) }}\n"
        // Parameters
        val parameters = if (parameterNames.isEmpty()) ""
        else parameterNames.mapIndexed { index, name ->
            "$name: ${parameterTypes[index].qualifiedName}"
        }.joinToString(", ")
        // Function
        result += "${"\t".repeat(indent)}fun "
        if (typeParameterNames.isNotEmpty()) {
            result += "<${typeParameterNames.joinToString(", ")}> "
        }
        val escapedName = if (' ' in name) "`$name`" else name
        result += "$escapedName($parameters): ${returnType.qualifiedName}"
        for (local in locals) {
            result += "\n${local.toFormattedString(indent + 1)}"
        }
        return result
    }
}