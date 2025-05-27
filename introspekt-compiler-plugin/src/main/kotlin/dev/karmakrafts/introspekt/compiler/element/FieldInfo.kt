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
import dev.karmakrafts.introspekt.compiler.util.getEnumValue
import dev.karmakrafts.introspekt.compiler.util.getLocation
import dev.karmakrafts.introspekt.compiler.util.getObjectInstance
import dev.karmakrafts.introspekt.compiler.util.getVisibilityName
import dev.karmakrafts.introspekt.compiler.util.toAnnotationMap
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImplWithShape
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.toIrConst

internal class FieldInfo(
    val location: SourceLocation,
    val qualifiedName: String,
    val name: String,
    val type: IrType,
    val visibility: Visibility,
    val isStatic: Boolean,
    val isExternal: Boolean,
    val isFinal: Boolean,
    override val annotations: Map<IrType, List<AnnotationUsageInfo>>
) : ElementInfo, AnnotatedElement {
    override fun instantiateCached( // @formatter:off
        module: IrModuleFragment,
        file: IrFile,
        source: List<String>,
        context: IntrospektPluginContext
    ): IrCall = with(context) { // @formatter:on
        IrCallImplWithShape(
            startOffset = SYNTHETIC_OFFSET,
            endOffset = SYNTHETIC_OFFSET,
            type = fieldInfoType.defaultType,
            symbol = fieldInfoGetOrCreate,
            typeArgumentsCount = 0,
            valueArgumentsCount = 9,
            contextParameterCount = 0,
            hasDispatchReceiver = true,
            hasExtensionReceiver = false
        ).apply { // @formatter:off
            var index = 0
            // location
            putValueArgument(index++, location.instantiateCached(context))
            // qualifiedName
            putValueArgument(index++, qualifiedName.toIrConst(irBuiltIns.stringType))
            // name
            putValueArgument(index++, name.toIrConst(irBuiltIns.stringType))
            // type
            putValueArgument(index++, type.getClass()!!.getClassInfo(module, file, source, context)
                .instantiateCached(module, file, source, context))
            // visibility
            putValueArgument(index++, visibility.getEnumValue(visibilityModifierType) { getVisibilityName() })
            // isStatic
            putValueArgument(index++, isStatic.toIrConst(irBuiltIns.booleanType))
            // isExternal
            putValueArgument(index++, isExternal.toIrConst(irBuiltIns.booleanType))
            // isFinal
            putValueArgument(index++, isFinal.toIrConst(irBuiltIns.booleanType))
            // annotations
            putValueArgument(index, instantiateAnnotations(module, file, source, context))
            dispatchReceiver = fieldInfoCompanionType.getObjectInstance()
        } // @formatter:on
    }
}

internal fun IrField.getFieldInfo( // @formatter:off
    module: IrModuleFragment,
    file: IrFile,
    source: List<String>
): FieldInfo = FieldInfo( // @formatter:on
    location = getLocation(module, file, source),
    qualifiedName = kotlinFqName.asString(),
    name = name.asString(),
    type = type,
    visibility = visibility.delegate,
    isStatic = isStatic,
    isExternal = isExternal,
    isFinal = isFinal,
    annotations = annotations.toAnnotationMap(module, file, source)
)