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

import dev.karmakrafts.introspekt.compiler.introspektTransformerPipeline
import dev.karmakrafts.introspekt.compiler.isCachedSourceLocation
import dev.karmakrafts.iridium.matcher.IrElementMatcher
import dev.karmakrafts.iridium.runCompilerTest
import dev.karmakrafts.iridium.util.renderIrTree
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.util.target
import kotlin.test.Test

class SourceLocationTransformerTest {
    fun IrElementMatcher<IrModuleFragment>.checkInlinedIntrinsic() {
        val call = getChild<IrCall> { it.target.name.asString() == "foo" }
        val locationParam = call.target.parameters.find { it.kind == IrParameterKind.Regular }
        locationParam shouldNotBe null
        val locationArg = call.arguments[locationParam!!.indexInParameters]
        locationArg shouldNotBe null
        locationArg!!::class shouldBe IrCallImpl::class
        (locationArg as IrCallImpl) matches { isCachedSourceLocation("test", "test", 7, 5) }
        println(element.renderIrTree(Int.MAX_VALUE))
    }

    @Test
    fun `Obtain caller source location by parameter default`() = runCompilerTest {
        introspektTransformerPipeline()
        // @formatter:off
        source("""
            package com.example
            import dev.karmakrafts.introspekt.util.SourceLocation
            fun foo(location: SourceLocation = SourceLocation.here()) {
                println(location)
            }
            fun bar() {
                foo()
            }
        """.trimIndent())
        // @formatter:on
        compiler shouldNotReport { error() }
        result irMatches { checkInlinedIntrinsic() }
    }

    @Test
    fun `Obtain caller source location by inline parameter default`() = runCompilerTest {
        introspektTransformerPipeline()
        // @formatter:off
        source("""
            package com.example
            import dev.karmakrafts.introspekt.util.SourceLocation
            inline fun foo(location: SourceLocation = SourceLocation.here()) {
                println(location)
            }
            fun bar() {
                foo()
            }
        """.trimIndent())
        // @formatter:on
        compiler shouldNotReport { error() }
        result irMatches { checkInlinedIntrinsic() }
    }

    @Test
    fun `Obtain caller source location by inline parameter default with trailing closure`() = runCompilerTest {
        introspektTransformerPipeline()
        // @formatter:off
        source("""
            package com.example
            import dev.karmakrafts.introspekt.util.SourceLocation
            inline fun <reified T> foo(location: SourceLocation = SourceLocation.here(), i: Int = 20, closure: () -> T): T {
                return closure()
            }
            fun bar() {
                foo { println("HELLO, WORLD!") }
            }
        """.trimIndent())
        // @formatter:on
        compiler shouldNotReport { error() }
        result irMatches { checkInlinedIntrinsic() }
    }

    @Test
    fun `Obtain current source location`() = runCompilerTest {
        introspektTransformerPipeline()
        // @formatter:off
        source("""
            import dev.karmakrafts.introspekt.util.SourceLocation
            class Test {
                fun test() {
                    val location = SourceLocation.here()
                }
            }
        """.trimIndent())
        // @formatter:on
        compiler shouldNotReport { error() }
        result irMatches {
            getChild<IrCall> { it.type.classFqName == IntrospektNames.SourceLocation.fqName } matches {
                isCachedSourceLocation("test", "test", 4, 39)
            }
        }
    }

    @Test
    fun `Obtain current function location`() = runCompilerTest {
        introspektTransformerPipeline()
        // @formatter:off
        source("""
            import dev.karmakrafts.introspekt.util.SourceLocation
            class Test {
                fun test() {
                    val location = SourceLocation.currentFunction()
                }
            }
        """.trimIndent())
        // @formatter:on
        compiler shouldNotReport { error() }
        result irMatches {
            getChild<IrCall> { it.type.classFqName == IntrospektNames.SourceLocation.fqName } matches {
                isCachedSourceLocation("test", "test", 3, 5)
            }
        }
    }

    @Test
    fun `Obtain current class location`() = runCompilerTest {
        introspektTransformerPipeline()
        // @formatter:off
        source("""
            import dev.karmakrafts.introspekt.util.SourceLocation
            class Test {
                fun test() {
                    val location = SourceLocation.currentClass()
                }
            }
        """.trimIndent())
        // @formatter:on
        compiler shouldNotReport { error() }
        result irMatches {
            getChild<IrCall> { it.type.classFqName == IntrospektNames.SourceLocation.fqName } matches {
                isCachedSourceLocation("test", "test", 2, 1)
            }
        }
    }

    @Test
    fun `Obtain location via function reference`() = runCompilerTest {
        introspektTransformerPipeline()
        // @formatter:off
        source("""
            import dev.karmakrafts.introspekt.util.SourceLocation
            class Test {
                fun foo() {}
                fun test() {
                    val location = SourceLocation.ofFunction(::foo)
                }
            }
        """.trimIndent())
        // @formatter:on
        compiler shouldNotReport { error() }
        result irMatches {
            getChild<IrCall> { it.type.classFqName == IntrospektNames.SourceLocation.fqName } matches {
                isCachedSourceLocation("test", "test", 3, 5)
            }
        }
    }

    @Test
    fun `Obtain location via type parameter`() = runCompilerTest {
        introspektTransformerPipeline()
        // @formatter:off
        source("""
            import dev.karmakrafts.introspekt.util.SourceLocation
            class Test {
                class Foo
                fun test() {
                    val location = SourceLocation.ofClass<Foo>()
                }
            }
        """.trimIndent())
        // @formatter:on
        compiler shouldNotReport { error() }
        result irMatches {
            getChild<IrCall> { it.type.classFqName == IntrospektNames.SourceLocation.fqName } matches {
                isCachedSourceLocation("test", "test", 3, 5)
            }
        }
    }
}