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
import org.jetbrains.kotlin.ir.visitors.IrVisitor
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import java.util.*

private class TraceFilterTransformer : IrVisitorVoid() {

}

internal class TraceTransformer : IrVisitor<Unit, Stack<Int>>() {
    override fun visitElement(element: IrElement, data: Stack<Int>) {
        element.acceptChildren(this, data)
    }

    override fun visitClass(declaration: IrClass, data: Stack<Int>) {
        super.visitClass(declaration, data)
    }

    override fun visitFunction(declaration: IrFunction, data: Stack<Int>) {
        super.visitFunction(declaration, data)
    }

    override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer, data: Stack<Int>) {
        super.visitAnonymousInitializer(declaration, data)
    }
}