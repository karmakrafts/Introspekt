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

data class ParameterInfo(
    override val location: SourceLocation,
    override val qualifiedName: String,
    override val name: String,
    val type: KClass<*>,
    val annotations: Map<KClass<out Annotation>, List<AnnotationUsageInfo>>
) : ElementInfo {
    fun hasAnnotation(type: KClass<out Annotation>): Boolean = type in annotations
    inline fun <reified A : Annotation> hasAnnotation(): Boolean = hasAnnotation(A::class)

    fun getAnnotation(type: KClass<out Annotation>): AnnotationUsageInfo = annotations[type]!!.first()
    inline fun <reified A : Annotation> getAnnotation(): AnnotationUsageInfo = getAnnotation(A::class)

    fun getAnnotations(type: KClass<out Annotation>): List<AnnotationUsageInfo> = annotations[type] ?: emptyList()
    inline fun <reified A : Annotation> getAnnotations(): List<AnnotationUsageInfo> = getAnnotations(A::class)

    fun getAnnotationOrNull(type: KClass<out Annotation>): AnnotationUsageInfo? = annotations[type]?.firstOrNull()
    inline fun <reified A : Annotation> getAnnotationOrNull(): AnnotationUsageInfo? = getAnnotationOrNull(A::class)
}