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

import dev.karmakrafts.introspekt.compiler.IntrospektPluginContext
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.toIrConst

internal data class StdPair( // @formatter:off
    val first: IrExpression?,
    val second: IrExpression?
) { // @formatter:on
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    fun instantiate(
        context: IntrospektPluginContext,
        firstType: IrType = first?.type ?: context.irBuiltIns.anyType.makeNullable(),
        secondType: IrType = second?.type ?: context.irBuiltIns.anyType.makeNullable()
    ): IrConstructorCallImpl = with(context) {
        IrConstructorCallImpl(
            startOffset = SYNTHETIC_OFFSET,
            endOffset = SYNTHETIC_OFFSET,
            type = introspektSymbols.pairType.typeWith(firstType, secondType),
            symbol = introspektSymbols.pairConstructor,
            typeArgumentsCount = 2,
            constructorTypeArgumentsCount = 2
        ).apply {
            val constructor = symbol.owner
            typeArguments[0] = firstType
            typeArguments[1] = secondType
            arguments[constructor.parameters.first { it.name.asString() == "first" }] =
                first ?: null.toIrConst(firstType)
            arguments[constructor.parameters.first { it.name.asString() == "second" }] =
                second ?: null.toIrConst(secondType)
        }
    }
}

internal fun Pair<IrExpression?, IrExpression?>.toStdPair(): StdPair {
    val (first, second) = this
    return StdPair(first, second)
}