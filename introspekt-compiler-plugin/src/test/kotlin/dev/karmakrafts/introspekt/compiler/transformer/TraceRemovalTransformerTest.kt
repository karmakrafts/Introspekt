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
import dev.karmakrafts.iridium.runCompilerTest
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import kotlin.test.Test

class TraceRemovalTransformerTest {
    @Test
    fun `Remove enter trace span callback`() = runCompilerTest {
        introspektTransformerPipeline()
        // @formatter:off
        source("""
            import dev.karmakrafts.introspekt.trace.Trace
            import dev.karmakrafts.introspekt.trace.TraceSpan
            import kotlin.uuid.ExperimentalUuidApi
            @OptIn(ExperimentalUuidApi::class)
            @Trace(Trace.Target.FUNCTION_ENTER, Trace.Target.FUNCTION_LEAVE)
            fun test() {
                TraceSpan.enter("🦊")
            }
        """.trimIndent())
        // @formatter:on
        compiler shouldNotReport { error() }
        result irMatches {
            getChild<IrFunction> { it.name.asString() == "test" } matches {
                containsNoChild<IrCall>()
            }
        }
    }

    @Test
    fun `Remove leave trace span callback`() = runCompilerTest {
        introspektTransformerPipeline()
        // @formatter:off
        source("""
            import dev.karmakrafts.introspekt.trace.Trace
            import dev.karmakrafts.introspekt.trace.TraceSpan
            @Trace(Trace.Target.FUNCTION_ENTER, Trace.Target.FUNCTION_LEAVE)
            fun test() {
                TraceSpan.leave()
            }
        """.trimIndent())
        // @formatter:on
        compiler shouldNotReport { error() }
        result irMatches {
            getChild<IrFunction> { it.name.asString() == "test" } matches {
                containsNoChild<IrCall>()
            }
        }
    }

    @Test
    fun `Remove trace event callback`() = runCompilerTest {
        introspektTransformerPipeline()
        // @formatter:off
        source("""
            import dev.karmakrafts.introspekt.trace.Trace
            import kotlin.uuid.ExperimentalUuidApi
            @OptIn(ExperimentalUuidApi::class)
            @Trace(Trace.Target.FUNCTION_ENTER, Trace.Target.FUNCTION_LEAVE)
            fun test() {
                Trace.event("Have you seen the 🦊?")
            }
        """.trimIndent())
        // @formatter:on
        compiler shouldNotReport { error() }
        result irMatches {
            getChild<IrFunction> { it.name.asString() == "test" } matches {
                containsNoChild<IrCall>()
            }
        }
    }
}