/*
 * Copyright 2025 Karma Krafts
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
import dev.karmakrafts.introspekt.compiler.isCachedClassInfo
import dev.karmakrafts.introspekt.compiler.util.IntrospektNames
import dev.karmakrafts.introspekt.util.SourceLocation
import dev.karmakrafts.iridium.runCompilerTest
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.types.classFqName
import kotlin.test.Test

class ClassInfoTransformerTest {
    @Test
    fun `Obtain class info of class via type parameter`() = runCompilerTest {
        introspektTransformerPipeline()
        // @formatter:off
        source("""
            package com.example
            import dev.karmakrafts.introspekt.element.ClassInfo
            class Test {
                val foo: String = "HELLO, WORLD!"
                fun bar(s: String) {}
            }
            fun foo() {
                val info = ClassInfo.of<Test>()
            }
        """.trimIndent())
        // @formatter:on
        compiler shouldNotReport { error() }
        result irMatches {
            getChild<IrCall> { it.type.classFqName == IntrospektNames.ClassInfo.fqName } matches {
                isCachedClassInfo(
                    module = "test",
                    file = "test",
                    line = 3,
                    column = 1,
                    qualifiedName = "com.example.Test",
                    name = "Test",
                    properties = mapOf( // @formatter:off
                        "foo" to types.stringType
                    ), // @formatter:on
                    functions = mapOf( // @formatter:off
                        "bar" to listOf(types.unitType, types.stringType)
                    ) // @formatter:on
                ) { type("com/example/Test") }
            }
        }
    }

    @Test
    fun `Obtain class info of primitive type via type parameter`() = runCompilerTest {
        introspektTransformerPipeline()
        // @formatter:off
        source("""
            package com.example
            import dev.karmakrafts.introspekt.element.ClassInfo
            fun foo() {
                val info = ClassInfo.of<Int>()
            }
        """.trimIndent())
        // @formatter:on
        compiler shouldNotReport { error() }
        result irMatches {
            getChild<IrCall> { it.type.classFqName == IntrospektNames.ClassInfo.fqName } matches {
                isCachedClassInfo(
                    module = "test",
                    file = "test",
                    line = SourceLocation.UNDEFINED_OFFSET,
                    column = SourceLocation.UNDEFINED_OFFSET,
                    qualifiedName = "kotlin.Int",
                    name = "Int",
                    properties = mapOf(
                        "MIN_VALUE" to types.intType,
                        "MAX_VALUE" to types.intType,
                        "SIZE_BYTES" to types.intType,
                        "SIZE_BITS" to types.intType
                    ),
                    functions = mapOf(
                        "compareTo" to listOf(types.intType, types.byteType),
                        "plus" to listOf(types.intType, types.byteType),
                        "minus" to listOf(types.intType, types.byteType),
                        "times" to listOf(types.intType, types.byteType),
                        "div" to listOf(types.intType, types.byteType)
                    )
                ) { type("kotlin/Int") }
            }
        }
    }
}