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

import dev.karmakrafts.introspekt.compiler.IntrospektPluginContext
import dev.karmakrafts.introspekt.compiler.util.InlineDefaultMode
import dev.karmakrafts.introspekt.compiler.util.IntrospektIntrinsic
import dev.karmakrafts.introspekt.compiler.util.IntrospektNames
import dev.karmakrafts.introspekt.compiler.util.getInlineDefaultModes
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.target
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

internal class InlineDefaultCallerTransformer(
    private val pluginContext: IntrospektPluginContext
) : IrVisitorVoid() {
    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    private fun inlineIntrinsicDefault( // @formatter:off
        expression: IrFunctionAccessExpression,
        parameter: IrValueParameter,
        type: IntrospektIntrinsic
    ) { // @formatter:on
        // Inline intrinsic call to specified intrinsic type
        expression.arguments[parameter] = type.createCall( // @formatter:off
            context = pluginContext,
            startOffset = expression.startOffset,
            endOffset = expression.endOffset
        ) // @formatter:on
    }

    private fun transformCall(expression: IrFunctionAccessExpression) {
        val function = expression.target
        if (!function.hasAnnotation(IntrospektNames.InlineDefaults.id)) return
        val modes = function.getInlineDefaultModes()
        val parameters = function.parameters.filter { it.kind == IrParameterKind.Regular }
        check(parameters.size == modes.size) { "Number of inline modes must match parameter count" }
        val modeIterator = modes.iterator()
        for (parameter in parameters) {
            val argument = expression.arguments[parameter]
            val mode = modeIterator.next()
            if (argument != null) continue // Skip any parameters that already have a call site argument
            when (mode) {
                InlineDefaultMode.None -> continue
                is InlineDefaultMode.Intrinsic -> inlineIntrinsicDefault(expression, parameter, mode.type)
            }
        }
    }

    override fun visitConstructorCall(expression: IrConstructorCall) {
        super.visitConstructorCall(expression)
        transformCall(expression)
    }

    override fun visitCall(expression: IrCall) {
        super.visitCall(expression)
        transformCall(expression)
    }
}