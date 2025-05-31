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
import dev.karmakrafts.introspekt.compiler.isCachedFunctionInfo
import dev.karmakrafts.introspekt.compiler.util.IntrospektNames
import dev.karmakrafts.iridium.runCompilerTest
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.types.classFqName
import kotlin.test.Test

class FunctionInfoTransformerTest {
    @Test
    fun `Obtain current function info`() = runCompilerTest {
        introspektTransformerPipeline()
        // @formatter:off
        source("""
            package com.example
            import dev.karmakrafts.introspekt.element.FunctionInfo
            class Test {
                fun foo() {
                    val info = FunctionInfo.current()
                }
            }
        """.trimIndent())
        // @formatter:on
        compiler shouldNotReport { error() }
        result irMatches {
            getChild<IrCall> { it.type.classFqName == IntrospektNames.FunctionInfo.fqName } matches {
                isCachedFunctionInfo( // @formatter:off
                    module = "test",
                    file = "test",
                    line = 4,
                    column = 5,
                    qualifiedName = "com.example.Test.foo",
                    name = "foo"
                ) // @formatter:on
            }
        }
    }

    @Test
    fun `Obtain function info via function reference`() = runCompilerTest {
        introspektTransformerPipeline()
        // @formatter:off
        source("""
            package com.example
            import dev.karmakrafts.introspekt.element.FunctionInfo
            class Test {
                fun test() {}
                fun foo() {
                    val info = FunctionInfo.of(::test)
                }
            }
        """.trimIndent())
        // @formatter:on
        compiler shouldNotReport { error() }
        result irMatches {
            getChild<IrCall> { it.type.classFqName == IntrospektNames.FunctionInfo.fqName } matches {
                isCachedFunctionInfo( // @formatter:off
                    module = "test",
                    file = "test",
                    line = 4,
                    column = 5,
                    qualifiedName = "com.example.Test.test",
                    name = "test"
                ) // @formatter:on
            }
        }
    }
}