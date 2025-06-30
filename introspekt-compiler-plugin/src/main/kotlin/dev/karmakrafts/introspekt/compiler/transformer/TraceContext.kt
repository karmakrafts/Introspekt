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

import dev.karmakrafts.introspekt.compiler.IntrospektPluginContext
import dev.karmakrafts.introspekt.compiler.util.TraceType
import dev.karmakrafts.introspekt.compiler.util.getTraceType
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.symbols.IrReturnTargetSymbol
import java.util.*

internal class TraceContext(
    val pluginContext: IntrospektPluginContext
) {
    private val traceTypeStack: Stack<List<TraceType>> = Stack()
    val functionStack: Stack<IrFunction> = Stack()
    val classStack: Stack<IrClass> = Stack()
    val returnTargetStack: Stack<IrReturnTargetSymbol> = Stack()
    var isFunctionReference: Boolean = false

    inline val returnTargetOrNull: IrReturnTargetSymbol?
        get() = returnTargetStack.lastOrNull()

    inline val traceType: List<TraceType>?
        get() = traceTypeStack.lastOrNull()

    inline val classOrNull: IrClass?
        get() = classStack.lastOrNull()

    inline val declParentOrNull: IrDeclarationParent?
        get() = functionStack.lastOrNull() ?: classStack.lastOrNull() ?: pluginContext.irFile

    fun pushTraceType(declaration: IrAnnotationContainer) {
        traceTypeStack.push(declaration.getTraceType())
    }

    fun popTraceType() = traceTypeStack.pop()
}