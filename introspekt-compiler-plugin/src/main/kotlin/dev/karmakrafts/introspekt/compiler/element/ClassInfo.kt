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

package dev.karmakrafts.introspekt.compiler.element

import dev.karmakrafts.introspekt.compiler.IntrospektPluginContext
import dev.karmakrafts.introspekt.compiler.util.ClassModifier
import dev.karmakrafts.introspekt.compiler.util.SourceLocation
import dev.karmakrafts.introspekt.compiler.util.getClassModifier
import dev.karmakrafts.introspekt.compiler.util.getCompanionObjects
import dev.karmakrafts.introspekt.compiler.util.getEnumValue
import dev.karmakrafts.introspekt.compiler.util.getLocation
import dev.karmakrafts.introspekt.compiler.util.getObjectInstance
import dev.karmakrafts.introspekt.compiler.util.getVisibilityName
import dev.karmakrafts.introspekt.compiler.util.toAnnotationMap
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImplWithShape
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.isObject
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.util.toIrConst

internal data class ClassInfo(
    val location: SourceLocation,
    val type: TypeInfo,
    val typeParameterNames: List<String>,
    val companionObjects: List<ClassInfo>,
    val superTypes: List<TypeInfo>,
    val isInterface: Boolean,
    val isObject: Boolean,
    val isCompanionObject: Boolean,
    val isExpect: Boolean,
    val visibility: Visibility,
    val modality: Modality,
    val classType: ClassModifier?,
    var functions: List<FunctionInfo> = emptyList(),
    var properties: List<PropertyInfo> = emptyList(),
    override var annotations: Map<TypeInfo, List<AnnotationUsageInfo>> = emptyMap()
) : ElementInfo, AnnotatedElement {
    companion object {
        private val cache: HashMap<TypeInfo, ClassInfo> = HashMap()

        fun getOrCreate(
            location: SourceLocation,
            type: TypeInfo,
            typeParameterNames: List<String>,
            companionObjects: List<ClassInfo>,
            superTypes: List<TypeInfo>,
            isInterface: Boolean,
            isObject: Boolean,
            isCompanionObject: Boolean,
            isExpect: Boolean,
            visibility: Visibility,
            modality: Modality,
            classType: ClassModifier?,
            createCallback: ClassInfo.() -> Unit = {}
        ): ClassInfo = cache.getOrPut(type) {
            ClassInfo(
                location = location,
                type = type,
                typeParameterNames = typeParameterNames,
                companionObjects = companionObjects,
                superTypes = superTypes,
                isInterface = isInterface,
                isObject = isObject,
                isCompanionObject = isCompanionObject,
                isExpect = isExpect,
                visibility = visibility,
                modality = modality,
                classType = classType
            ).apply(createCallback)
        }
    }

    inline val name: String
        get() = type.type.classFqName?.shortName()?.asString() ?: "n/a"

    inline val qualifiedName: String
        get() = type.type.classFqName?.asString() ?: "n/a"

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
            type = introspektSymbols.classInfoType.defaultType,
            symbol = introspektSymbols.classInfoGetOrCreate,
            typeArgumentsCount = 0,
            valueArgumentsCount = 14,
            contextParameterCount = 0,
            hasDispatchReceiver = true,
            hasExtensionReceiver = false
        ).apply {
            val function = symbol.owner
            arguments[function.parameters.first { it.name.asString() == "type" }] =
                this@ClassInfo.type.instantiateCached(module, file, source, context)
            arguments[function.parameters.first { it.name.asString() == "typeParameterNames" }] =
                createListOf(irBuiltIns.stringType, typeParameterNames.map { it.toIrConst(irBuiltIns.stringType) })
            arguments[function.parameters.first { it.name.asString() == "annotations" }] =
                instantiateAnnotations(module, file, source, context)
            arguments[function.parameters.first { it.name.asString() == "functions" }] = createListOf(
                introspektSymbols.functionInfoType.defaultType, functions.map { it.instantiateCached(module, file, source, context) })
            arguments[function.parameters.first { it.name.asString() == "properties" }] = createListOf(
                introspektSymbols.propertyInfoType.defaultType, properties.map { it.instantiateCached(module, file, source, context) })
            arguments[function.parameters.first { it.name.asString() == "companionObjects" }] = createListOf(
                introspektSymbols.classInfoType.defaultType, companionObjects.map { it.instantiateCached(module, file, source, context) })
            arguments[function.parameters.first { it.name.asString() == "superTypes" }] = createListOf(
                introspektSymbols.typeInfoType.defaultType, superTypes.map { it.instantiateCached(module, file, source, context) })
            arguments[function.parameters.first { it.name.asString() == "isInterface" }] =
                isInterface.toIrConst(irBuiltIns.booleanType)
            arguments[function.parameters.first { it.name.asString() == "isObject" }] =
                isObject.toIrConst(irBuiltIns.booleanType)
            arguments[function.parameters.first { it.name.asString() == "isCompanionObject" }] =
                isCompanionObject.toIrConst(irBuiltIns.booleanType)
            arguments[function.parameters.first { it.name.asString() == "isExpect" }] =
                isExpect.toIrConst(irBuiltIns.booleanType)
            arguments[function.parameters.first { it.name.asString() == "visibility" }] =
                visibility.getEnumValue(introspektSymbols.visibilityModifierType) { getVisibilityName() }
            arguments[function.parameters.first { it.name.asString() == "modality" }] =
                modality.getEnumValue(introspektSymbols.modalityModifierType) { name }
            arguments[function.parameters.first { it.name.asString() == "classModifier" }] =
                classType?.getEnumValue(introspektSymbols.classModifierType, ClassModifier::name)
                    ?: null.toIrConst(introspektSymbols.classModifierType.defaultType)
            dispatchReceiver = introspektSymbols.classInfoCompanionType.getObjectInstance()
        }
    }

    // Duplicated class properties are not relevant to identity hash
    override fun hashCode(): Int = type.hashCode()

    // Duplicated class properties are not relevant to value equality
    override fun equals(other: Any?): Boolean {
        return if (other !is ClassInfo) false
        else type === other.type // Use type identity
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal fun IrClass.getClassInfo( // @formatter:off
    module: IrModuleFragment,
    file: IrFile,
    source: List<String>,
    context: IntrospektPluginContext
): ClassInfo = ClassInfo.getOrCreate(
    location = getLocation(module, file, source),
    type = symbol.defaultType.getTypeInfo(module, file, source),
    typeParameterNames = typeParameters.map { it.name.asString() },
    companionObjects = getCompanionObjects().map { it.getClassInfo(module, file, source, context) },
    superTypes = superTypes
        .filter { it.classOrNull != null }
        .map { it.getTypeInfo(module, file, source) },
    isInterface = isInterface,
    isObject = isObject,
    isCompanionObject = isCompanion,
    isExpect = isExpect,
    visibility = visibility.delegate,
    modality = modality,
    classType = getClassModifier()
) {
    functions = this@getClassInfo.functions.map { it.getFunctionInfo(module, file, source) }.toList()
    properties = this@getClassInfo.properties.map { it.getPropertyInfo(module, file, source) }.toList()
    annotations = this@getClassInfo.annotations.toAnnotationMap(module, file, source)
}