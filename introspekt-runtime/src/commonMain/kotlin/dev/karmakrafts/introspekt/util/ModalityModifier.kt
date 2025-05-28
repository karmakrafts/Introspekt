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

package dev.karmakrafts.introspekt.util

/**
 * Represents modality modifiers available in Kotlin.
 */
enum class ModalityModifier {
    // @formatter:off
    ABSTRACT,
    OPEN,
    SEALED,
    FINAL;
    // @formatter:on

    /**
     * Returns the lowercase string representation of this modality modifier.
     *
     * @return The name of the modality modifier in lowercase.
     */
    override fun toString(): String = name.lowercase()
}
