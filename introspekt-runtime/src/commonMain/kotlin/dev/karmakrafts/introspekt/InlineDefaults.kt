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

@GeneratedIntrospektApi
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CONSTRUCTOR)
annotation class InlineDefaults(vararg val modes: Mode) {
    enum class Mode {
        // @formatter:off
        NONE,
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