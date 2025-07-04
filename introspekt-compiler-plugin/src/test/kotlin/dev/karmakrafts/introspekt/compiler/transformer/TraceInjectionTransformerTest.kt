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

package dev.karmakrafts.introspekt.compiler.transformer

import dev.karmakrafts.introspekt.compiler.introspektTransformerPipeline
import dev.karmakrafts.introspekt.compiler.util.findChildren
import dev.karmakrafts.iridium.runCompilerTest
import dev.karmakrafts.iridium.util.renderIrTree
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.util.target
import kotlin.test.Test

class TraceInjectionTransformerTest {
    @Test
    fun `Inject call callback into suspend operator in companion object`() = runCompilerTest {
        introspektTransformerPipeline()
        // @formatter:off
        source("""
            import dev.karmakrafts.introspekt.trace.Trace
            class Test
            object Receiver
            fun <R> foo(i: Int = 0, block: Receiver.() -> R): R = Receiver.block()
            class MyClass {
                companion object {
                    @Trace(Trace.Target.AFTER_CALL)
                    suspend operator fun Test.plus(other: Test): Test {
                        println("HELLO, WORLD!")
                        foo { println("HELLO, WORLD!") }
                        return other
                    }
                }
            }
        """.trimIndent())
        // @formatter:on
        compiler shouldNotReport { error() }
        result irMatches {
            getChild<IrClass> { it.name.asString() == "MyClass" } matches {
                getChild<IrClass> { it.name.asString() == "Companion" } matches {
                    println(element.renderIrTree(Int.MAX_VALUE))
                    containsChild<IrCall> { call ->
                        call.target.name.asString() == "afterCall"
                    }
                }
            }
        }
    }

    @Test
    fun `Inject call callback into suspend operator`() = runCompilerTest {
        introspektTransformerPipeline()
        // @formatter:off
        source("""
            import dev.karmakrafts.introspekt.trace.Trace
            class Test
            object Receiver
            fun <R> foo(i: Int = 0, block: Receiver.() -> R): R = Receiver.block()
            @Trace(Trace.Target.AFTER_CALL)
            suspend operator fun Test.plus(other: Test): Test {
                println("HELLO, WORLD!")
                foo { println("HELLO, WORLD!") }
                return other
            }
        """.trimIndent())
        // @formatter:on
        compiler shouldNotReport { error() }
        result irMatches {
            containsChild<IrCall> { call ->
                call.target.name.asString() == "afterCall"
            }
        }
    }

    @Test
    fun `Inject call callback into suspend function`() = runCompilerTest {
        introspektTransformerPipeline()
        // @formatter:off
        source("""
            import dev.karmakrafts.introspekt.trace.Trace
            object Receiver
            fun <R> foo(i: Int = 0, block: Receiver.() -> R): R = Receiver.block()
            @Trace(Trace.Target.AFTER_CALL)
            suspend fun test() {
                println("HELLO, WORLD!")
                foo { println("HELLO, WORLD!") }
            }
        """.trimIndent())
        // @formatter:on
        compiler shouldNotReport { error() }
        result irMatches {
            containsChild<IrCall> { call ->
                call.target.name.asString() == "afterCall"
            }
        }
    }

    @Test
    fun `Inject call callback`() = runCompilerTest {
        introspektTransformerPipeline()
        // @formatter:off
        source("""
            import dev.karmakrafts.introspekt.trace.Trace
            object Receiver
            fun <R> foo(i: Int = 0, block: Receiver.() -> R): R = Receiver.block()
            @Trace(Trace.Target.AFTER_CALL)
            fun test() {
                println("HELLO, WORLD!")
                foo { println("HELLO, WORLD!") }
            }
        """.trimIndent())
        // @formatter:on
        compiler shouldNotReport { error() }
        result irMatches {
            containsChild<IrCall> { call ->
                call.target.name.asString() == "afterCall"
            }
        }
    }

    @Test
    fun `Inject all callbacks`() = runCompilerTest {
        introspektTransformerPipeline()
        // @formatter:off
        source("""
            import dev.karmakrafts.introspekt.trace.Trace
            object Receiver
            fun <R> foo(i: Int = 0, block: Receiver.() -> R): R = Receiver.block()
            @Trace
            fun test() {
                println("HELLO, WORLD!")
                foo { println("HELLO, WORLD!") }
            }
        """.trimIndent())
        // @formatter:on
        compiler shouldNotReport { error() }
        result irMatches {
            containsChild<IrCall> { it.target.name.asString() == "afterCall" }
            containsChild<IrCall> { it.target.name.asString() == "beforeCall" }
        }
    }

    @Test
    fun `Inject multiple call callbacks into suspend function`() = runCompilerTest {
        introspektTransformerPipeline()
        // @formatter:off
        source("""
            import dev.karmakrafts.introspekt.trace.Trace
            @Trace(Trace.Target.AFTER_CALL)
            suspend fun test() {
                println("HELLO")
                println("WORLD")
                println("THIS")
                println("IS")
                println("🦊")
            }
        """.trimIndent())
        // @formatter:on
        compiler shouldNotReport { error() }
        result irMatches {
            getChild<IrFunction> { it.name.asString() == "test" } matches {
                val body = element.body
                body shouldNotBe null
                body!!::class shouldBe IrBlockBodyImpl::class
                val blockBody = body as IrBlockBodyImpl
                blockBody.findChildren<IrCall> { it.target.name.asString() == "afterCall" }.size shouldBe 5
            }
        }
    }

    @Test
    fun `Inject multiple call callbacks`() = runCompilerTest {
        introspektTransformerPipeline()
        // @formatter:off
        source("""
            import dev.karmakrafts.introspekt.trace.Trace
            @Trace(Trace.Target.AFTER_CALL)
            fun test() {
                println("HELLO")
                println("WORLD")
                println("THIS")
                println("IS")
                println("🦊")
            }
        """.trimIndent())
        // @formatter:on
        compiler shouldNotReport { error() }
        result irMatches {
            getChild<IrFunction> { it.name.asString() == "test" } matches {
                val body = element.body
                body shouldNotBe null
                body!!::class shouldBe IrBlockBodyImpl::class
                val blockBody = body as IrBlockBodyImpl
                blockBody.findChildren<IrCall> { it.target.name.asString() == "afterCall" }.size shouldBe 5
            }
        }
    }

    @Test
    fun `Inject enter function callback`() = runCompilerTest {
        introspektTransformerPipeline()
        // @formatter:off
        source("""
            import dev.karmakrafts.introspekt.trace.Trace
            @Trace(Trace.Target.FUNCTION_ENTER)
            fun test() {
                println("HELLO, WORLD!")
            }
        """.trimIndent())
        // @formatter:on
        compiler shouldNotReport { error() }
        result irMatches {
            containsChild<IrCall> { call ->
                call.target.name.asString() == "enterFunction"
            }
        }
    }

    @Test
    fun `Inject leave function callback`() = runCompilerTest {
        introspektTransformerPipeline()
        // @formatter:off
        source("""
            import dev.karmakrafts.introspekt.trace.Trace
            @Trace(Trace.Target.FUNCTION_LEAVE)
            fun test() {
                println("HELLO, WORLD!")
            }
        """.trimIndent())
        // @formatter:on
        compiler shouldNotReport { error() }
        result irMatches {
            containsChild<IrCall> { call ->
                call.target.name.asString() == "leaveFunction"
            }
        }
    }

    @Test
    fun `Inject leave function callback with multiple return paths`() = runCompilerTest {
        introspektTransformerPipeline()
        // @formatter:off
        source("""
            import dev.karmakrafts.introspekt.trace.Trace
            @Trace(Trace.Target.FUNCTION_LEAVE)
            fun test(s: String) {
                if(s == "X") return
                println("HELLO, WORLD!")
            }
        """.trimIndent())
        // @formatter:on
        compiler shouldNotReport { error() }
        result irMatches {
            getChild<IrFunction> { it.name.asString() == "test" } matches {
                val body = element.body
                body shouldNotBe null
                body!!::class shouldBe IrBlockBodyImpl::class
                val blockBody = body as IrBlockBodyImpl
                blockBody.findChildren<IrCall> { it.target.name.asString() == "leaveFunction" }.size shouldBe 2
            }
        }
    }
}