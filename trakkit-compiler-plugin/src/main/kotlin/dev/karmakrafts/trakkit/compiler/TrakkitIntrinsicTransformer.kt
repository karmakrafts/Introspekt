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

package dev.karmakrafts.trakkit.compiler

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.util.getAnnotationArgumentValue
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.target
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer

internal abstract class TrakkitIntrinsicTransformer<D>(
    val intrinsics: Set<TrakkitIntrinsics>
) : IrElementTransformer<D> {
    override fun visitElement(element: IrElement, data: D): IrElement {
        element.transformChildren(this, data)
        return element
    }

    override fun visitCall(expression: IrCall, data: D): IrElement {
        val function = expression.target

        if (!function.hasAnnotation(TrakkitNames.TrakkitIntrinsic.id)) return super.visitCall(expression, data)
        val intrinsicName = function.getAnnotationArgumentValue<String>(TrakkitNames.TrakkitIntrinsic.fqName, "value")
            ?: return super.visitCall(expression, data)
        val intrinsicType = TrakkitIntrinsics.byName(intrinsicName) ?: return super.visitCall(expression, data)
        if (intrinsicType !in intrinsics) return super.visitCall(expression, data)

        return visitIntrinsic(intrinsicType, expression, data)
    }

    abstract fun visitIntrinsic(type: TrakkitIntrinsics, expression: IrCall, data: D): IrElement
}