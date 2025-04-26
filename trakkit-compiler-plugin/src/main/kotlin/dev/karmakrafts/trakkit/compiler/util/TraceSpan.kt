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

package dev.karmakrafts.trakkit.compiler.util

import dev.karmakrafts.trakkit.compiler.TrakkitPluginContext
import dev.karmakrafts.trakkit.compiler.element.FunctionInfo
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET

internal data class TraceSpan( // @formatter:off
    val name: IrExpression,
    val start: SourceLocation,
    val end: SourceLocation,
    val function: FunctionInfo
) { // @formatter:on
    fun instantiate(context: TrakkitPluginContext): IrConstructorCallImpl = with(context) {
        IrConstructorCallImpl(
            startOffset = SYNTHETIC_OFFSET,
            endOffset = SYNTHETIC_OFFSET,
            type = traceSpanType.defaultType,
            symbol = traceSpanConstructor,
            typeArgumentsCount = 0,
            constructorTypeArgumentsCount = 0
        ).apply {
            var index = 0
            putValueArgument(index++, name)
            putValueArgument(index++, start.instantiateCached())
            putValueArgument(index++, end.instantiateCached())
            putValueArgument(index, function.instantiateCached(context))
        }
    }
}