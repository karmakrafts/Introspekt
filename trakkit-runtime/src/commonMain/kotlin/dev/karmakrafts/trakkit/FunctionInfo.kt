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
import kotlin.reflect.KFunction

data class FunctionInfo(
    override val location: SourceLocation,
    val qualifiedName: String,
    val name: String,
    val typeParameterNames: List<String>,
    val returnType: KClass<*>,
    val parameterTypes: List<KClass<*>>,
    val parameterNames: List<String>,
    val annotations: Map<KClass<out Annotation>, List<AnnotationUsageInfo>>
) : ElementInfo {
    companion object {
        private val cache: SharedHashMap<Int, FunctionInfo> = SharedHashMap()

        @TrakkitIntrinsic(TrakkitIntrinsic.FI_CURRENT)
        fun current(): FunctionInfo = throw TrakkitPluginNotAppliedException()

        @TrakkitIntrinsic(TrakkitIntrinsic.FI_OF)
        fun of(function: KFunction<*>): FunctionInfo = throw TrakkitPluginNotAppliedException()

        private fun getCacheKey(
            qualifiedName: String, returnType: KClass<*>, parameterTypes: List<KClass<*>>
        ): Int {
            var result = qualifiedName.hashCode()
            result = 31 * result + returnType.hashCode()
            result = 31 * result + parameterTypes.hashCode()
            return result
        }

        @TrakkitCompilerApi
        internal fun getOrCreate(
            location: SourceLocation,
            qualifiedName: String,
            name: String,
            typeParameterNames: List<String>,
            returnType: KClass<*>,
            parameterTypes: List<KClass<*>>,
            parameterNames: List<String>,
            annotations: Map<KClass<out Annotation>, List<AnnotationUsageInfo>>
        ): FunctionInfo {
            return cache.getOrPut(getCacheKey(qualifiedName, returnType, parameterTypes)) {
                FunctionInfo(
                    location,
                    qualifiedName,
                    name,
                    typeParameterNames,
                    returnType,
                    parameterTypes,
                    parameterNames,
                    annotations
                )
            }
        }
    }

    fun hasAnnotation(type: KClass<out Annotation>): Boolean = type in annotations
    inline fun <reified A : Annotation> hasAnnotation(): Boolean = hasAnnotation(A::class)

    fun getAnnotation(type: KClass<out Annotation>): AnnotationUsageInfo = annotations[type]!!.first()
    inline fun <reified A : Annotation> getAnnotation(): AnnotationUsageInfo = getAnnotation(A::class)

    fun getAnnotations(type: KClass<out Annotation>): List<AnnotationUsageInfo> = annotations[type] ?: emptyList()
    inline fun <reified A : Annotation> getAnnotations(): List<AnnotationUsageInfo> = getAnnotations(A::class)

    fun getAnnotationOrNull(type: KClass<out Annotation>): AnnotationUsageInfo? = annotations[type]?.firstOrNull()
    inline fun <reified A : Annotation> getAnnotationOrNull(): AnnotationUsageInfo? = getAnnotationOrNull(A::class)

    fun toFormattedString(indent: Int = 0): String {
        // Annotations
        var result = if (annotations.isEmpty()) ""
        else "${annotations.values.flatten().joinToString("\n") { it.toFormattedString(indent) }}\n"
        // Parameters
        val parameters = if (parameterNames.isEmpty()) ""
        else parameterNames.mapIndexed { index, name ->
            "$name: ${parameterTypes[index].getQualifiedName()}"
        }.joinToString(", ")
        // Function
        result += "${"\t".repeat(indent)}fun "
        if (typeParameterNames.isNotEmpty()) {
            result += "<${typeParameterNames.joinToString(", ")}> "
        }
        val escapedName = if (' ' in name) "`$name`" else name
        result += "$escapedName($parameters): ${returnType.getQualifiedName()}"
        return result
    }
}