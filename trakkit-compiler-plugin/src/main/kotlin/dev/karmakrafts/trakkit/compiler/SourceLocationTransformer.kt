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

package dev.karmakrafts.trakkit.compiler

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall

internal class SourceLocationTransformer( // @formatter:off
    val pluginContext: TrakkitPluginContext,
    val moduleFragment: IrModuleFragment,
    val file: IrFile,
    val source: List<String>
) : TrakkitIntrinsicTransformer( // @formatter:on
    setOf(
        TrakkitIntrinsic.SL_HERE,
        TrakkitIntrinsic.SL_HERE_HASH,
        TrakkitIntrinsic.SL_CURRENT_FUNCTION,
        TrakkitIntrinsic.SL_CURRENT_FUNCTION_HASH,
        TrakkitIntrinsic.SL_CURRENT_CLASS,
        TrakkitIntrinsic.SL_CURRENT_CLASS_HASH
    )
) {
    override fun visitIntrinsic(
        type: TrakkitIntrinsic, expression: IrCall, context: IntrinsicContext
    ): IrElement = with(pluginContext) {
        when (type) { // @formatter:off
            TrakkitIntrinsic.SL_HERE ->
                getCallLocation(moduleFragment, file, source, expression, context.function).instantiate()
            TrakkitIntrinsic.SL_HERE_HASH ->
                getCallLocation(moduleFragment, file, source, expression, context.function).createHashSum()
            TrakkitIntrinsic.SL_CURRENT_FUNCTION ->
                getFunctionLocation(moduleFragment, file, source, requireNotNull(context.function) { "Not inside any function" }).instantiate()
            TrakkitIntrinsic.SL_CURRENT_FUNCTION_HASH ->
                getFunctionLocation(moduleFragment, file, source, requireNotNull(context.function) { "Not inside any function" }).createHashSum()
            TrakkitIntrinsic.SL_CURRENT_CLASS ->
                getClassLocation(moduleFragment, file, source, requireNotNull(context.clazz) { "Not inside any class" }).instantiate()
            TrakkitIntrinsic.SL_CURRENT_CLASS_HASH ->
                getClassLocation(moduleFragment, file, source, requireNotNull(context.clazz) { "Not inside any class" }).createHashSum()
            else -> error("Unsupported intrinsic for SourceLocationTransformer")
        } // @formatter:on
    }
}