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

import dev.karmakrafts.introspekt.compiler.element.getTypeInfo
import dev.karmakrafts.introspekt.compiler.util.IntrospektIntrinsic
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.IrCall

internal class TypeInfoTransformer : IntrinsicTransformer(
    setOf(IntrospektIntrinsic.TI_OF)
) {
    private fun emitOf(expression: IrCall, context: IntrinsicContext): IrElement {
        val pluginContext = context.pluginContext
        val moduleFragment = pluginContext.irModule
        val file = pluginContext.irFile
        val source = pluginContext.source
        return requireNotNull(expression.typeArguments.first()) {
            "Missing class type parameter"
        }.getTypeInfo(moduleFragment, file, source).instantiateCached(moduleFragment, file, source, pluginContext)
    }

    override fun visitIntrinsic( // @formatter:off
        type: IntrospektIntrinsic,
        expression: IrCall,
        context: IntrinsicContext
    ): IrElement = when (type) { // @formatter:on
        IntrospektIntrinsic.TI_OF -> emitOf(expression, context)
        else -> error("Unsupported intrinsic for TypeInfoTransformer")
    }
}