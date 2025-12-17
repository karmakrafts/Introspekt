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

package dev.karmakrafts.introspekt.compiler

import dev.karmakrafts.introspekt.compiler.util.IntrospektIntrinsic
import dev.karmakrafts.introspekt.compiler.util.getIntrinsicType
import dev.karmakrafts.iridium.setupCompilerTest
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import kotlin.test.Test

class IntrospektIntrinsicTest {
    @Test
    fun `Get intrinsic type`() = setupCompilerTest {
        introspektPipeline()
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
                getChild<IrFunction> { it.name.asString() == "intrinsic" }.getIntrinsicType() shouldBe intrinsicType
            }
            evaluate()
        }
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    @Test
    fun `Create call`() = setupCompilerTest {
        introspektPipeline()
        for (intrinsicType in IntrospektIntrinsic.entries) {
            resetAssertions()
            compiler shouldNotReport { error() }
            result irMatches {
                val introspektSymbols = IntrospektSymbols(pluginContext)
                val introspektContext = IntrospektPluginContext(pluginContext, element, element.files.first(), this@setupCompilerTest.sourceLines, introspektSymbols)

                val call = intrinsicType.createCall(introspektContext)
                call.type shouldBe intrinsicType.resultType(introspektContext)

                val function = call.symbol.owner
                function.name shouldBe intrinsicType.functionId.callableName

                val clazz = function.parentClassOrNull
                clazz shouldNotBe null
                clazz!!.name shouldBe intrinsicType.functionId.classId!!.shortClassName
            }
            evaluate()
        }
    }
}