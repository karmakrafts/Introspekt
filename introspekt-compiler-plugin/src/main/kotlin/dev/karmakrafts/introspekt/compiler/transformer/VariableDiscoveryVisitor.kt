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
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

internal class VariableDiscoveryVisitor(
    val visitUntil: (IrElement) -> Boolean = { false }
) : IrVisitorVoid() {
    val variables: ArrayList<IrVariable> = ArrayList()
    private var isInScope: Boolean = true

    override fun visitElement(element: IrElement) {
        if (visitUntil(element)) return
        element.acceptChildrenVoid(this)
    }

    override fun visitFunction(declaration: IrFunction) {
        if (visitUntil(declaration)) return
        isInScope = false
        super.visitFunction(declaration)
        isInScope = true
    }

    override fun visitVariable(declaration: IrVariable) {
        if (visitUntil(declaration)) return
        super.visitVariable(declaration)
        if (!isInScope) return
        variables += declaration
    }
}