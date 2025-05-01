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

import dev.karmakrafts.introspekt.compiler.IntrospektPluginContext
import dev.karmakrafts.introspekt.compiler.element.getFunctionInfo
import dev.karmakrafts.introspekt.compiler.util.IntrospektIntrinsic
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.target

internal class FunctionInfoTransformer(
    private val pluginContext: IntrospektPluginContext,
    private val moduleFragment: IrModuleFragment,
    private val file: IrFile,
    private val source: List<String>
) : IntrinsicTransformer(
    setOf( // @formatter:off
        IntrospektIntrinsic.FI_CURRENT,
        IntrospektIntrinsic.FI_OF
    ) // @formatter:on
) {
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun emitOf(expression: IrCall): IrElement {
        val parameter = expression.target.parameters.first { it.kind == IrParameterKind.Regular }
        val argument = expression.getValueArgument(parameter.indexInOldValueParameters)
        check(argument is IrFunctionReference) { "Parameter must be a function reference" }
        return requireNotNull(argument.reflectionTarget) {
            "Parameter reference must have a reflection target"
        }.owner.getFunctionInfo(moduleFragment, file, source).instantiateCached(pluginContext)
    }

    override fun visitIntrinsic(
        type: IntrospektIntrinsic, expression: IrCall, context: IntrinsicContext
    ): IrElement = when (type) { // @formatter:off
        IntrospektIntrinsic.FI_CURRENT -> context.getFunctionInfo(moduleFragment, file, source).instantiateCached(pluginContext)
        IntrospektIntrinsic.FI_OF -> emitOf(expression)
        else -> error("Unsupported intrinsic for FunctionInfoTransformer")
    } // @formatter:on
}