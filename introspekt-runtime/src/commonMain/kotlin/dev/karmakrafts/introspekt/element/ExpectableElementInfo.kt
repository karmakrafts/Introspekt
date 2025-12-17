/*
 * Copyright 2025 Karma Krafts
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

/**
 * Represents an element that can be declared with the `expect` keyword in Kotlin multiplatform code.
 *
 * This interface extends [ElementInfo] to provide information about whether a declaration
 * is an expect declaration that requires corresponding actual implementations in platform-specific code.
 * Expect declarations are used in Kotlin multiplatform projects to define a common API that
 * will have platform-specific implementations.
 */
sealed interface ExpectableElementInfo : ElementInfo {
    /**
     * Indicates whether this element is declared with the `expect` keyword.
     *
     * When `true`, this element is an expect declaration that requires corresponding actual
     * implementations in platform-specific code. When `false`, this element is either a regular
     * declaration or an actual implementation.
     */
    val isExpect: Boolean
}
