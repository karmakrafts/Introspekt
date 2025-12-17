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

import dev.karmakrafts.introspekt.compiler.util.TraceType
import dev.karmakrafts.introspekt.compiler.util.getTraceType
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET

internal class TraceRemovalTransformer : TraceTransformer() {
    companion object {
        private val types: Set<TraceType> = setOf( // @formatter:off
            TraceType.SPAN_ENTER,
            TraceType.SPAN_LEAVE,
            TraceType.EVENT
        ) // @formatter:on
    }

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression, data: TraceContext): IrElement {
        val transformedCall = super.visitFunctionAccess(expression, data)
        if (transformedCall is IrFunctionAccessExpression) {
            // Find out if the target function is a trace function and if so, which type
            val callTraceType = transformedCall.getTraceType() ?: return transformedCall
            // If the called trace function isn't supposed to be handled by this transformer, we return early
            if (callTraceType !in types) return transformedCall
            // Find out the trace types of the current parent scope (function or constructor)
            val allowedTraceTypes = data.traceType ?: return transformedCall
            // If the called trace function is present in the current trace scope, return early as calls may stay
            if (callTraceType in allowedTraceTypes) return transformedCall
            // Otherwise we remove the call by stubbing it with an empty composite expression
            return IrCompositeImpl( // @formatter:off
                startOffset = SYNTHETIC_OFFSET,
                endOffset = SYNTHETIC_OFFSET,
                type = data.pluginContext.irBuiltIns.unitType
            ) // @formatter:on
        }
        return transformedCall
    }
}