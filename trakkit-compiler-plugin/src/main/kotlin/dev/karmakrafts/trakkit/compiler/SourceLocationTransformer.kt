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
        TrakkitIntrinsics.SL_HERE,
        TrakkitIntrinsics.SL_HERE_HASH,
        TrakkitIntrinsics.SL_CURRENT_FUNCTION,
        TrakkitIntrinsics.SL_CURRENT_FUNCTION_HASH,
        TrakkitIntrinsics.SL_CURRENT_CLASS,
        TrakkitIntrinsics.SL_CURRENT_CLASS_HASH
    )
) {
    override fun visitIntrinsic(
        type: TrakkitIntrinsics, expression: IrCall, context: IntrinsicContext
    ): IrElement = with(pluginContext) {
        when (type) { // @formatter:off
            TrakkitIntrinsics.SL_HERE ->
                getCallLocation(moduleFragment, file, source, expression, context.function).instantiate()
            TrakkitIntrinsics.SL_HERE_HASH ->
                getCallLocation(moduleFragment, file, source, expression, context.function).createHashSum()
            TrakkitIntrinsics.SL_CURRENT_FUNCTION ->
                getFunctionLocation(moduleFragment, file, source, requireNotNull(context.function) { "Not inside any function" }).instantiate()
            TrakkitIntrinsics.SL_CURRENT_FUNCTION_HASH ->
                getFunctionLocation(moduleFragment, file, source, requireNotNull(context.function) { "Not inside any function" }).createHashSum()
            TrakkitIntrinsics.SL_CURRENT_CLASS ->
                getClassLocation(moduleFragment, file, source, requireNotNull(context.clazz) { "Not inside any class" }).instantiate()
            TrakkitIntrinsics.SL_CURRENT_CLASS_HASH ->
                getClassLocation(moduleFragment, file, source, requireNotNull(context.clazz) { "Not inside any class" }).createHashSum()
            else -> error("Unsupported intrinsic for SourceLocationTransformer")
        } // @formatter:on
    }
}