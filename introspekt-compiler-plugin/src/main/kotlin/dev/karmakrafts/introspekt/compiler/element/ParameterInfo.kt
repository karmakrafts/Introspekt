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

package dev.karmakrafts.introspekt.compiler.element

import dev.karmakrafts.introspekt.compiler.IntrospektPluginContext
import dev.karmakrafts.introspekt.compiler.util.SourceLocation
import dev.karmakrafts.introspekt.compiler.util.getCleanName
import dev.karmakrafts.introspekt.compiler.util.getLocation
import dev.karmakrafts.introspekt.compiler.util.getObjectInstance
import dev.karmakrafts.introspekt.compiler.util.toAnnotationMap
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImplWithShape
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.toIrConst

internal class ParameterInfo(
    val location: SourceLocation,
    val qualifiedName: String,
    val name: String,
    val type: TypeInfo,
    override val annotations: Map<TypeInfo, List<AnnotationUsageInfo>>
) : ElementInfo, AnnotatedElement {
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun instantiateCached( // @formatter:off
        module: IrModuleFragment,
        file: IrFile,
        source: List<String>,
        context: IntrospektPluginContext
    ): IrCall = with(context) {
        IrCallImplWithShape(
            startOffset = SYNTHETIC_OFFSET,
            endOffset = SYNTHETIC_OFFSET,
            type = introspektSymbols.parameterInfoType.defaultType,
            symbol = introspektSymbols.parameterInfoGetOrCreate,
            typeArgumentsCount = 0,
            valueArgumentsCount = 5,
            contextParameterCount = 0,
            hasDispatchReceiver = true,
            hasExtensionReceiver = false
        ).apply {
            val function = symbol.owner
            arguments[function.parameters.first { it.name.asString() == "location" }] =
                location.instantiateCached(context)
            arguments[function.parameters.first { it.name.asString() == "qualifiedName" }] =
                qualifiedName.toIrConst(irBuiltIns.stringType)
            arguments[function.parameters.first { it.name.asString() == "name" }] =
                name.toIrConst(irBuiltIns.stringType)
            arguments[function.parameters.first { it.name.asString() == "type" }] =
                this@ParameterInfo.type.instantiateCached(module, file, source, context)
            arguments[function.parameters.first { it.name.asString() == "annotations" }] =
                instantiateAnnotations(module, file, source, context)
            dispatchReceiver = introspektSymbols.parameterInfoCompanionType.getObjectInstance()
        }
    }
}

internal fun IrValueParameter.getParameterInfo(
    module: IrModuleFragment,
    file: IrFile,
    source: List<String>
): ParameterInfo = ParameterInfo(
    location = getLocation(module, file, source),
    qualifiedName = "${parent.kotlinFqName.getCleanName()}.${name.asString()}",
    name = name.asString(),
    type = type.getTypeInfo(module, file, source),
    annotations = annotations.toAnnotationMap(module, file, source)
)