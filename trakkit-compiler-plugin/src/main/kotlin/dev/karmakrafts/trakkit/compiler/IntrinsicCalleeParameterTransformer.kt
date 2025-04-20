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
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.util.target
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

/**
 * Invoked before the main intrinsic passes to de-default all intrinsic method default
 * parameters and move their default value to the call-site if it matches one of the
 * registered intrinsics to allow caller-tracing (like with std::source_location in C++).
 */
internal class IntrinsicCalleeParameterTransformer(
    val pluginContext: TrakkitPluginContext
) : IrVisitorVoid() {
    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitFunction(declaration: IrFunction) {
        super.visitFunction(declaration)
        val callsiteIntrinsics = ArrayList<Pair<Int, String>>()
        for (parameter in declaration.valueParameters) {
            val defaultBody = parameter.defaultValue ?: continue
            var shouldRemoveDefault = false
            for (statement in defaultBody.statements) {
                if (statement !is IrCall) continue
                val function = statement.target
                val intrinsicType = function.getIntrinsicType() ?: continue
                shouldRemoveDefault = true
                callsiteIntrinsics += Pair(parameter.indexInParameters, intrinsicType.name.lowercase())
            }
            if (!shouldRemoveDefault) continue
            parameter.defaultValue = null
        }
        // Inject @CaptureCaller marker so we can reconstruct intrinsics @ the callsite(s) in index:name format
        declaration.annotations += pluginContext.createCaptureCaller(callsiteIntrinsics.map { (index, name) ->
            "$index:$name"
        })
    }
}