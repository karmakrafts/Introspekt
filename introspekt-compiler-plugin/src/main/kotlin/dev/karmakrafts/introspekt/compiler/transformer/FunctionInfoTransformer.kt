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

import dev.karmakrafts.introspekt.compiler.element.getFunctionInfo
import dev.karmakrafts.introspekt.compiler.util.IntrospektIntrinsic
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.target
import org.jetbrains.kotlin.ir.util.toIrConst

internal class FunctionInfoTransformer : IntrinsicTransformer(
    setOf( // @formatter:off
        IntrospektIntrinsic.FI_CURRENT,
        IntrospektIntrinsic.FI_OF
    ) // @formatter:on
) {
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun emitOf(expression: IrCall, context: IntrinsicContext): IrElement {
        val pluginContext = context.pluginContext
        val moduleFragment = pluginContext.irModule
        val file = pluginContext.irFile
        val source = pluginContext.source
        val parameter = expression.target.parameters.first { it.kind == IrParameterKind.Regular }
        val argument = expression.arguments[parameter]
        check(argument is IrFunctionReference) { "Parameter must be a function reference" }
        return requireNotNull(argument.reflectionTarget) {
            "Parameter reference must have a reflection target"
        }.owner.getFunctionInfo(moduleFragment, file, source)
            .instantiateCached(moduleFragment, file, source, pluginContext)
    }

    override fun visitIntrinsic(
        type: IntrospektIntrinsic, expression: IrCall, context: IntrinsicContext
    ): IrElement {
        val pluginContext = context.pluginContext
        val moduleFragment = pluginContext.irModule
        val file = pluginContext.irFile
        val source = pluginContext.source
        return when (type) { // @formatter:off
            IntrospektIntrinsic.FI_CURRENT -> context.getFunctionInfo(moduleFragment, file, source)
                ?.instantiateCached(moduleFragment, file, source, pluginContext)
                ?: null.toIrConst(expression.type)
            IntrospektIntrinsic.FI_OF -> emitOf(expression, context)
            else -> error("Unsupported intrinsic for FunctionInfoTransformer")
        }
    } // @formatter:on
}