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

/**
 * Annotation used to mark functions as intrinsics for the Introspekt compiler plugin.
 * 
 * Introspekt intrinsics are special functions that are recognized by the Introspekt compiler plugin
 * and replaced with compile-time generated code that provides introspection capabilities.
 * These intrinsics allow access to source code information, class metadata, function details,
 * and type information at compile time.
 *
 * @property type The type of intrinsic operation to perform.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class IntrospektIntrinsic(val type: Type) {
    /**
     * Enumeration of supported intrinsic types.
     * 
     * The enum values are grouped by prefix:
     * - SL_*: Source Location intrinsics for getting file, line, and position information
     * - FI_*: Function Information intrinsics for accessing function metadata
     * - CI_*: Class Information intrinsics for accessing class metadata
     * - TI_*: Type Information intrinsics for accessing type metadata
     */
    enum class Type {
        // @formatter:off
        SL_HERE,
        SL_HERE_HASH,
        SL_CURRENT_FUNCTION,
        SL_CURRENT_FUNCTION_HASH,
        SL_CURRENT_CLASS,
        SL_CURRENT_CLASS_HASH,
        SL_OF_CLASS,
        SL_OF_FUNCTION,
        FI_CURRENT,
        FI_OF,
        CI_CURRENT,
        CI_OF,
        TI_OF;
        // @formatter:on
    }
}
