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

import kotlin.reflect.KClass

/**
 * Represents an element that can be annotated with Kotlin annotations.
 *
 * This interface extends [ElementInfo] to provide functionality for accessing and querying
 * annotations attached to code elements such as classes, functions, properties, etc.
 * It allows for type-safe access to annotations through reflection.
 */
sealed interface AnnotatedElementInfo : ElementInfo {
    /**
     * A map of annotations applied to this element.
     *
     * The keys are [TypeInfo] objects representing the annotation types, and the values
     * are lists of [AnnotationUsageInfo] objects containing the annotation instances
     * and their parameter values.
     */
    val annotations: Map<TypeInfo, List<AnnotationUsageInfo>>

    /**
     * Checks if this element has an annotation of the specified type.
     *
     * @param type The Kotlin class of the annotation to check for
     * @return `true` if this element has at least one annotation of the specified type, `false` otherwise
     */
    fun hasAnnotation(type: KClass<out Annotation>): Boolean = annotations.keys.any { it.reflectType == type }

    /**
     * Gets all annotations of the specified type applied to this element.
     *
     * @param type The Kotlin class of the annotation to retrieve
     * @return A list of [AnnotationUsageInfo] objects for annotations of the specified type,
     *         or an empty list if no such annotations exist
     */
    fun getAnnotations(type: KClass<out Annotation>): List<AnnotationUsageInfo> =
        annotations.keys.find { it.reflectType == type }?.let(annotations::get) ?: emptyList()

    /**
     * Gets the first annotation of the specified type applied to this element.
     *
     * @param type The Kotlin class of the annotation to retrieve
     * @return The first [AnnotationUsageInfo] for an annotation of the specified type
     * @throws NoSuchElementException if no annotation of the specified type exists
     */
    fun getAnnotation(type: KClass<out Annotation>): AnnotationUsageInfo = getAnnotations(type).first()

    /**
     * Gets the first annotation of the specified type applied to this element, or null if none exists.
     *
     * @param type The Kotlin class of the annotation to retrieve
     * @return The first [AnnotationUsageInfo] for an annotation of the specified type,
     *         or `null` if no such annotation exists
     */
    fun getAnnotationOrNull(type: KClass<out Annotation>): AnnotationUsageInfo? = getAnnotations(type).firstOrNull()
}

/**
 * Checks if this element has an annotation of the specified reified type.
 *
 * This is a convenience extension function that uses reified generics to avoid explicitly
 * passing the annotation class.
 *
 * @param A The annotation type to check for
 * @return `true` if this element has at least one annotation of the specified type, `false` otherwise
 */
inline fun <reified A : Annotation> AnnotatedElementInfo.hasAnnotation(): Boolean = hasAnnotation(A::class)

/**
 * Gets the first annotation of the specified reified type applied to this element.
 *
 * This is a convenience extension function that uses reified generics to avoid explicitly
 * passing the annotation class.
 *
 * @param A The annotation type to retrieve
 * @return The first [AnnotationUsageInfo] for an annotation of the specified type
 * @throws NoSuchElementException if no annotation of the specified type exists
 */
inline fun <reified A : Annotation> AnnotatedElementInfo.getAnnotation(): AnnotationUsageInfo = getAnnotation(A::class)

/**
 * Gets all annotations of the specified reified type applied to this element.
 *
 * This is a convenience extension function that uses reified generics to avoid explicitly
 * passing the annotation class.
 *
 * @param A The annotation type to retrieve
 * @return A list of [AnnotationUsageInfo] objects for annotations of the specified type,
 *         or an empty list if no such annotations exist
 */
inline fun <reified A : Annotation> AnnotatedElementInfo.getAnnotations(): List<AnnotationUsageInfo> =
    getAnnotations(A::class)

/**
 * Gets the first annotation of the specified reified type applied to this element, or null if none exists.
 *
 * This is a convenience extension function that uses reified generics to avoid explicitly
 * passing the annotation class.
 *
 * @param A The annotation type to retrieve
 * @return The first [AnnotationUsageInfo] for an annotation of the specified type,
 *         or `null` if no such annotation exists
 */
inline fun <reified A : Annotation> AnnotatedElementInfo.getAnnotationOrNull(): AnnotationUsageInfo? =
    getAnnotationOrNull(A::class)
