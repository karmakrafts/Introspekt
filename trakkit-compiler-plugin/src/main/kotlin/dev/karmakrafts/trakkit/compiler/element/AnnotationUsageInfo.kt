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

package dev.karmakrafts.trakkit.compiler.element

import dev.karmakrafts.trakkit.compiler.TrakkitPluginContext
import dev.karmakrafts.trakkit.compiler.util.SourceLocation
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.toIrConst

internal data class AnnotationUsageInfo( // @formatter:off
    val location: SourceLocation,
    val type: IrType,
    val values: Map<String, Any?>
) { // @formatter:on
    fun instantiate(context: TrakkitPluginContext): IrConstructorCallImpl = with(context) {
        return IrConstructorCallImpl(
            startOffset = SYNTHETIC_OFFSET,
            endOffset = SYNTHETIC_OFFSET,
            type = annotationInfoType.defaultType,
            symbol = annotationInfoConstructor,
            typeArgumentsCount = 0,
            constructorTypeArgumentsCount = 0
        ).apply {
            var index = 0
            putValueArgument(index++, location.instantiateCached())
            putValueArgument(index++, this@AnnotationUsageInfo.type.toClassReference())
            putValueArgument(
                index, createMapOf(
                    keyType = irBuiltIns.stringType,
                    valueType = irBuiltIns.anyType,
                    values = values.map { (key, value) ->
                        key.toIrConst(irBuiltIns.stringType) to value?.toIrValue()
                    })
            )
        }
    }
}