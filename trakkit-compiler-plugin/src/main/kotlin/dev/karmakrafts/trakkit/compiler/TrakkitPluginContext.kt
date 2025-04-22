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

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImplWithShape
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetEnumValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetObjectValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrEnumEntrySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.starProjectedType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.constructedClassType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isEnumClass
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.isObject
import org.jetbrains.kotlin.ir.util.isVararg
import org.jetbrains.kotlin.ir.util.toIrConst

internal data class TrakkitPluginContext(
    private val pluginContext: IrPluginContext
) : IrPluginContext by pluginContext {
    private val annotationType: IrClassSymbol = pluginContext.referenceClass(TrakkitNames.Kotlin.Annotation.id)!!

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private val listOfFunction: IrSimpleFunctionSymbol = pluginContext.referenceFunctions(TrakkitNames.Kotlin.listOf)
        .find { symbol -> symbol.owner.valueParameters.any { it.isVararg } }!!
    private val listType: IrClassSymbol = pluginContext.referenceClass(TrakkitNames.Kotlin.List.id)!!

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private val mapOfFunction: IrSimpleFunctionSymbol = pluginContext.referenceFunctions(TrakkitNames.Kotlin.mapOf)
        .find { symbol -> symbol.owner.valueParameters.any { it.isVararg } }!!
    private val mapType: IrClassSymbol = pluginContext.referenceClass(TrakkitNames.Kotlin.Map.id)!!

    private val pairConstructor: IrConstructorSymbol =
        pluginContext.referenceConstructors(TrakkitNames.Kotlin.Pair.id).first()
    private val pairType: IrClassSymbol = pluginContext.referenceClass(TrakkitNames.Kotlin.Pair.id)!!

    private val annotationInfoType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(TrakkitNames.AnnotationInfo.id)) {
            "Cannot find AnnotationInfo type, Trakkit runtime library is most likely missing"
        }
    private val annotationInfoConstructor: IrConstructorSymbol =
        pluginContext.referenceConstructors(TrakkitNames.AnnotationInfo.id).first()

    private val sourceLocationType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(TrakkitNames.SourceLocation.id)) {
            "Cannot find SourceLocation type, Trakkit runtime library is most likely missing"
        }
    private val sourceLocationConstructor: IrConstructorSymbol =
        pluginContext.referenceConstructors(TrakkitNames.SourceLocation.id).first()

    private val functionInfoType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(TrakkitNames.FunctionInfo.id)) {
            "Cannot find FunctionInfo type, Trakkit runtime library is most likely missing"
        }
    private val functionInfoConstructor: IrConstructorSymbol =
        pluginContext.referenceConstructors(TrakkitNames.FunctionInfo.id).first()

    private val propertyInfoType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(TrakkitNames.PropertyInfo.id)) {
            "Cannot find PropertyInfo type, Trakkit runtime library is most likely missing"
        }
    private val propertyInfoConstructor: IrConstructorSymbol =
        pluginContext.referenceConstructors(TrakkitNames.PropertyInfo.id).first()

    private val classInfoType: IrClassSymbol = requireNotNull(pluginContext.referenceClass(TrakkitNames.ClassInfo.id)) {
        "Cannot find ClassInfo type, Trakkit runtime library is most likely missing"
    }
    private val classInfoConstructor: IrConstructorSymbol =
        pluginContext.referenceConstructors(TrakkitNames.ClassInfo.id).first()

    private val captureCallerType: IrClassSymbol = pluginContext.referenceClass(TrakkitNames.CaptureCaller.id)!!
    private val captureCallerConstructor: IrConstructorSymbol =
        pluginContext.referenceConstructors(TrakkitNames.CaptureCaller.id).first()

    private val visibilityModifierType: IrClassSymbol =
        pluginContext.referenceClass(TrakkitNames.VisibilityModifier.id)!!
    private val modalityModifierType: IrClassSymbol = pluginContext.referenceClass(TrakkitNames.ModalityModifier.id)!!
    private val classModifierType: IrClassSymbol = pluginContext.referenceClass(TrakkitNames.ClassModifier.id)!!

    private fun TrakkitIntrinsic.getType(): IrType = when (this) {
        TrakkitIntrinsic.SL_HERE, TrakkitIntrinsic.SL_CURRENT_FUNCTION, TrakkitIntrinsic.SL_CURRENT_CLASS -> sourceLocationType.defaultType
        TrakkitIntrinsic.FI_CURRENT -> functionInfoType.defaultType
        TrakkitIntrinsic.CI_CURRENT -> classInfoType.defaultType
        else -> irBuiltIns.intType // Hashsums
    }

    private fun TrakkitIntrinsic.getSymbol(): IrSimpleFunctionSymbol {
        return pluginContext.referenceFunctions(functionId).first()
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    fun TrakkitIntrinsic.createCall(
        startOffset: Int = SYNTHETIC_OFFSET, endOffset: Int = SYNTHETIC_OFFSET
    ): IrCallImpl = IrCallImpl( // @formatter:off
        startOffset = startOffset,
        endOffset = endOffset,
        type = getType(),
        symbol = getSymbol()
    ).apply {
        functionId.classId?.let(pluginContext::referenceClass)?.let { classSymbol ->
            check(classSymbol.owner.isCompanion) { "Intrinsic parent must be a companion object or the top level scope" }
            dispatchReceiver = IrGetObjectValueImpl(
                startOffset = SYNTHETIC_OFFSET,
                endOffset = SYNTHETIC_OFFSET,
                type = classSymbol.defaultType,
                symbol = classSymbol
            )  // @formatter:on
        }
    }

    fun createCaptureCaller(intrinsics: List<String>): IrConstructorCallImpl = IrConstructorCallImpl(
        startOffset = SYNTHETIC_OFFSET,
        endOffset = SYNTHETIC_OFFSET,
        type = captureCallerType.defaultType,
        symbol = captureCallerConstructor,
        typeArgumentsCount = 0,
        constructorTypeArgumentsCount = 0
    ).apply {
        putValueArgument(
            0, IrVarargImpl(
                startOffset = SYNTHETIC_OFFSET,
                endOffset = SYNTHETIC_OFFSET,
                type = irBuiltIns.arrayClass.typeWith(irBuiltIns.stringType),
                varargElementType = irBuiltIns.stringType,
                elements = intrinsics.map { it.toIrValueOrType() })
        )
    }

    private fun Pair<IrExpression?, IrExpression?>.instantiate(
        firstType: IrType, secondType: IrType
    ): IrConstructorCallImpl {
        return IrConstructorCallImpl(
            startOffset = SYNTHETIC_OFFSET,
            endOffset = SYNTHETIC_OFFSET,
            type = pairType.typeWith(firstType, secondType),
            symbol = pairConstructor,
            typeArgumentsCount = 2,
            constructorTypeArgumentsCount = 2
        ).apply {
            putTypeArgument(0, firstType)
            putTypeArgument(1, secondType)
            putValueArgument(0, first ?: null.toIrConst(firstType))
            putValueArgument(1, second ?: null.toIrConst(secondType))
        }
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    fun IrConstructorCall.getAnnotationValues(): Map<String, Any?> {
        val constructor = symbol.owner
        val parameters = constructor.parameters.filter { it.kind == IrParameterKind.Regular }
        if (parameters.isEmpty()) return emptyMap()
        val parameterNames = parameters.map { it.name.asString() }
        check(parameterNames.size == valueArguments.size) { "Missing annotation parameter info" }
        val values = HashMap<String, Any?>()
        val firstParamIndex = parameters.first().indexInOldValueParameters
        val lastParamIndex = firstParamIndex + parameters.size
        for (index in firstParamIndex..<lastParamIndex) {
            val value = valueArguments[index]
            values[parameterNames[index]] = when (value) {
                is IrConst -> value.value
                is IrVararg -> value.elements.map { element ->
                    check(element is IrConst) { "Annotation vararg value must be an IrConst" }
                    element.value
                }.toList()

                else -> error("Unsupported annotation parameter type $value")
            }
        }
        return values
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    fun IrConstructorCall.getAnnotationInfo( // @formatter:off
        module: IrModuleFragment,
        file: IrFile,
        source: List<String>
    ): AnnotationInfo = AnnotationInfo( // @formatter:on
        location = getLocation(module, file, source),
        type = symbol.owner.constructedClassType,
        values = getAnnotationValues()
    )

    private fun createListOf(type: IrType, values: List<IrExpression>): IrCallImpl {
        return IrCallImplWithShape(
            startOffset = SYNTHETIC_OFFSET,
            endOffset = SYNTHETIC_OFFSET,
            type = listType.typeWith(type),
            symbol = listOfFunction,
            typeArgumentsCount = 1,
            valueArgumentsCount = 1,
            contextParameterCount = 0,
            hasDispatchReceiver = false,
            hasExtensionReceiver = false
        ).apply { // @formatter:off
            putTypeArgument(0, type)
            putValueArgument(0, IrVarargImpl(
                startOffset = SYNTHETIC_OFFSET,
                endOffset = SYNTHETIC_OFFSET,
                type = irBuiltIns.arrayClass.typeWith(type),
                varargElementType = type,
                elements = values
            ))
        } // @formatter:on
    }

    private fun createMapOf( // @formatter:off
        keyType: IrType,
        valueType: IrType,
        values: List<Pair<IrExpression?, IrExpression?>>
    ): IrCallImpl { // @formatter:on
        return IrCallImplWithShape(
            startOffset = SYNTHETIC_OFFSET,
            endOffset = SYNTHETIC_OFFSET,
            type = mapType.typeWith(keyType, valueType),
            symbol = mapOfFunction,
            typeArgumentsCount = 2,
            valueArgumentsCount = 1,
            contextParameterCount = 0,
            hasDispatchReceiver = false,
            hasExtensionReceiver = false
        ).apply { // @formatter:off
            putTypeArgument(0, keyType)
            putTypeArgument(1, valueType)
            val pairType = pairType.typeWith(keyType, valueType)
            putValueArgument(0, IrVarargImpl(
                startOffset = SYNTHETIC_OFFSET,
                endOffset = SYNTHETIC_OFFSET,
                type = irBuiltIns.arrayClass.typeWith(pairType),
                varargElementType = pairType,
                elements = values.map { it.instantiate(keyType, valueType) }
            ))
        } // @formatter:on
    }

    private fun Any?.getConstType(): IrType = when (this) {
        is Byte -> irBuiltIns.byteType
        is Short -> irBuiltIns.shortType
        is Int -> irBuiltIns.intType
        is Long -> irBuiltIns.longType
        is Float -> irBuiltIns.floatType
        is Double -> irBuiltIns.doubleType
        is String -> irBuiltIns.stringType
        is Char -> irBuiltIns.charType
        is Boolean -> irBuiltIns.booleanType
        is IrType -> irBuiltIns.kClassClass.starProjectedType
        is Array<*>, is List<*> -> irBuiltIns.arrayClass.starProjectedType
        null -> irBuiltIns.anyType
        else -> error("Unsupported IrConst type ${this@getConstType::class}")
    }

    private fun Any?.toIrValueOrType(): IrExpression = when (this@toIrValueOrType) {
        is IrType -> this@toIrValueOrType.toClassReference()
        is List<*> -> createListOf(irBuiltIns.anyType, this@toIrValueOrType.map { it.toIrValueOrType() })
        // TODO: create actual arrays here?
        is Array<*> -> createListOf(irBuiltIns.anyType, this@toIrValueOrType.map { it.toIrValueOrType() })
        else -> toIrConst(this@toIrValueOrType.getConstType())
    }

    private fun AnnotationInfo.instantiate(): IrConstructorCallImpl {
        return IrConstructorCallImpl(
            startOffset = SYNTHETIC_OFFSET,
            endOffset = SYNTHETIC_OFFSET,
            type = annotationInfoType.defaultType,
            symbol = annotationInfoConstructor,
            typeArgumentsCount = 0,
            constructorTypeArgumentsCount = 0
        ).apply { // @formatter:off
            var index = 0
            putValueArgument(index++, location.instantiate())
            putValueArgument(index++, this@instantiate.type.toClassReference())
            putValueArgument(index, createMapOf(
                keyType = irBuiltIns.stringType,
                valueType = irBuiltIns.anyType,
                values = values.map { (key, value) ->
                    key.toIrConst(irBuiltIns.stringType) to value.toIrValueOrType()
                }
            ))
        } // @formatter:on
    }

    fun SourceLocation.instantiate(): IrConstructorCallImpl {
        return IrConstructorCallImpl(
            startOffset = SYNTHETIC_OFFSET,
            endOffset = SYNTHETIC_OFFSET,
            type = sourceLocationType.defaultType,
            symbol = sourceLocationConstructor,
            typeArgumentsCount = 0,
            constructorTypeArgumentsCount = 0
        ).apply {
            var index = 0
            putValueArgument(index++, module.toIrConst(irBuiltIns.stringType))
            putValueArgument(index++, file.toIrConst(irBuiltIns.stringType))
            putValueArgument(index++, line.toIrConst(irBuiltIns.intType))
            putValueArgument(index, column.toIrConst(irBuiltIns.intType))
        }
    }

    fun SourceLocation.createHashSum(): IrConst {
        return hashCode().toIrConst(irBuiltIns.intType)
    }

    private fun IrType.toClassReference(): IrClassReferenceImpl {
        return IrClassReferenceImpl(
            startOffset = SYNTHETIC_OFFSET,
            endOffset = SYNTHETIC_OFFSET,
            type = this,
            symbol = classOrFail,
            classType = this
        )
    }

    private fun List<IrConstructorCall>.toAnnotationMap( // @formatter:off
        module: IrModuleFragment,
        file: IrFile,
        source: List<String>
    ): HashMap<IrType, AnnotationInfo> { // @formatter:on
        val annotations = HashMap<IrType, AnnotationInfo>()
        for (call in this) {
            annotations[call.type] = call.getAnnotationInfo(module, file, source)
        }
        return annotations
    }

    fun IrFunction.getFunctionInfo( // @formatter:off
        module: IrModuleFragment,
        file: IrFile,
        source: List<String>,
    ): FunctionInfo = FunctionInfo( // @formatter:on
        location = getFunctionLocation(module, file, source),
        name = name.asString(),
        typeParameterNames = typeParameters.map { it.name.asString() },
        returnType = returnType,
        parameterTypes = valueParameters.filter { it.kind == IrParameterKind.Regular }.map { it.type },
        parameterNames = valueParameters.filter { it.kind == IrParameterKind.Regular }.map { it.name.asString() },
        annotations = annotations.toAnnotationMap(module, file, source)
    )

    fun FunctionInfo.instantiate(): IrConstructorCallImpl { // @formatter:on
        return IrConstructorCallImpl(
            startOffset = SYNTHETIC_OFFSET,
            endOffset = SYNTHETIC_OFFSET,
            type = functionInfoType.defaultType,
            symbol = functionInfoConstructor,
            typeArgumentsCount = 0,
            constructorTypeArgumentsCount = 0
        ).apply { // @formatter:off
            var index = 0
            // location
            putValueArgument(index++, location.instantiate())
            // name
            putValueArgument(index++, name.toIrConst(irBuiltIns.stringType))
            // typeParameterNames
            putValueArgument(index++, createListOf(
                type = irBuiltIns.stringType,
                values = typeParameterNames.map { it.toIrConst(irBuiltIns.stringType) }
            ))
            // returnType
            putValueArgument(index++, returnType.toClassReference())
            // parameterTypes
            putValueArgument(index++, createListOf(
                type = irBuiltIns.kClassClass.starProjectedType,
                values = parameterTypes.map { it.type.toIrValueOrType() }
            ))
            // parameterNames
            putValueArgument(index++, createListOf(
                type = irBuiltIns.stringType,
                values = parameterNames.map { it.toIrConst(irBuiltIns.stringType) }
            ))
            // annotations
            putValueArgument(index, createMapOf(
                keyType = irBuiltIns.kClassClass.typeWith(annotationType.defaultType),
                valueType = annotationInfoType.defaultType,
                values = annotations.map { (type, info) ->
                    type.toIrValueOrType() to info.instantiate()
                }
            ))
        } // @formatter:on
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrClassSymbol.getEnumConstant(name: String): IrEnumEntrySymbol {
        return requireNotNull(
            defaultType.classOrFail.owner.declarations.filterIsInstance<IrEnumEntry>()
                .find { it.name.asString() == name }) { "No entry $name in $this" }.symbol
    }

    private inline fun <T> T.getEnumValue(type: IrClassSymbol, mapper: T.() -> String): IrGetEnumValueImpl {
        return IrGetEnumValueImpl(
            startOffset = SYNTHETIC_OFFSET,
            endOffset = SYNTHETIC_OFFSET,
            type = type.defaultType,
            symbol = type.getEnumConstant(this.mapper())
        )
    }

    private fun Visibility.getVisibilityName(): String = when (this) {
        Visibilities.Public -> "PUBLIC"
        Visibilities.Protected -> "PROTECTED"
        Visibilities.Internal -> "INTERNAL"
        else -> "PRIVATE"
    }

    private fun Modality.getModalityName(): String = when (this) {
        Modality.OPEN -> "OPEN"
        Modality.SEALED -> "SEALED"
        Modality.ABSTRACT -> "ABSTRACT"
        else -> "FINAL"
    }

    private fun PropertyInfo.instantiate(): IrConstructorCallImpl = IrConstructorCallImpl(
        startOffset = SYNTHETIC_OFFSET,
        endOffset = SYNTHETIC_OFFSET,
        type = propertyInfoType.defaultType,
        symbol = propertyInfoConstructor,
        typeArgumentsCount = 0,
        constructorTypeArgumentsCount = 0
    ).apply { // @formatter:off
        var index = 0
        // location
        putValueArgument(index++, location.instantiate())
        // name
        putValueArgument(index++, name.toIrConst(irBuiltIns.stringType))
        // type
        putValueArgument(index++, type.toClassReference())
        // isMutable
        putValueArgument(index++, isMutable.toIrConst(irBuiltIns.booleanType))
        // visibility
        putValueArgument(index++, visibility.getEnumValue(visibilityModifierType) { getVisibilityName() })
        // modality
        putValueArgument(index, modality.getEnumValue(modalityModifierType) { getModalityName() })
    } // @formatter:on

    private fun IrClass.getClassModifier(): ClassModifier? = when {
        isData -> ClassModifier.DATA
        isValue -> ClassModifier.VALUE
        isEnumClass -> ClassModifier.ENUM
        else -> null
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrClass.getCompanionObjects(
        module: IrModuleFragment, file: IrFile, source: List<String>
    ): List<ClassInfo> {
        return declarations.filterIsInstance<IrClass>()
            .filter { it.isCompanion }
            .map { it.getClassInfo(module, file, source) }
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    fun IrClass.getClassInfo( // @formatter:off
        module: IrModuleFragment,
        file: IrFile,
        source: List<String>
    ): ClassInfo = ClassInfo(
        location = getLocation(module, file, source),
        type = symbol.defaultType,
        typeParameterNames = typeParameters.map { it.name.asString() },
        annotations = annotations.toAnnotationMap(module, file, source),
        functions = functions.map { it.getFunctionInfo(module, file, source) }.toList(),
        companionObjects = getCompanionObjects(module, file, source),
        isInterface = isInterface,
        isObject = isObject,
        isCompanionObject = isCompanion,
        visibility = visibility.delegate,
        modality = modality,
        classType = getClassModifier()
    ) // @formatter:on

    fun ClassInfo.instantiate(): IrConstructorCallImpl = IrConstructorCallImpl(
        startOffset = SYNTHETIC_OFFSET,
        endOffset = SYNTHETIC_OFFSET,
        type = classInfoType.defaultType,
        symbol = classInfoConstructor,
        typeArgumentsCount = 0,
        constructorTypeArgumentsCount = 0
    ).apply { // @formatter:off
        var index = 0
        // location
        putValueArgument(index++, location.instantiate())
        // type
        putValueArgument(index++, this@instantiate.type.toClassReference())
        // typeParameterNames
        putValueArgument(index++, createListOf(
            type = irBuiltIns.stringType,
            values = typeParameterNames.map { it.toIrConst(irBuiltIns.stringType) })
        )
        // annotations
        putValueArgument(index++, createMapOf(
            keyType = irBuiltIns.kClassClass.typeWith(annotationType.defaultType),
            valueType = annotationInfoType.defaultType,
            values = annotations.map { (type, info) ->
                type.toIrValueOrType() to info.instantiate()
            })
        )
        // functions
        putValueArgument(index++, createListOf(
            type = functionInfoType.defaultType,
            values = functions.map { it.instantiate() })
        )
        // properties
        putValueArgument(index++, createListOf(
            type = propertyInfoType.defaultType,
            values = emptyList() // TODO: implement this
        ))
        // companionObjects
        putValueArgument(index++, createListOf(
            type = classInfoType.defaultType,
            values = companionObjects.map { it.instantiate() })
        )
        // isInterface
        putValueArgument(index++, isInterface.toIrConst(irBuiltIns.booleanType))
        // isObject
        putValueArgument(index++, isObject.toIrConst(irBuiltIns.booleanType))
        // isCompanionObject
        putValueArgument(index++, isCompanionObject.toIrConst(irBuiltIns.booleanType))
        // visibility
        putValueArgument(index++, visibility.getEnumValue(visibilityModifierType) { getVisibilityName() })
        // modality
        putValueArgument(index++, modality.getEnumValue(modalityModifierType) { getModalityName() })
        // classModifier
        putValueArgument(index, classType?.getEnumValue(classModifierType, ClassModifier::name)
            ?: null.toIrConst(classModifierType.defaultType))
    } // @formatter:on
}