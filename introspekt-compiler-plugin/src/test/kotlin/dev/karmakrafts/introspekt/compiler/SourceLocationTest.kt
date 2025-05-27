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

import dev.karmakrafts.introspekt.compiler.util.SourceLocation
import dev.karmakrafts.introspekt.compiler.util.getFunctionLocation
import dev.karmakrafts.introspekt.compiler.util.getLocation
import dev.karmakrafts.iridium.pipeline.defaultPipelineSpec
import dev.karmakrafts.iridium.runCompilerTest
import dev.karmakrafts.iridium.util.getChild
import io.kotest.matchers.shouldBe
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import kotlin.test.Test

class SourceLocationTest {
    @Test
    fun `Get function location`() = runCompilerTest {
        val moduleName = "testing"
        fileName = "test.kt"
        pipeline {
            defaultPipelineSpec(moduleName)
        }
        // @formatter:off
        source("""
            class Foo {
                fun bar() { 
                    println("Hello, World!") 
                }       
            }
        """.trimIndent())
        // @formatter:on
        compiler shouldNotReport { error() }
        result irMatches {
            element.getChild<IrFunction> { it.name.asString() == "bar" }
                .getFunctionLocation(element, element.files.first(), this@runCompilerTest.sourceLines)
                .shouldBe(SourceLocation(moduleName, this@runCompilerTest.fileName, 2, 5))
        }
    }

    @Test
    fun `Get property location`() = runCompilerTest {
        val moduleName = "testing"
        fileName = "test.kt"
        pipeline {
            defaultPipelineSpec(moduleName)
        }
        // @formatter:off
        source("""
            class Foo {
                val bar: String = "Hello, World!"
            }
        """.trimIndent())
        // @formatter:on
        compiler shouldNotReport { error() }
        result irMatches {
            element.getChild<IrProperty> { it.name.asString() == "bar" }
                .getLocation(element, element.files.first(), this@runCompilerTest.sourceLines)
                .shouldBe(SourceLocation(moduleName, this@runCompilerTest.fileName, 2, 5))
        }
    }

    @Test
    fun `Get class location`() = runCompilerTest {
        val moduleName = "testing"
        fileName = "test.kt"
        pipeline {
            defaultPipelineSpec(moduleName)
        }
        // @formatter:off
        source("""
            class Foo {
                class Bar
            }
        """.trimIndent())
        // @formatter:on
        compiler shouldNotReport { error() }
        result irMatches {
            element.getChild<IrClass> { it.name.asString() == "Bar" }
                .getLocation(element, element.files.first(), this@runCompilerTest.sourceLines)
                .shouldBe(SourceLocation(moduleName, this@runCompilerTest.fileName, 2, 5))
        }
    }
}