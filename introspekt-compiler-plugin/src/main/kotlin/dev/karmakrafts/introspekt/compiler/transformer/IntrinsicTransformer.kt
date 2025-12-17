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

import dev.karmakrafts.introspekt.compiler.util.IntrospektIntrinsic
import dev.karmakrafts.introspekt.compiler.util.getIntrinsicType
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.util.target
import org.jetbrains.kotlin.ir.visitors.IrTransformer

internal abstract class IntrinsicTransformer(
    private val intrinsics: Set<IntrospektIntrinsic>
) : IrTransformer<IntrinsicContext>() {
    override fun visitElement(element: IrElement, data: IntrinsicContext): IrElement {
        element.transformChildren(this, data)
        return element
    }

    override fun visitBody(body: IrBody, data: IntrinsicContext): IrBody {
        data.bodyStack.push(body)
        val result = super.visitBody(body, data)
        data.bodyStack.pop()
        return result
    }

    override fun visitClass(
        declaration: IrClass, data: IntrinsicContext
    ): IrStatement {
        data.classStack.push(declaration)
        val result = super.visitClass(declaration, data)
        data.classStack.pop()
        return result
    }

    override fun visitFunction(declaration: IrFunction, data: IntrinsicContext): IrStatement {
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
        val transformedCall = super.visitCall(expression, data)
        if (transformedCall is IrCall) {
            val intrinsicType = transformedCall.target.getIntrinsicType() ?: return transformedCall
            if (intrinsicType !in intrinsics) return transformedCall
            return visitIntrinsic(intrinsicType, expression, data)
        }
        return transformedCall
    }

    abstract fun visitIntrinsic(type: IntrospektIntrinsic, expression: IrCall, context: IntrinsicContext): IrElement
}