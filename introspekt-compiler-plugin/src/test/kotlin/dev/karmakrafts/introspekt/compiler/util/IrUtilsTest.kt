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
import dev.karmakrafts.iridium.setupCompilerTest
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.name.FqName
import kotlin.test.Test

class IrUtilsTest {
    @Test
    fun `Get visibility name`() = setupCompilerTest {
        Visibilities.Public.getVisibilityName() shouldBe "PUBLIC"
        Visibilities.Protected.getVisibilityName() shouldBe "PROTECTED"
        Visibilities.Private.getVisibilityName() shouldBe "PRIVATE"
        Visibilities.Internal.getVisibilityName() shouldBe "INTERNAL"
        Visibilities.Unknown.getVisibilityName() shouldBe "PRIVATE"
    }

    @Test
    fun `Compute line and column from source range`() = setupCompilerTest {
        introspektPipeline()
        val sourceBuffer = StringBuilder()
        for (x in 0..<10) for (y in 0..<10) {
            resetAssertions()
            sourceBuffer.clear()
            sourceBuffer.append("\n".repeat(y))
            sourceBuffer.append(" ".repeat(x))
            sourceBuffer.append("class Test")
            source(sourceBuffer.toString())
            compiler shouldNotReport { error() }
            result irMatches {
                val clazz = getChild<IrClass> { it.name.asString() == "Test" }
                val source = this@setupCompilerTest.sourceLines
                getLineNumber(source, clazz.startOffset, clazz.endOffset) shouldBe (y + 1)
                getColumnNumber(source, clazz.startOffset, clazz.endOffset) shouldBe (x + 1)
            }
            evaluate()
        }
    }

    @Test
    fun `Parse enum annotation value`() = runCompilerTest {
        introspektPipeline()
        // @formatter:off
        source("""
            package com.example
            enum class FooType { FOO1, FOO2 }
            @Retention(AnnotationRetention.BINARY)
            @Target(AnnotationTarget.CLASS)
            annotation class Foo(val value: FooType)
            @Foo(FooType.FOO1)
            class Bar
        """.trimIndent())
        // @formatter:on
        compiler shouldNotReport { error() }
        result irMatches {
            val annotation = getChild<IrClass> { it.name.asString() == "Bar" }.getAnnotation(FqName("com.example.Foo"))
            annotation shouldNotBe null
            val values = annotation!!.getAnnotationValues()
            values.size shouldBe 1
            values["value"] shouldBe "FOO1" // Enum instances are parsed as the names of their constants
        }
    }

    @Test
    fun `Parse String annotation value`() = runCompilerTest {
        introspektPipeline()
        // @formatter:off
        source("""
            package com.example
            @Retention(AnnotationRetention.BINARY)
            @Target(AnnotationTarget.CLASS)
            annotation class Foo(val value: String)
            @Foo("Hello, World!")
            class Bar
        """.trimIndent())
        // @formatter:on
        compiler shouldNotReport { error() }
        result irMatches {
            val annotation = getChild<IrClass> { it.name.asString() == "Bar" }.getAnnotation(FqName("com.example.Foo"))
            annotation shouldNotBe null
            val values = annotation!!.getAnnotationValues()
            values.size shouldBe 1
            values["value"] shouldBe "Hello, World!"
        }
    }

    @Test
    fun `Parse Int annotation value`() = runCompilerTest {
        introspektPipeline()
        // @formatter:off
        source("""
            package com.example
            @Retention(AnnotationRetention.BINARY)
            @Target(AnnotationTarget.CLASS)
            annotation class Foo(val value: Int)
            @Foo(420)
            class Bar
        """.trimIndent())
        // @formatter:on
        compiler shouldNotReport { error() }
        result irMatches {
            val annotation = getChild<IrClass> { it.name.asString() == "Bar" }.getAnnotation(FqName("com.example.Foo"))
            annotation shouldNotBe null
            val values = annotation!!.getAnnotationValues()
            values.size shouldBe 1
            values["value"] shouldBe 420
        }
    }

    @Test
    fun `Parse Float annotation value`() = runCompilerTest {
        introspektPipeline()
        // @formatter:off
        source("""
            package com.example
            @Retention(AnnotationRetention.BINARY)
            @Target(AnnotationTarget.CLASS)
            annotation class Foo(val value: Float)
            @Foo(4.20F)
            class Bar
        """.trimIndent())
        // @formatter:on
        compiler shouldNotReport { error() }
        result irMatches {
            val annotation = getChild<IrClass> { it.name.asString() == "Bar" }.getAnnotation(FqName("com.example.Foo"))
            annotation shouldNotBe null
            val values = annotation!!.getAnnotationValues()
            values.size shouldBe 1
            values["value"] shouldBe 4.20F
        }
    }

    @Test
    fun `Parse Boolean annotation value`() = runCompilerTest {
        introspektPipeline()
        // @formatter:off
        source("""
            package com.example
            @Retention(AnnotationRetention.BINARY)
            @Target(AnnotationTarget.CLASS)
            annotation class Foo(val value: Boolean)
            @Foo(true)
            class Bar
        """.trimIndent())
        // @formatter:on
        compiler shouldNotReport { error() }
        result irMatches {
            val annotation = getChild<IrClass> { it.name.asString() == "Bar" }.getAnnotation(FqName("com.example.Foo"))
            annotation shouldNotBe null
            val values = annotation!!.getAnnotationValues()
            values.size shouldBe 1
            values["value"] shouldBe true
        }
    }

    @Test
    fun `Parse type annotation value`() = runCompilerTest {
        introspektPipeline()
        // @formatter:off
        source("""
            package com.example
            import kotlin.reflect.KClass
            @Retention(AnnotationRetention.BINARY)
            @Target(AnnotationTarget.CLASS)
            annotation class Foo(val value: KClass<*>)
            @Foo(String::class)
            class Bar
        """.trimIndent())
        // @formatter:on
        compiler shouldNotReport { error() }
        result irMatches {
            val annotation = getChild<IrClass> { it.name.asString() == "Bar" }.getAnnotation(FqName("com.example.Foo"))
            annotation shouldNotBe null
            val values = annotation!!.getAnnotationValues()
            values.size shouldBe 1
            val value = values["value"]
            value shouldNotBe null
            value!!::class shouldBe IrSimpleTypeImpl::class
            (value as IrSimpleTypeImpl).classFqName shouldBe FqName("kotlin.String")
        }
    }

    @Test
    fun `Parse enum array annotation value`() = runCompilerTest {
        introspektPipeline()
        // @formatter:off
        source("""
            package com.example
            enum class FooType { FOO1, FOO2, FOO3 }
            @Retention(AnnotationRetention.BINARY)
            @Target(AnnotationTarget.CLASS)
            annotation class Foo(vararg val values: FooType)
            @Foo(FooType.FOO1, FooType.FOO2)
            class Bar
        """.trimIndent())
        // @formatter:on
        compiler shouldNotReport { error() }
        result irMatches {
            val annotation = getChild<IrClass> { it.name.asString() == "Bar" }.getAnnotation(FqName("com.example.Foo"))
            annotation shouldNotBe null
            val values = annotation!!.getAnnotationValues()
            values.size shouldBe 1
            (values["values"] as List<*>) shouldBe listOf("FOO1", "FOO2")
        }
    }

    @Test
    fun `Parse String array annotation value`() = runCompilerTest {
        introspektPipeline()
        // @formatter:off
        source("""
            package com.example
            @Retention(AnnotationRetention.BINARY)
            @Target(AnnotationTarget.CLASS)
            annotation class Foo(vararg val values: String)
            @Foo("Hello", "World!")
            class Bar
        """.trimIndent())
        // @formatter:on
        compiler shouldNotReport { error() }
        result irMatches {
            val annotation = getChild<IrClass> { it.name.asString() == "Bar" }.getAnnotation(FqName("com.example.Foo"))
            annotation shouldNotBe null
            val values = annotation!!.getAnnotationValues()
            values.size shouldBe 1
            (values["values"] as List<*>) shouldBe listOf("Hello", "World!")
        }
    }

    @Test
    fun `Parse Int array annotation value`() = runCompilerTest {
        introspektPipeline()
        // @formatter:off
        source("""
            package com.example
            @Retention(AnnotationRetention.BINARY)
            @Target(AnnotationTarget.CLASS)
            annotation class Foo(vararg val values: Int)
            @Foo(4, 20)
            class Bar
        """.trimIndent())
        // @formatter:on
        compiler shouldNotReport { error() }
        result irMatches {
            val annotation = getChild<IrClass> { it.name.asString() == "Bar" }.getAnnotation(FqName("com.example.Foo"))
            annotation shouldNotBe null
            val values = annotation!!.getAnnotationValues()
            values.size shouldBe 1
            (values["values"] as List<*>) shouldBe listOf(4, 20)
        }
    }

    @Test
    fun `Parse Float array annotation value`() = runCompilerTest {
        introspektPipeline()
        // @formatter:off
        source("""
            package com.example
            @Retention(AnnotationRetention.BINARY)
            @Target(AnnotationTarget.CLASS)
            annotation class Foo(vararg val values: Float)
            @Foo(4F, 20F)
            class Bar
        """.trimIndent())
        // @formatter:on
        compiler shouldNotReport { error() }
        result irMatches {
            val annotation = getChild<IrClass> { it.name.asString() == "Bar" }.getAnnotation(FqName("com.example.Foo"))
            annotation shouldNotBe null
            val values = annotation!!.getAnnotationValues()
            values.size shouldBe 1
            (values["values"] as List<*>) shouldBe listOf(4F, 20F)
        }
    }

    @Test
    fun `Parse Boolean array annotation value`() = runCompilerTest {
        introspektPipeline()
        // @formatter:off
        source("""
            package com.example
            @Retention(AnnotationRetention.BINARY)
            @Target(AnnotationTarget.CLASS)
            annotation class Foo(vararg val values: Boolean)
            @Foo(true, false)
            class Bar
        """.trimIndent())
        // @formatter:on
        compiler shouldNotReport { error() }
        result irMatches {
            val annotation = getChild<IrClass> { it.name.asString() == "Bar" }.getAnnotation(FqName("com.example.Foo"))
            annotation shouldNotBe null
            val values = annotation!!.getAnnotationValues()
            values.size shouldBe 1
            (values["values"] as List<*>) shouldBe listOf(true, false)
        }
    }

    @Test
    fun `Parse type array annotation value`() = runCompilerTest {
        introspektPipeline()
        // @formatter:off
        source("""
            package com.example
            import kotlin.reflect.KClass
            @Retention(AnnotationRetention.BINARY)
            @Target(AnnotationTarget.CLASS)
            annotation class Foo(vararg val values: KClass<*>)
            @Foo(String::class, Int::class)
            class Bar
        """.trimIndent())
        // @formatter:on
        compiler shouldNotReport { error() }
        result irMatches {
            val annotation = getChild<IrClass> { it.name.asString() == "Bar" }.getAnnotation(FqName("com.example.Foo"))
            annotation shouldNotBe null
            val values = annotation!!.getAnnotationValues()
            values.size shouldBe 1
            val value = values["values"] as? List<*>
            value shouldNotBe null

            val (arg1, arg2) = value!!

            arg1!!::class shouldBe IrSimpleTypeImpl::class
            (arg1 as IrSimpleTypeImpl) matches { string() }

            arg2!!::class shouldBe IrSimpleTypeImpl::class
            (arg2 as IrSimpleTypeImpl) matches { int() }
        }
    }
}