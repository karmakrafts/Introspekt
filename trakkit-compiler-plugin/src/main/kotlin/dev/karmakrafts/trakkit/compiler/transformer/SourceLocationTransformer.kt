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

package dev.karmakrafts.trakkit.compiler.transformer

import dev.karmakrafts.trakkit.compiler.IntrinsicContext
import dev.karmakrafts.trakkit.compiler.util.TrakkitIntrinsic
import dev.karmakrafts.trakkit.compiler.TrakkitPluginContext
import dev.karmakrafts.trakkit.compiler.util.getFunctionLocation
import dev.karmakrafts.trakkit.compiler.util.getLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.target

internal class SourceLocationTransformer( // @formatter:off
    private val pluginContext: TrakkitPluginContext,
    private val moduleFragment: IrModuleFragment,
    private val file: IrFile,
    private val source: List<String>
) : IntrinsicTransformer( // @formatter:on
    setOf( // @formatter:off
        TrakkitIntrinsic.SL_HERE,
        TrakkitIntrinsic.SL_HERE_HASH,
        TrakkitIntrinsic.SL_CURRENT_FUNCTION,
        TrakkitIntrinsic.SL_CURRENT_FUNCTION_HASH,
        TrakkitIntrinsic.SL_CURRENT_CLASS,
        TrakkitIntrinsic.SL_CURRENT_CLASS_HASH,
        TrakkitIntrinsic.SL_OF_CLASS,
        TrakkitIntrinsic.SL_OF_FUNCTION
    ) // @formatter:on
) {
    private fun TrakkitPluginContext.emitOfClass(expression: IrCall): IrElement {
        return requireNotNull(expression.typeArguments.first()?.getClass()) {
            "Missing class type parameter"
        }.getLocation(moduleFragment, file, source).instantiateCached()
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun TrakkitPluginContext.emitOfFunction(expression: IrCall): IrElement {
        val parameter = expression.target.parameters.first { it.kind == IrParameterKind.Regular }
        val argument = expression.getValueArgument(parameter.indexInOldValueParameters)
        check(argument is IrFunctionReference) { "Parameter must be a function reference" }
        return requireNotNull(argument.reflectionTarget) {
            "Parameter reference must have a reflection target"
        }.owner.getFunctionLocation(moduleFragment, file, source).instantiateCached()
    }

    override fun visitIntrinsic(
        type: TrakkitIntrinsic, expression: IrCall, context: IntrinsicContext
    ): IrElement = with(pluginContext) {
        when (type) { // @formatter:off
            TrakkitIntrinsic.SL_HERE -> expression.getLocation(moduleFragment, file, source).instantiateCached()
            TrakkitIntrinsic.SL_HERE_HASH -> expression.getLocation(moduleFragment, file, source).createHashSum()
            TrakkitIntrinsic.SL_CURRENT_FUNCTION -> context.getFunctionLocation(moduleFragment, file, source).instantiateCached()
            TrakkitIntrinsic.SL_CURRENT_FUNCTION_HASH -> context.getFunctionLocation(moduleFragment, file, source).createHashSum()
            TrakkitIntrinsic.SL_CURRENT_CLASS -> context.clazz.getLocation(moduleFragment, file, source).instantiateCached()
            TrakkitIntrinsic.SL_CURRENT_CLASS_HASH -> context.clazz.getLocation(moduleFragment, file, source).createHashSum()
            TrakkitIntrinsic.SL_OF_CLASS -> emitOfClass(expression)
            TrakkitIntrinsic.SL_OF_FUNCTION -> emitOfFunction(expression)
            else -> error("Unsupported intrinsic for SourceLocationTransformer")
        } // @formatter:on
    }
}