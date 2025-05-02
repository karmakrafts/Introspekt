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
import dev.karmakrafts.introspekt.compiler.util.FrameSnapshot
import dev.karmakrafts.introspekt.compiler.util.IntrospektIntrinsic
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall

internal class FrameSnapshotTransformer( // @formatter:off
    val pluginContext: IntrospektPluginContext,
    val module: IrModuleFragment,
    val file: IrFile,
    val source: List<String>
) : IntrinsicTransformer( // @formatter:on
    setOf(IntrospektIntrinsic.FS_CREATE)
) {
    override fun visitIntrinsic( // @formatter:off
        type: IntrospektIntrinsic,
        expression: IrCall,
        context: IntrinsicContext
    ): IrElement = when (type) { // @formatter:on
        IntrospektIntrinsic.FS_CREATE -> context.createFrameSnapshot(module, file, source, expression)
            ?.instantiate(pluginContext) ?: FrameSnapshot.empty(pluginContext)

        else -> error("Unsupported intrinsic for FrameSnapshotTransformer")
    }
}