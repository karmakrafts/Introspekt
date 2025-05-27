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

package dev.karmakrafts.introspekt.compiler

import dev.karmakrafts.introspekt.compiler.util.getVisibilityName
import io.kotest.matchers.shouldBe
import org.jetbrains.kotlin.descriptors.Visibilities
import kotlin.test.Test

class IrUtilsTest {
    @Test
    fun `Get visibility name`() {
        Visibilities.Public.getVisibilityName() shouldBe "PUBLIC"
        Visibilities.Protected.getVisibilityName() shouldBe "PROTECTED"
        Visibilities.Private.getVisibilityName() shouldBe "PRIVATE"
        Visibilities.Internal.getVisibilityName() shouldBe "INTERNAL"
        Visibilities.Unknown.getVisibilityName() shouldBe "PRIVATE"
    }
}