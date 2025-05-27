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

import dev.karmakrafts.introspekt.compiler.util.ClassModifier
import dev.karmakrafts.introspekt.compiler.util.IntrospektIntrinsic
import dev.karmakrafts.introspekt.compiler.util.SourceLocation
import dev.karmakrafts.introspekt.compiler.util.TraceType
import dev.karmakrafts.introspekt.compiler.util.getClassModifier
import dev.karmakrafts.introspekt.compiler.util.getFunctionLocation
import dev.karmakrafts.introspekt.compiler.util.getIntrinsicType
import dev.karmakrafts.introspekt.compiler.util.getLocation
import dev.karmakrafts.introspekt.compiler.util.getTraceType
import dev.karmakrafts.introspekt.compiler.util.getVisibilityName
import dev.karmakrafts.iridium.pipeline.addJvmClasspathRootByType
import dev.karmakrafts.iridium.pipeline.defaultPipelineSpec
import dev.karmakrafts.iridium.runCompilerTest
import dev.karmakrafts.iridium.setupCompilerTest
import dev.karmakrafts.iridium.util.getChild
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import kotlin.test.Test
import dev.karmakrafts.introspekt.IntrospektIntrinsic as IntrospektIntrinsicAnnotation

class IrUtilsTest {
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

    @Test
    fun `Get regular class modifier`() = runCompilerTest {
        pipeline {
            defaultPipelineSpec()
        }
        // @formatter:off
        source("""
            class Foo
        """.trimIndent())
        // @formatter:on
        compiler shouldNotReport { error() }
        result irMatches {
            element.getChild<IrClass> { it.name.asString() == "Foo" }.apply {
                getClassModifier() shouldBe null
            }
        }
    }

    @Test
    fun `Get enum class modifier`() = runCompilerTest {
        pipeline {
            defaultPipelineSpec()
        }
        // @formatter:off
        source("""
            enum class Foo
        """.trimIndent())
        // @formatter:on
        compiler shouldNotReport { error() }
        result irMatches {
            element.getChild<IrClass> { it.name.asString() == "Foo" }.apply {
                getClassModifier() shouldBe ClassModifier.ENUM
            }
        }
    }

    @Test
    fun `Get data class modifier`() = runCompilerTest {
        pipeline {
            defaultPipelineSpec()
        }
        // @formatter:off
        source("""
            data class Foo(val x: Int)
        """.trimIndent())
        // @formatter:on
        compiler shouldNotReport { error() }
        result irMatches {
            element.getChild<IrClass> { it.name.asString() == "Foo" }.apply {
                getClassModifier() shouldBe ClassModifier.DATA
            }
        }
    }

    @Test
    fun `Get value class modifier`() = runCompilerTest {
        pipeline {
            defaultPipelineSpec()
        }
        // @formatter:off
        source("""
            @JvmInline
            value class Foo(val x: Int)
        """.trimIndent())
        // @formatter:on
        compiler shouldNotReport { error() }
        result irMatches {
            element.getChild<IrClass> { it.name.asString() == "Foo" }.apply {
                getClassModifier() shouldBe ClassModifier.VALUE
            }
        }
    }

    @Test
    fun `Get intrinsic type`() = setupCompilerTest {
        pipeline {
            defaultPipelineSpec()
            config {
                addJvmClasspathRootByType<IntrospektIntrinsicAnnotation>()
            }
        }
        for (intrinsicType in IntrospektIntrinsic.entries) {
            resetAssertions()
            compiler shouldNotReport { error() }
            // @formatter:off
            source("""
                import dev.karmakrafts.introspekt.IntrospektPluginNotAppliedException
                import dev.karmakrafts.introspekt.IntrospektIntrinsic
                @IntrospektIntrinsic(IntrospektIntrinsic.Type.${intrinsicType.name})
                fun intrinsic(): Nothing = throw IntrospektPluginNotAppliedException()
            """.trimIndent())
            // @formatter:on
            result irMatches {
                element.getChild<IrFunction> { it.name.asString() == "intrinsic" }
                    .getIntrinsicType() shouldBe intrinsicType
            }
            evaluate()
        }
    }

    @Test
    fun `Get trace type`() = setupCompilerTest {
        pipeline {
            defaultPipelineSpec()
            config {
                addJvmClasspathRootByType<IntrospektIntrinsicAnnotation>()
            }
        }
        for (traceType in TraceType.entries) {
            resetAssertions()
            compiler shouldNotReport { error() }
            // @formatter:off
            source("""
                import dev.karmakrafts.introspekt.Trace
                @Trace(Trace.Target.${traceType.name})
                fun test() {
                    println("Hello, World!")
                }
            """.trimIndent())
            // @formatter:on
            result irMatches {
                element.getChild<IrFunction> { it.name.asString() == "test" }.getTraceType() shouldContain traceType
            }
            evaluate()
        }
    }

    @Test
    fun `Get visibility name`() {
        Visibilities.Public.getVisibilityName() shouldBe "PUBLIC"
        Visibilities.Protected.getVisibilityName() shouldBe "PROTECTED"
        Visibilities.Private.getVisibilityName() shouldBe "PRIVATE"
        Visibilities.Internal.getVisibilityName() shouldBe "INTERNAL"
        Visibilities.Unknown.getVisibilityName() shouldBe "PRIVATE"
    }
}