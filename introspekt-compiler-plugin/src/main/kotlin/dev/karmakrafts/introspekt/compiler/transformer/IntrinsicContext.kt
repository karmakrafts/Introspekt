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
import dev.karmakrafts.introspekt.compiler.element.FunctionInfo
import dev.karmakrafts.introspekt.compiler.element.getFunctionInfo
import dev.karmakrafts.introspekt.compiler.util.SourceLocation
import dev.karmakrafts.introspekt.compiler.util.getFunctionLocation
import dev.karmakrafts.introspekt.compiler.util.getLocation
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrBody
import java.util.*

internal data class IntrinsicContext(val pluginContext: IntrospektPluginContext) {
    val classStack: Stack<IrClass> = Stack()
    val functionStack: Stack<IrFunction> = Stack()
    val initializerStack: Stack<IrAnonymousInitializer> = Stack()
    val bodyStack: Stack<IrBody> = Stack()

    inline val `class`: IrClass
        get() = requireNotNull(classStack.firstOrNull()) { "Not inside any class" }

    private inline val functionOrNull: IrFunction?
        get() = functionStack.firstOrNull()

    private inline val initializerOrNull: IrAnonymousInitializer?
        get() = initializerStack.firstOrNull()

    fun getFunctionLocation( // @formatter:off
        module: IrModuleFragment,
        file: IrFile,
        source: List<String>
    ): SourceLocation {
        return functionOrNull?.getFunctionLocation(module, file, source)
            ?: initializerOrNull?.getLocation(module, file, source)
            ?: SourceLocation.undefined
    } // @formatter:on

    fun getFunctionInfo( // @formatter:off
        module: IrModuleFragment,
        file: IrFile,
        source: List<String>
    ): FunctionInfo? {
        return functionOrNull?.getFunctionInfo(module, file, source)
            ?: initializerOrNull?.getFunctionInfo(module, file, source)
    } // @formatter:on
}