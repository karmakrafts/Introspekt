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

sealed interface DecoratedElementInfo : ElementInfo {
    val annotations: Map<KClass<out Annotation>, List<AnnotationUsageInfo>>

    fun hasAnnotation(type: KClass<out Annotation>): Boolean = type in annotations
    fun getAnnotation(type: KClass<out Annotation>): AnnotationUsageInfo = annotations[type]!!.first()
    fun getAnnotations(type: KClass<out Annotation>): List<AnnotationUsageInfo> = annotations[type] ?: emptyList()
    fun getAnnotationOrNull(type: KClass<out Annotation>): AnnotationUsageInfo? = annotations[type]?.firstOrNull()
}

inline fun <reified A : Annotation> DecoratedElementInfo.hasAnnotation(): Boolean = hasAnnotation(A::class)
inline fun <reified A : Annotation> DecoratedElementInfo.getAnnotation(): AnnotationUsageInfo = getAnnotation(A::class)
inline fun <reified A : Annotation> DecoratedElementInfo.getAnnotations(): List<AnnotationUsageInfo> = getAnnotations(A::class)
inline fun <reified A : Annotation> DecoratedElementInfo.getAnnotationOrNull(): AnnotationUsageInfo? = getAnnotationOrNull(A::class)