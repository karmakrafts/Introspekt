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
import dev.karmakrafts.trakkit.compiler.util.TrakkitIntrinsic
import dev.karmakrafts.trakkit.compiler.util.getIntrinsicType
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.util.target
import org.jetbrains.kotlin.ir.visitors.IrTransformer

internal abstract class TrakkitIntrinsicTransformer(
    private val intrinsics: Set<TrakkitIntrinsic>
) : IrTransformer<IntrinsicContext>() {
    override fun visitElement(element: IrElement, data: IntrinsicContext): IrElement {
        element.transformChildren(this, data)
        return element
    }

    override fun visitClass(
        declaration: IrClass, data: IntrinsicContext
    ): IrStatement {
        data.clazzStack.push(declaration)
        val result = super.visitClass(declaration, data)
        data.clazzStack.pop()
        return result
    }

    override fun visitFunction(
        declaration: IrFunction, data: IntrinsicContext
    ): IrStatement {
        data.functionStack.push(declaration)
        val result = super.visitFunction(declaration, data)
        data.functionStack.pop()
        return result
    }

    override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer, data: IntrinsicContext): IrStatement {
        data.initializerStack.push(declaration)
        val result = super.visitAnonymousInitializer(declaration, data)
        data.initializerStack.pop()
        return result
    }

    override fun visitCall(expression: IrCall, data: IntrinsicContext): IrElement {
        val intrinsicType = expression.target.getIntrinsicType() ?: return super.visitCall(expression, data)
        if (intrinsicType !in intrinsics) return super.visitCall(expression, data)
        return visitIntrinsic(intrinsicType, expression, data)
    }

    abstract fun visitIntrinsic(type: TrakkitIntrinsic, expression: IrCall, context: IntrinsicContext): IrElement
}