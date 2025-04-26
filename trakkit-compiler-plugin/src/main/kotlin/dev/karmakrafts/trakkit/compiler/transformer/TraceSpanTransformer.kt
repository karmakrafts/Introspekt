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
import dev.karmakrafts.trakkit.compiler.TrakkitPluginContext
import dev.karmakrafts.trakkit.compiler.util.TraceSpan
import dev.karmakrafts.trakkit.compiler.util.TrakkitIntrinsic
import dev.karmakrafts.trakkit.compiler.util.getIntrinsicType
import dev.karmakrafts.trakkit.compiler.util.getLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImplWithShape
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.target
import org.jetbrains.kotlin.ir.visitors.IrVisitor

internal class FindTraceEndContext {
    var hasStart: Boolean = false
    var isFound: Boolean = false
    var call: IrCall? = null
}

internal class FindTraceEndVisitor(
    private val skipUntilCall: IrCall
) : IrVisitor<Unit, FindTraceEndContext>() {
    override fun visitElement(element: IrElement, data: FindTraceEndContext) {
        if (data.isFound) return
        element.acceptChildren(this, data)
    }

    override fun visitCall(expression: IrCall, data: FindTraceEndContext) {
        super.visitCall(expression, data)
        if (expression == skipUntilCall) {
            data.hasStart = true
            return
        }
        if (!data.hasStart || expression.target.getIntrinsicType() != TrakkitIntrinsic.TS_POP) return
        data.isFound = true
        data.call = expression
    }
}

internal class TraceSpanTransformer(
    private val pluginContext: TrakkitPluginContext,
    private val module: IrModuleFragment,
    private val file: IrFile,
    private val source: List<String>
) : IntrinsicTransformer(setOf(TrakkitIntrinsic.TS_PUSH)) {
    private fun findTraceEnd(expression: IrCall, body: IrBody): IrCall? {
        val context = FindTraceEndContext()
        body.acceptChildren(FindTraceEndVisitor(expression), context)
        return context.call
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun visitIntrinsic(type: TrakkitIntrinsic, expression: IrCall, context: IntrinsicContext): IrElement =
        with(pluginContext) {
            val body = context.bodyOrNull ?: return@with expression
            // Select either the current function or the primary enclosing class constructor if we are inside an init-block
            // @formatter:off
            val function = context.functionOrNull
                ?: context.classOrNull?.primaryConstructor
                ?: return@with expression
            // @formatter:on
            val nameIndex = expression.target.parameters.filter { it.kind == IrParameterKind.Regular }
                .first { it.name.asString() == "name" }.indexInOldValueParameters
            when (type) {
                TrakkitIntrinsic.TS_PUSH -> IrCallImplWithShape(
                    startOffset = SYNTHETIC_OFFSET,
                    endOffset = SYNTHETIC_OFFSET,
                    type = traceSpanType.defaultType,
                    symbol = traceSpanPush,
                    typeArgumentsCount = 0,
                    valueArgumentsCount = 1,
                    contextParameterCount = 0,
                    hasDispatchReceiver = true,
                    hasExtensionReceiver = false
                ).apply { // @formatter:off
                    putValueArgument(0, TraceSpan(
                        name = expression.getValueArgument(nameIndex)!!,
                        start = expression.getLocation(module, file, source),
                        end = requireNotNull(findTraceEnd(expression, body)) {
                            "Could not find TraceSpan end in ${function.kotlinFqName}"
                        }.getLocation(module, file, source),
                        function = function.getFunctionInfo(module, file, source)
                    ).instantiate(pluginContext))
                    // @formatter:on
                    dispatchReceiver = traceSpanCompanionType.getObjectInstance()
                }

                else -> error("Unsupported intrinsic $type for TraceSpanTransformer")
            }
        }
}