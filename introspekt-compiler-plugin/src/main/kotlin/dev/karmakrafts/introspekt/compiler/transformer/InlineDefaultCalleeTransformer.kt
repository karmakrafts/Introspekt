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

import dev.karmakrafts.introspekt.compiler.IntrospektPluginContext
import dev.karmakrafts.introspekt.compiler.util.InlineDefaultMode
import dev.karmakrafts.introspekt.compiler.util.getIntrinsicType
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.util.target
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

internal class InlineDefaultCalleeTransformer(
    private val pluginContext: IntrospektPluginContext
) : IrVisitorVoid() {
    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitFunction(declaration: IrFunction) {
        super.visitFunction(declaration)
        val regularParameters = declaration.parameters.filter { it.kind == IrParameterKind.Regular }
        val inlineModes = ArrayList<InlineDefaultMode>()
        for (parameter in regularParameters) {
            val defaultValue = parameter.defaultValue?.expression
            // If the parameter has no default value, the mode is NONE
            if (defaultValue == null) {
                inlineModes += InlineDefaultMode.None
                continue
            }
            // If the parameter has a default that is not an intrinsic, the mode is also NONE
            val intrinsicType = (defaultValue as? IrCall)?.target?.getIntrinsicType()
            if (intrinsicType == null) {
                inlineModes += InlineDefaultMode.None
                continue
            }
            // Otherwise, we are dealing with an intrinsic
            inlineModes += InlineDefaultMode.Intrinsic(intrinsicType)
            parameter.defaultValue = null // Remove intrinsic from declaration
        }
        // Make sure we have at least one intrinsic default value
        if (!inlineModes.any { it is InlineDefaultMode.Intrinsic }) return
        // Inject the annotation with all inline modes for every parameter
        val annotation = pluginContext.createInlineDefaults(inlineModes)
        pluginContext.metadataDeclarationRegistrar.addMetadataVisibleAnnotationsToElement(declaration, annotation)
    }
}