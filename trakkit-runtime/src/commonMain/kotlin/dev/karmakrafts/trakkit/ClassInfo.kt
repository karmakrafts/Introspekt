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
    val functions: List<FunctionInfo>,
    val properties: List<PropertyInfo>,
    val companionObjects: List<ClassInfo>,
    val isInterface: Boolean,
    val isObject: Boolean,
    val isCompanionObject: Boolean,
    val visibility: VisibilityModifier,
    val modality: ModalityModifier,
    val classModifier: ClassModifier?
) {
    companion object {
        @TrakkitIntrinsic(TrakkitIntrinsic.CI_CURRENT)
        fun current(): ClassInfo = throw TrakkitPluginNotAppliedException()

        @TrakkitIntrinsic(TrakkitIntrinsic.CI_OF)
        fun <T : Any> of(): ClassInfo = throw TrakkitPluginNotAppliedException()
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
        result += "$indentString$visibility $modality $classModifier$qualifier ${type.getQualifiedName()}$typeParams {\n"
        // Functions
        if (functions.isNotEmpty()) {
            result += "${functions.joinToString("\n\n") { it.toFormattedString(indent + 1) }}\n"
        }
        result += "$indentString}"
        return result
    }
}