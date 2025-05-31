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
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImplWithShape
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.toIrConst

internal data class PropertyInfo(
    val location: SourceLocation,
    val qualifiedName: String,
    val name: String,
    val isMutable: Boolean,
    val visibility: Visibility,
    val modality: Modality,
    val isExpect: Boolean,
    val isDelegated: Boolean,
    val backingField: FieldInfo?,
    val getter: FunctionInfo?,
    val setter: FunctionInfo?,
    override var annotations: Map<TypeInfo, List<AnnotationUsageInfo>> = emptyMap()
) : ElementInfo, AnnotatedElement {
    companion object {
        private val cache: HashMap<String, PropertyInfo> = HashMap()

        inline fun getOrCreate(
            location: SourceLocation,
            qualifiedName: String,
            name: String,
            isMutable: Boolean,
            visibility: Visibility,
            modality: Modality,
            isExpect: Boolean,
            isDelegated: Boolean,
            backingField: FieldInfo?,
            getter: FunctionInfo?,
            setter: FunctionInfo?,
            createCallback: PropertyInfo.() -> Unit = {}
        ): PropertyInfo = cache.getOrPut(qualifiedName) {
            PropertyInfo(
                location = location,
                qualifiedName = qualifiedName,
                name = name,
                isMutable = isMutable,
                visibility = visibility,
                modality = modality,
                isExpect = isExpect,
                isDelegated = isDelegated,
                backingField = backingField,
                getter = getter,
                setter = setter
            ).apply(createCallback)
        }
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun instantiateCached( // @formatter:off
        module: IrModuleFragment,
        file: IrFile,
        source: List<String>,
        context: IntrospektPluginContext
    ): IrCall = with(context) { // @formatter:on
        IrCallImplWithShape(
            startOffset = SYNTHETIC_OFFSET,
            endOffset = SYNTHETIC_OFFSET,
            type = propertyInfoType.defaultType,
            symbol = propertyInfoGetOrCreate,
            typeArgumentsCount = 0,
            valueArgumentsCount = 12,
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
            arguments[function.parameters.first { it.name.asString() == "isMutable" }] =
                isMutable.toIrConst(irBuiltIns.booleanType)
            arguments[function.parameters.first { it.name.asString() == "visibility" }] =
                visibility.getEnumValue(visibilityModifierType) { getVisibilityName() }
            arguments[function.parameters.first { it.name.asString() == "modality" }] =
                modality.getEnumValue(modalityModifierType) { name }
            arguments[function.parameters.first { it.name.asString() == "isExpect" }] =
                isExpect.toIrConst(irBuiltIns.booleanType)
            arguments[function.parameters.first { it.name.asString() == "isDelegated" }] =
                isDelegated.toIrConst(irBuiltIns.booleanType)
            arguments[function.parameters.first { it.name.asString() == "backingField" }] =
                backingField?.instantiateCached(module, file, source, context)
                    ?: null.toIrConst(fieldInfoType.defaultType)
            arguments[function.parameters.first { it.name.asString() == "getter" }] =
                getter?.instantiateCached(module, file, source, context) ?: null.toIrConst(functionInfoType.defaultType)
            arguments[function.parameters.first { it.name.asString() == "setter" }] =
                setter?.instantiateCached(module, file, source, context) ?: null.toIrConst(functionInfoType.defaultType)
            arguments[function.parameters.first { it.name.asString() == "annotations" }] =
                instantiateAnnotations(module, file, source, context)
            dispatchReceiver = propertyInfoCompanionType.getObjectInstance()
        }
    }

    // Modifiers are not relevant to identity hash
    override fun hashCode(): Int = qualifiedName.hashCode()

    // Modifiers are not relevant to value equality
    override fun equals(other: Any?): Boolean {
        return if (other !is PropertyInfo) false
        else qualifiedName == other.qualifiedName
    }
}

internal fun IrProperty.getPropertyInfo( // @formatter:off
    module: IrModuleFragment,
    file: IrFile,
    source: List<String>
): PropertyInfo = PropertyInfo.getOrCreate(
    location = getLocation(module, file, source),
    qualifiedName = requireNotNull(fqNameWhenAvailable) {
        "Could not obtain fully qualified name of property ${this@getPropertyInfo}"
    }.asString(),
    name = name.asString(),
    isMutable = isVar,
    visibility = visibility.delegate,
    modality = modality,
    isExpect = isExpect,
    isDelegated = isDelegated,
    backingField = backingField?.getFieldInfo(module, file, source),
    getter = getter?.getFunctionInfo(module, file, source),
    setter = setter?.getFunctionInfo(module, file, source)
) // @formatter:on