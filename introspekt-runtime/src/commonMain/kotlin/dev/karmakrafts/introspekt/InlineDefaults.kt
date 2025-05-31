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
 * Annotation used to control how default parameter values are inlined with respect to
 * introspection capabilities provided by Introspekt.
 *
 * When applied to a function or constructor, this annotation specifies which introspection
 * information should be inlined into the default parameter values at compile time.
 *
 * @property modes The introspection modes to be applied for inlining default values.
 */
@GeneratedIntrospektApi
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CONSTRUCTOR)
annotation class InlineDefaults(vararg val modes: Mode) {
    /**
     * Enumeration of supported inlining modes for default parameter values.
     *
     * The enum values are grouped by prefix:
     * - NONE: No introspection information is inlined
     * - SL_*: Source Location introspection modes for getting file, line, and position information
     * - FI_*: Function Information introspection modes for accessing function metadata
     * - CI_*: Class Information introspection modes for accessing class metadata
     */
    enum class Mode {
        // @formatter:off
        NONE,
        SL_HERE,
        SL_HERE_HASH,
        SL_CURRENT_FUNCTION,
        SL_CURRENT_FUNCTION_HASH,
        SL_CURRENT_CLASS,
        SL_CURRENT_CLASS_HASH,
        FI_CURRENT,
        CI_CURRENT;
        // @formatter:on
    }
}
