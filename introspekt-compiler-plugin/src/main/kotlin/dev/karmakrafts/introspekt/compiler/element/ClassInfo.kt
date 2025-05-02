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
import dev.karmakrafts.introspekt.compiler.util.ClassModifier
import dev.karmakrafts.introspekt.compiler.util.SourceLocation
import dev.karmakrafts.introspekt.compiler.util.getClassModifier
import dev.karmakrafts.introspekt.compiler.util.getCompanionObjects
import dev.karmakrafts.introspekt.compiler.util.getEnumValue
import dev.karmakrafts.introspekt.compiler.util.getLocation
import dev.karmakrafts.introspekt.compiler.util.getModalityName
import dev.karmakrafts.introspekt.compiler.util.getObjectInstance
import dev.karmakrafts.introspekt.compiler.util.getVisibilityName
import dev.karmakrafts.introspekt.compiler.util.toAnnotationMap
import dev.karmakrafts.introspekt.compiler.util.toClassReference
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImplWithShape
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.isObject
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.util.toIrConst

internal data class ClassInfo(
    val location: SourceLocation,
    val type: IrType,
    val typeParameterNames: List<String>,
    val companionObjects: List<ClassInfo>,
    val superTypes: List<ClassInfo>,
    val isInterface: Boolean,
    val isObject: Boolean,
    val isCompanionObject: Boolean,
    val isExpect: Boolean,
    val visibility: Visibility,
    val modality: Modality,
    val classType: ClassModifier?,
    var functions: List<FunctionInfo> = emptyList(),
    var properties: List<PropertyInfo> = emptyList(),
    override var annotations: Map<IrType, List<AnnotationUsageInfo>> = emptyMap()
) : ElementInfo, AnnotatedElement {
    companion object {
        private val cache: HashMap<IrType, ClassInfo> = HashMap()

        fun getOrCreate(
            location: SourceLocation,
            type: IrType,
            typeParameterNames: List<String>,
            companionObjects: List<ClassInfo>,
            superTypes: List<ClassInfo>,
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
        get() = type.classFqName?.shortName()?.asString() ?: "n/a"

    inline val qualifiedName: String
        get() = type.classFqName?.asString() ?: "n/a"

    override fun instantiateCached(context: IntrospektPluginContext): IrCall = with(context) {
        return IrCallImplWithShape(
            startOffset = SYNTHETIC_OFFSET,
            endOffset = SYNTHETIC_OFFSET,
            type = classInfoType.defaultType,
            symbol = classInfoGetOrCreate,
            typeArgumentsCount = 0,
            valueArgumentsCount = 17,
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
            putValueArgument(index++, this@ClassInfo.type.toClassReference(context))
            // typeParameterNames
            putValueArgument(index++, createListOf(
                type = irBuiltIns.stringType,
                values = typeParameterNames.map { it.toIrConst(irBuiltIns.stringType) })
            )
            // annotations
            putValueArgument(index++, instantiateAnnotations(context))
            // functions
            putValueArgument(index++, createListOf(
                type = functionInfoType.defaultType,
                values = functions.map { it.instantiateCached(context) })
            )
            // properties
            putValueArgument(index++, createListOf(
                type = propertyInfoType.defaultType,
                values = properties.map { it.instantiateCached(context) }
            ))
            // companionObjects
            putValueArgument(index++, createListOf(
                type = classInfoType.defaultType,
                values = companionObjects.map { it.instantiateCached(context) })
            )
            // superTypes
            putValueArgument(index++, createListOf(
                type = classInfoType.defaultType,
                values = superTypes.map { it.instantiateCached(context) }
            ))
            // isInterface
            putValueArgument(index++, isInterface.toIrConst(irBuiltIns.booleanType))
            // isObject
            putValueArgument(index++, isObject.toIrConst(irBuiltIns.booleanType))
            // isCompanionObject
            putValueArgument(index++, isCompanionObject.toIrConst(irBuiltIns.booleanType))
            // isExpect
            putValueArgument(index++, isExpect.toIrConst(irBuiltIns.booleanType))
            // visibility
            putValueArgument(index++, visibility.getEnumValue(visibilityModifierType) { getVisibilityName() })
            // modality
            putValueArgument(index++, modality.getEnumValue(modalityModifierType) { getModalityName() })
            // classModifier
            putValueArgument(index, classType?.getEnumValue(classModifierType, ClassModifier::name)
                ?: null.toIrConst(classModifierType.defaultType))
            dispatchReceiver = classInfoCompanionType.getObjectInstance()
        } // @formatter:on
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
    type = symbol.defaultType,
    typeParameterNames = typeParameters.map { it.name.asString() },
    companionObjects = getCompanionObjects().map { it.getClassInfo(module, file, source, context) },
    superTypes = superTypes
        .filter { it.classOrNull != null }
        .mapNotNull { it.getClass()?.getClassInfo(module, file, source, context) },
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