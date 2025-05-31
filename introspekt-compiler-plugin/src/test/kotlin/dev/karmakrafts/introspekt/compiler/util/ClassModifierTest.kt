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

package dev.karmakrafts.introspekt.compiler.util

import dev.karmakrafts.introspekt.compiler.introspektPipeline
import dev.karmakrafts.iridium.runCompilerTest
import org.jetbrains.kotlin.ir.declarations.IrClass
import kotlin.test.Test

class ClassModifierTest {
    @Test
    fun `Get regular class modifier`() = runCompilerTest {
        introspektPipeline()
        // @formatter:off
        source("""
            class Foo
        """.trimIndent())
        // @formatter:on
        compiler shouldNotReport { error() }
        result irMatches {
            getChild<IrClass> { it.name.asString() == "Foo" }.apply {
                getClassModifier() shouldBe null
            }
        }
    }

    @Test
    fun `Get enum class modifier`() = runCompilerTest {
        introspektPipeline()
        // @formatter:off
        source("""
            enum class Foo
        """.trimIndent())
        // @formatter:on
        compiler shouldNotReport { error() }
        result irMatches {
            getChild<IrClass> { it.name.asString() == "Foo" }.apply {
                getClassModifier() shouldBe ClassModifier.ENUM
            }
        }
    }

    @Test
    fun `Get data class modifier`() = runCompilerTest {
        introspektPipeline()
        // @formatter:off
        source("""
            data class Foo(val x: Int)
        """.trimIndent())
        // @formatter:on
        compiler shouldNotReport { error() }
        result irMatches {
            getChild<IrClass> { it.name.asString() == "Foo" }.apply {
                getClassModifier() shouldBe ClassModifier.DATA
            }
        }
    }

    @Test
    fun `Get value class modifier`() = runCompilerTest {
        introspektPipeline()
        // @formatter:off
        source("""
            @JvmInline
            value class Foo(val x: Int)
        """.trimIndent())
        // @formatter:on
        compiler shouldNotReport { error() }
        result irMatches {
            getChild<IrClass> { it.name.asString() == "Foo" }.apply {
                getClassModifier() shouldBe ClassModifier.VALUE
            }
        }
    }
}