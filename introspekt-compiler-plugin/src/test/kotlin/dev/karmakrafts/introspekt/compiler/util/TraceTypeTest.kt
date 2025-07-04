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
import dev.karmakrafts.iridium.setupCompilerTest
import org.jetbrains.kotlin.ir.declarations.IrFunction
import kotlin.test.Ignore
import kotlin.test.Test

class TraceTypeTest {
    @Test
    fun `Get trace type`() = setupCompilerTest {
        introspektPipeline()
        for (traceType in TraceType.entries) {
            resetAssertions()
            compiler shouldNotReport { error() }
            // @formatter:off
            source("""
                import dev.karmakrafts.introspekt.trace.Trace
                @Trace(Trace.Target.${traceType.name})
                fun test() {
                    println("Hello, World!")
                }
            """.trimIndent())
            // @formatter:on
            result irMatches {
                getChild<IrFunction> { it.name.asString() == "test" }.getTraceType()!! shouldContain listOf(traceType)
            }
            evaluate()
        }
    }
}