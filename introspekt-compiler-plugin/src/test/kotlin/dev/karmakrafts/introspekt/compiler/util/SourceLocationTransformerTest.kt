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
import dev.karmakrafts.iridium.runCompilerTest
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.types.classFqName
import kotlin.test.Test

class SourceLocationTransformerTest {
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