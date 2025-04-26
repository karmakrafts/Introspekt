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

import co.touchlab.stately.collections.SharedHashMap
import kotlin.reflect.KClass

data class ClassInfo(
    override val location: SourceLocation,
    val type: KClass<*>,
    val qualifiedName: String,
    val typeParameterNames: List<String>,
    val annotations: Map<KClass<out Annotation>, AnnotationUsageInfo>,
    val functions: List<FunctionInfo>,
    val properties: List<PropertyInfo>,
    val companionObjects: List<ClassInfo>,
    val isInterface: Boolean,
    val isObject: Boolean,
    val isCompanionObject: Boolean,
    val visibility: VisibilityModifier,
    val modality: ModalityModifier,
    val classModifier: ClassModifier?
) : ElementInfo {
    companion object {
        private val cache: SharedHashMap<KClass<*>, ClassInfo> = SharedHashMap()

        @TrakkitIntrinsic(TrakkitIntrinsic.CI_CURRENT)
        fun current(): ClassInfo = throw TrakkitPluginNotAppliedException()

        @TrakkitIntrinsic(TrakkitIntrinsic.CI_OF)
        fun <T : Any> of(): ClassInfo = throw TrakkitPluginNotAppliedException()

        @TrakkitCompilerApi
        internal fun getOrCreate(
            location: SourceLocation,
            type: KClass<*>,
            qualifiedName: String,
            typeParameterNames: List<String>,
            annotations: Map<KClass<out Annotation>, AnnotationUsageInfo>,
            functions: List<FunctionInfo>,
            properties: List<PropertyInfo>,
            companionObjects: List<ClassInfo>,
            isInterface: Boolean,
            isObject: Boolean,
            isCompanionObject: Boolean,
            visibility: VisibilityModifier,
            modality: ModalityModifier,
            classModifier: ClassModifier?
        ): ClassInfo {
            return cache.getOrPut(type) {
                ClassInfo(
                    location,
                    type,
                    qualifiedName,
                    typeParameterNames,
                    annotations,
                    functions,
                    properties,
                    companionObjects,
                    isInterface,
                    isObject,
                    isCompanionObject,
                    visibility,
                    modality,
                    classModifier
                )
            }
        }
    }

    fun toFormattedString(indent: Int = 0): String {
        val indentString = "\t".repeat(indent)
        // Annotations
        var result = if (annotations.isEmpty()) ""
        else "${annotations.values.joinToString("\n") { it.toFormattedString(indent) }}\n"
        // Class
        val qualifier = when {
            isInterface -> "interface"
            isCompanionObject -> "companion object"
            isObject -> "object"
            else -> "class"
        }
        val typeParams = if (typeParameterNames.isEmpty()) "" else "<${typeParameterNames.joinToString(", ")}>"
        val classModifier = classModifier?.toString()?.let { "$it " } ?: ""
        result += "$indentString$visibility $modality $classModifier$qualifier $qualifiedName$typeParams {\n"
        // Properties
        val hasProperties = properties.isNotEmpty()
        if (hasProperties) {
            result += "${properties.joinToString("\n") { it.toFormattedString(indent + 1) }}\n"
        }
        // Functions
        if (functions.isNotEmpty()) {
            if (hasProperties) result += '\n'
            result += "${functions.joinToString("\n\n") { it.toFormattedString(indent + 1) }}\n"
        }
        result += "$indentString}"
        return result
    }

    override fun hashCode(): Int = type.hashCode() // Use type identity

    override fun equals(other: Any?): Boolean {
        return if (other !is ClassInfo) false
        else type === other.type // Use type identity
    }
}