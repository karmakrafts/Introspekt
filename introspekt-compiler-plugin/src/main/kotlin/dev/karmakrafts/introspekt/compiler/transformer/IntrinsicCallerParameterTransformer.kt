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

import dev.karmakrafts.introspekt.compiler.util.IntrospektIntrinsic
import dev.karmakrafts.introspekt.compiler.util.IntrospektNames
import dev.karmakrafts.introspekt.compiler.IntrospektPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.target
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

/**
 * Invoked before the main intrinsic passes but after the de-defaulting pass
 * to adjust all callsites of functions with an injected @CaptureCaller annotation.
 */
internal class IntrinsicCallerParameterTransformer(
    private val pluginContext: IntrospektPluginContext
) : IrVisitorVoid() {
    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    @Suppress("UNCHECKED_CAST")
    private fun transformCall(expression: IrFunctionAccessExpression) {
        val function = expression.target
        if (!function.hasAnnotation(IntrospektNames.CaptureCaller.id)) return
        with(pluginContext) {
            val annotationValues = function.getAnnotation(IntrospektNames.CaptureCaller.fqName)!!.getAnnotationValues()
            val intrinsicStrings = annotationValues["intrinsics"] as? List<String> ?: return@with
            val valueArgumentsCount = expression.valueArgumentsCount
            intrinsicStrings.map { stringValue ->
                val (index, name) = stringValue.split(":")
                Pair(index.toInt(), IntrospektIntrinsic.valueOf(name))
            }.forEach { (index, type) ->
                if (index < valueArgumentsCount && expression.getValueArgument(index) != null) return@forEach
                expression.putValueArgument(
                    index, type.createCall(
                        startOffset = expression.startOffset,
                        endOffset = expression.endOffset,
                    )
                )
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