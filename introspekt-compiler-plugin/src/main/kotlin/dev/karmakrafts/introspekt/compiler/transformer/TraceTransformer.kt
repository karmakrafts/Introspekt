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

import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.IrTransformer

internal abstract class TraceTransformer : IrTransformer<TraceContext>() {
    open fun visitTraceableFunction(declaration: IrFunction, data: TraceContext) {}

    override fun visitClass(declaration: IrClass, data: TraceContext): IrStatement {
        data.classStack.push(declaration)
        data.pushTraceType(declaration)
        val transformedClass = super.visitClass(declaration, data)
        data.popTraceType()
        data.classStack.pop()
        return transformedClass
    }

    override fun visitFunction(declaration: IrFunction, data: TraceContext): IrStatement {
        data.functionStack.push(declaration)
        data.pushTraceType(declaration)
        val transformedFunction = super.visitFunction(declaration, data)
        if (transformedFunction is IrFunction) {
            visitTraceableFunction(transformedFunction, data)
        }
        data.popTraceType()
        data.functionStack.pop()
        return transformedFunction
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer, data: TraceContext): IrStatement {
        val constructor = data.classOrNull?.primaryConstructor ?: return declaration
        data.pushTraceType(constructor)
        val transformedInitializer = super.visitAnonymousInitializer(declaration, data)
        data.popTraceType()
        return transformedInitializer
    }
}