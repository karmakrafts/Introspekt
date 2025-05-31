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

package dev.karmakrafts.introspekt.compiler.util

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

private typealias InjectionTransformerFactory = ( // @formatter:off
    needleSelector: (IrStatement) -> Boolean,
    injection: () -> List<IrStatement>
) -> IrElementTransformerVoid // @formatter:on

private class BeforeInjectionTransformer( // @formatter:off
    private val needleSelector: (IrStatement) -> Boolean,
    private val injection: () -> List<IrStatement>
) : IrElementTransformerVoid() { // @formatter:on
    override fun visitExpression(expression: IrExpression): IrExpression {
        val transformedExpression = super.visitExpression(expression)
        if (needleSelector(transformedExpression)) {
            return IrCompositeImpl(
                startOffset = SYNTHETIC_OFFSET,
                endOffset = SYNTHETIC_OFFSET,
                type = transformedExpression.type,
                origin = null,
                statements = injection() + transformedExpression
            )
        }
        return transformedExpression
    }
}

private class AfterInjectionTransformer( // @formatter:off
    private val needleSelector: (IrStatement) -> Boolean,
    private val injection: () -> List<IrStatement>
) : IrElementTransformerVoid() { // @formatter:on
    override fun visitExpression(expression: IrExpression): IrExpression {
        val transformedExpression = super.visitExpression(expression)
        if (needleSelector(transformedExpression)) {
            return IrCompositeImpl(
                startOffset = SYNTHETIC_OFFSET,
                endOffset = SYNTHETIC_OFFSET,
                type = transformedExpression.type,
                origin = null,
                statements = listOf(transformedExpression) + injection()
            )
        }
        return transformedExpression
    }
}

internal enum class InjectionOrder(
    internal val transformerFactory: InjectionTransformerFactory
) {
    // @formatter:off
    BEFORE  (::BeforeInjectionTransformer),
    AFTER   (::AfterInjectionTransformer),
    // @formatter:on
}

internal fun IrElement.inject( // @formatter:off
    needleSelector: (IrStatement) -> Boolean,
    injection: () -> List<IrStatement>,
    order: InjectionOrder = InjectionOrder.AFTER
) { // @formatter:on
    transform(order.transformerFactory(needleSelector, injection), null)
}