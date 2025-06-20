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
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrClass
import java.util.*

internal class TraceContext(
    private val pluginContext: IntrospektPluginContext
) {
    private val traceTypeStack: Stack<List<TraceType>> = Stack()
    internal val classStack: Stack<IrClass> = Stack()
    internal val containerStack: Stack<IrElement> = Stack()

    inline val traceType: List<TraceType>
        get() = traceTypeStack.firstOrNull() ?: emptyList()

    inline val container: IrElement
        get() = containerStack.first()

    inline val containerOrNull: IrElement?
        get() = containerStack.firstOrNull()

    inline val classOrNull: IrClass?
        get() = classStack.firstOrNull()

    fun pushTraceType(declaration: IrAnnotationContainer) {
        traceTypeStack.push(declaration.getTraceType())
    }

    fun popTraceType() = traceTypeStack.pop()
}