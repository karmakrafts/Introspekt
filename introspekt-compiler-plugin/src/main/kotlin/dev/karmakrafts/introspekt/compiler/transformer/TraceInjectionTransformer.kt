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
import dev.karmakrafts.introspekt.compiler.util.InjectionOrder
import dev.karmakrafts.introspekt.compiler.util.TraceType
import dev.karmakrafts.introspekt.compiler.util.getTraceType
import dev.karmakrafts.introspekt.compiler.util.inject
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.target

internal class TraceInjectionTransformer : TraceTransformer() {
    private val functionTraceTypes: Set<TraceType> = setOf(TraceType.FUNCTION_ENTER, TraceType.FUNCTION_LEAVE)
    private val functions: ArrayList<Pair<IrFunction, List<TraceType>>> = ArrayList()
    private val calls: ArrayList<Pair<IrElement, IrCall>> = ArrayList()

    override fun visitCall(expression: IrCall, data: TraceContext) {
        super.visitCall(expression, data)
        val allowedTraceTypes = data.traceType
        if (allowedTraceTypes.isEmpty()) return
        if (expression.getTraceType() != null) return
        if (TraceType.CALL !in allowedTraceTypes) return
        calls += Pair(data.container, expression)
    }

    override fun visitTraceableFunction(declaration: IrFunction, data: TraceContext) {
        val allowedTraceTypes = data.traceType
        if (allowedTraceTypes.isEmpty()) return
        val functionTraceTypes = allowedTraceTypes.intersect(functionTraceTypes).toList()
        if (functionTraceTypes.isEmpty()) return
        functions += Pair(declaration, functionTraceTypes)
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun injectFunctionCallbacks(context: IntrospektPluginContext) {
        for ((function, types) in functions) {
            val body = function.body ?: continue
            val blockBody = body as? IrBlockBody ?: continue
            for (type in types) when (type) {
                TraceType.FUNCTION_ENTER -> blockBody.statements.add(0, type.createCall(context))
                TraceType.FUNCTION_LEAVE -> {
                    // Inject before every explicit return
                    function.inject(
                        needleSelector = { it is IrReturn },
                        injection = { listOf(type.createCall(context)) },
                        order = InjectionOrder.BEFORE
                    )
                    // Special case for implicit Unit returns
                    if (!function.returnType.isUnit()) continue
                    blockBody.statements.add(blockBody.statements.size, type.createCall(context))
                }

                else -> error("Function callback injection does not support target $type")
            }
        }
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun injectCallCallbacks( // @formatter:off
        context: IntrospektPluginContext,
        module: IrModuleFragment,
        file: IrFile,
        source: List<String>
    ) { // @formatter:on
        for ((container, call) in calls) {
            container.inject( // @formatter:off
                needleSelector = { it == call },
                injection = {
                    listOf(TraceType.CALL.createCall(context).apply {
                        val function = symbol.owner
                        arguments[function.parameters.first { it.name.asString() == "callee" }] =
                            call.target.getFunctionInfo(module, file, source)
                                .instantiateCached(module, file, source, context)
                    })
                }
            ) // @formatter:on
        }
    }

    fun injectCallbacks( // @formatter:off
        context: IntrospektPluginContext,
        module: IrModuleFragment,
        file: IrFile,
        source: List<String>
    ) { // @formatter:on
        injectFunctionCallbacks(context)
        injectCallCallbacks(context, module, file, source)
    }
}