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

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrStatementContainer
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.IrVisitor

internal abstract class TraceTransformer : IrVisitor<Unit, TraceContext>() {
    override fun visitElement(element: IrElement, data: TraceContext) {
        element.acceptChildren(this, data)
    }

    override fun visitClass(declaration: IrClass, data: TraceContext) {
        data.pushTraceType(declaration)
        super.visitClass(declaration, data)
        data.popTraceType()
    }

    override fun visitFunction(declaration: IrFunction, data: TraceContext) {
        val body = declaration.body
        val hasScope = body is IrBlockBody
        if (hasScope) data.containerStack.push(body as IrStatementContainer)
        data.pushTraceType(declaration)
        super.visitFunction(declaration, data)
        data.popTraceType()
        if (hasScope) data.containerStack.pop()
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer, data: TraceContext) {
        val body = declaration.body
        val constructor = declaration.parentClassOrNull?.primaryConstructor
        val hasConstructor = constructor != null
        data.containerStack.push(body)
        if (hasConstructor) data.pushTraceType(constructor!!)
        super.visitAnonymousInitializer(declaration, data)
        if (hasConstructor) data.popTraceType()
        data.containerStack.pop()
    }
}