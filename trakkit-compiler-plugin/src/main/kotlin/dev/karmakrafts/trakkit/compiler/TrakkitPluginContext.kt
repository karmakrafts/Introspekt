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

import dev.karmakrafts.trakkit.compiler.element.AnnotationUsageInfo
import dev.karmakrafts.trakkit.compiler.element.ClassInfo
import dev.karmakrafts.trakkit.compiler.element.FunctionInfo
import dev.karmakrafts.trakkit.compiler.element.PropertyInfo
import dev.karmakrafts.trakkit.compiler.util.ClassModifier
import dev.karmakrafts.trakkit.compiler.util.IntrinsicResultType
import dev.karmakrafts.trakkit.compiler.util.SourceLocation
import dev.karmakrafts.trakkit.compiler.util.TrakkitIntrinsic
import dev.karmakrafts.trakkit.compiler.util.TrakkitNames
import dev.karmakrafts.trakkit.compiler.util.getFunctionLocation
import dev.karmakrafts.trakkit.compiler.util.getLocation
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrParameterKind.Regular
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrErrorExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrGetField
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
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isEnumClass
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.isObject
import org.jetbrains.kotlin.ir.util.isVararg
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.util.toIrConst

internal data class TrakkitPluginContext(
    private val pluginContext: IrPluginContext
) : IrPluginContext by pluginContext {
    internal val annotationType: IrClassSymbol = pluginContext.referenceClass(TrakkitNames.Kotlin.Annotation.id)!!

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

    internal val annotationInfoType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(TrakkitNames.AnnotationUsageInfo.id)) {
            "Cannot find AnnotationInfo type, Trakkit runtime library is most likely missing"
        }
    internal val annotationInfoConstructor: IrConstructorSymbol =
        requireNotNull(pluginContext.referenceConstructors(TrakkitNames.AnnotationUsageInfo.id)).first()

    private val sourceLocationType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(TrakkitNames.SourceLocation.id)) {
            "Cannot find SourceLocation type, Trakkit runtime library is most likely missing"
        }
    private val sourcLocationCompanionType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(TrakkitNames.SourceLocation.Companion.id)) {
            "Cannot find SourceLocation.Companion type, Trakkit runtime library is most likely missing"
        }
    private val sourceLocationGetOrCreate: IrSimpleFunctionSymbol =
        pluginContext.referenceFunctions(TrakkitNames.SourceLocation.Companion.getOrCreate).first()

    internal val functionInfoType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(TrakkitNames.FunctionInfo.id)) {
            "Cannot find FunctionInfo type, Trakkit runtime library is most likely missing"
        }
    internal val functionInfoCompanionType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(TrakkitNames.FunctionInfo.Companion.id)) {
            "Cannot find FunctionInfo.Commpanion type, Trakkit runtime library is most likely missing"
        }
    internal val functionInfoGetOrCreate: IrSimpleFunctionSymbol =
        pluginContext.referenceFunctions(TrakkitNames.FunctionInfo.Companion.getOrCreate).first()

    internal val propertyInfoType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(TrakkitNames.PropertyInfo.id)) {
            "Cannot find PropertyInfo type, Trakkit runtime library is most likely missing"
        }
    internal val propertyInfoCompanionType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(TrakkitNames.PropertyInfo.Companion.id)) {
            "Cannot find PropertyInfo.Companion type, Trakkit runtime library is most likely missing"
        }
    internal val propertyInfoGetOrCreate: IrSimpleFunctionSymbol =
        pluginContext.referenceFunctions(TrakkitNames.PropertyInfo.Companion.getOrCreate).first()

    internal val classInfoType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(TrakkitNames.ClassInfo.id)) {
            "Cannot find ClassInfo type, Trakkit runtime library is most likely missing"
        }
    internal val classInfoCompanionType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(TrakkitNames.ClassInfo.Companion.id)) {
            "Cannot find ClassInfo.Companion type, Trakkit runtime library is most likely missing"
        }
    internal val classInfoGetOrCreate: IrSimpleFunctionSymbol =
        pluginContext.referenceFunctions(TrakkitNames.ClassInfo.Companion.getOrCreate).first()

    private val captureCallerType: IrClassSymbol = pluginContext.referenceClass(TrakkitNames.CaptureCaller.id)!!
    private val captureCallerConstructor: IrConstructorSymbol =
        pluginContext.referenceConstructors(TrakkitNames.CaptureCaller.id).first()

    internal val visibilityModifierType: IrClassSymbol =
        pluginContext.referenceClass(TrakkitNames.VisibilityModifier.id)!!
    internal val modalityModifierType: IrClassSymbol = pluginContext.referenceClass(TrakkitNames.ModalityModifier.id)!!
    internal val classModifierType: IrClassSymbol = pluginContext.referenceClass(TrakkitNames.ClassModifier.id)!!

    private fun TrakkitIntrinsic.getType(): IrType = when (resultType) {
        IntrinsicResultType.SOURCE_LOCATION -> sourceLocationType.defaultType
        IntrinsicResultType.FUNCTION_INFO -> functionInfoType.defaultType
        IntrinsicResultType.CLASS_INFO -> classInfoType.defaultType
        IntrinsicResultType.INT -> irBuiltIns.intType
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
            dispatchReceiver = classSymbol.getObjectInstance()
        }
    }

    fun createCaptureCaller(intrinsics: List<String>): IrConstructorCallImpl = IrConstructorCallImpl(
        startOffset = SYNTHETIC_OFFSET,
        endOffset = SYNTHETIC_OFFSET,
        type = captureCallerType.defaultType,
        symbol = captureCallerConstructor,
        typeArgumentsCount = 0,
        constructorTypeArgumentsCount = 0
    ).apply { // @formatter:off
        putValueArgument(0, IrVarargImpl(
            startOffset = SYNTHETIC_OFFSET,
            endOffset = SYNTHETIC_OFFSET,
            type = irBuiltIns.arrayClass.typeWith(irBuiltIns.stringType),
            varargElementType = irBuiltIns.stringType,
            elements = intrinsics.map { it.toIrConst(irBuiltIns.stringType) })
        )
    } // @formatter:on

    private fun Pair<IrExpression?, IrExpression?>.instantiate(
        firstType: IrType, secondType: IrType
    ): IrConstructorCallImpl = IrConstructorCallImpl(
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

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrElement?.unwrapAnnotationValue(): Any? {
        return when (this) {
            is IrErrorExpression -> error("Got IrErrorExpression in getConstType: $description")
            is IrExpressionBody -> expression
            is IrGetField -> symbol.owner.initializer
            is IrClassReference -> type
            is IrConst -> value
            is IrVararg -> elements.map { element ->
                check(element is IrExpression) { "Annotation vararg element must be an expression" }
                element.unwrapAnnotationValue()
            }.toList()

            else -> null
        }
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    fun IrConstructorCall.getAnnotationValues(): Map<String, Any?> {
        val constructor = symbol.owner
        val parameters = constructor.parameters.filter { it.kind == IrParameterKind.Regular }
        if (parameters.isEmpty()) return emptyMap()
        val parameterNames = parameters.map { it.name.asString() }
        check(parameterNames.size == valueArgumentsCount) { "Missing annotation parameter info" }
        val values = HashMap<String, Any?>()
        val firstParamIndex = parameters.first().indexInOldValueParameters
        val lastParamIndex = firstParamIndex + parameters.size
        var paramIndex = 0
        for (index in firstParamIndex..<lastParamIndex) {
            val value = getValueArgument(index)
            values[parameterNames[paramIndex]] = value.unwrapAnnotationValue()
            paramIndex++
        }
        return values
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    fun IrConstructorCall.getAnnotationUsageInfo( // @formatter:off
        module: IrModuleFragment,
        file: IrFile,
        source: List<String>,
    ): AnnotationUsageInfo = AnnotationUsageInfo(
        // @formatter:on
        location = getLocation(module, file, source),
        type = symbol.owner.constructedClassType,
        values = getAnnotationValues(),
    )

    internal fun createListOf(
        type: IrType, values: List<IrExpression>
    ): IrCallImpl = IrCallImplWithShape(
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

    internal fun createMapOf( // @formatter:off
        keyType: IrType,
        valueType: IrType,
        values: List<Pair<IrExpression?, IrExpression?>>
    ): IrCallImpl = IrCallImplWithShape( // @formatter:on
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

    private fun Any.getConstIrType(): IrType? = when (this) {
        is Byte -> irBuiltIns.byteType
        is Short -> irBuiltIns.shortType
        is Int -> irBuiltIns.intType
        is Long -> irBuiltIns.longType
        is Float -> irBuiltIns.floatType
        is Double -> irBuiltIns.doubleType
        is String -> irBuiltIns.stringType
        is Char -> irBuiltIns.charType
        is Boolean -> irBuiltIns.booleanType
        else -> null
    }

    private fun Any.getIrType(): IrType? = when(this) {
        is IrType, is IrClassReference -> irBuiltIns.kClassClass.starProjectedType
        is Array<*>, is List<*> -> irBuiltIns.arrayClass.starProjectedType
        else -> getConstIrType()
    }

    internal fun Any.toIrValueOrNull(): IrExpression? = when (this@toIrValueOrNull) {
        is IrClassReference -> this@toIrValueOrNull
        is IrType -> this@toIrValueOrNull.toClassReference()
        is List<*> -> createListOf(irBuiltIns.anyType, this@toIrValueOrNull.mapNotNull { it?.toIrValue() })
        is Array<*> -> createListOf(irBuiltIns.anyType, this@toIrValueOrNull.mapNotNull { it?.toIrValue() })
        else -> this@toIrValueOrNull.getConstIrType()?.let { this@toIrValueOrNull.toIrConst(it) }
    }

    internal fun Any.toIrValue(): IrExpression = requireNotNull(toIrValueOrNull())

    internal fun IrClassSymbol.getObjectInstance(): IrGetObjectValueImpl = IrGetObjectValueImpl(
        startOffset = SYNTHETIC_OFFSET,
        endOffset = SYNTHETIC_OFFSET,
        type = defaultType,
        symbol = this@getObjectInstance
    )

    fun SourceLocation.instantiateCached(): IrCallImpl = IrCallImplWithShape(
        startOffset = SYNTHETIC_OFFSET,
        endOffset = SYNTHETIC_OFFSET,
        type = sourceLocationType.defaultType,
        symbol = sourceLocationGetOrCreate,
        typeArgumentsCount = 0,
        valueArgumentsCount = 4,
        contextParameterCount = 0,
        hasDispatchReceiver = true,
        hasExtensionReceiver = false
    ).apply {
        var index = 0
        putValueArgument(index++, module.toIrConst(irBuiltIns.stringType))
        putValueArgument(index++, file.toIrConst(irBuiltIns.stringType))
        putValueArgument(index++, line.toIrConst(irBuiltIns.intType))
        putValueArgument(index, column.toIrConst(irBuiltIns.intType))
        dispatchReceiver = sourcLocationCompanionType.getObjectInstance()
    }

    fun SourceLocation.createHashSum(): IrConst {
        return hashCode().toIrConst(irBuiltIns.intType)
    }

    internal fun IrType.toClassReference(): IrClassReferenceImpl {
        return IrClassReferenceImpl(
            startOffset = SYNTHETIC_OFFSET,
            endOffset = SYNTHETIC_OFFSET,
            type = irBuiltIns.kClassClass.starProjectedType,
            symbol = classOrFail,
            classType = this
        )
    }

    private fun List<IrConstructorCall>.toAnnotationMap( // @formatter:off
        module: IrModuleFragment,
        file: IrFile,
        source: List<String>
    ): HashMap<IrType, ArrayList<AnnotationUsageInfo>> { // @formatter:on
        val annotationMap = HashMap<IrType, ArrayList<AnnotationUsageInfo>>()
        for (call in this) {
            annotationMap.getOrPut(call.type) { ArrayList() } += call.getAnnotationUsageInfo(module, file, source)
        }
        return annotationMap
    }

    fun IrFunction.getFunctionInfo( // @formatter:off
        module: IrModuleFragment,
        file: IrFile,
        source: List<String>,
    ): FunctionInfo {
        val regularParams = valueParameters.filter { it.kind == Regular }
        return FunctionInfo.getOrCreate(
            location = getFunctionLocation(module, file, source),
            qualifiedName = kotlinFqName.asString(),
            name = name.asString(),
            typeParameterNames = typeParameters.map { it.name.asString() },
            returnType = returnType,
            parameterTypes = regularParams.map { it.type },
            parameterNames = regularParams.map { it.name.asString() },
            hashTransform = { hash -> 31 * hash + parent.hashCode() }
        ) { // @formatter:on
            annotations = this@getFunctionInfo.annotations.toAnnotationMap(module, file, source)
        }
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    fun IrAnonymousInitializer.getFunctionInfo( // @formatter:off
        module: IrModuleFragment,
        file: IrFile,
        source: List<String>
    ): FunctionInfo { // @formatter:on
        val constructor = requireNotNull(parentAsClass.primaryConstructor) { "Missing primary class constructor" }
        val regularParams = constructor.valueParameters.filter { it.kind == Regular }
        return FunctionInfo.getOrCreate( // @formatter:off
            location = getLocation(module, file, source),
            qualifiedName = constructor.kotlinFqName.asString(),
            name = constructor.name.asString(),
            typeParameterNames = constructor.typeParameters.map { it.name.asString() },
            returnType = constructor.returnType,
            parameterTypes = regularParams.map { it.type },
            parameterNames = regularParams.map { it.name.asString() },
            hashTransform = { hash -> 31 * hash + parent.hashCode() }
        ) { // @formatter:on
            annotations = constructor.annotations.toAnnotationMap(module, file, source)
        }
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrClassSymbol.getEnumConstant(name: String): IrEnumEntrySymbol {
        return requireNotNull(
            defaultType.classOrFail.owner.declarations.filterIsInstance<IrEnumEntry>()
                .find { it.name.asString() == name }) { "No entry $name in $this" }.symbol
    }

    internal inline fun <T> T.getEnumValue(
        type: IrClassSymbol, mapper: T.() -> String
    ): IrGetEnumValueImpl = IrGetEnumValueImpl(
        startOffset = SYNTHETIC_OFFSET,
        endOffset = SYNTHETIC_OFFSET,
        type = type.defaultType,
        symbol = type.getEnumConstant(this.mapper())
    )

    internal fun Visibility.getVisibilityName(): String = when (this) {
        Visibilities.Public -> "PUBLIC"
        Visibilities.Protected -> "PROTECTED"
        Visibilities.Internal -> "INTERNAL"
        else -> "PRIVATE"
    }

    internal fun Modality.getModalityName(): String = when (this) {
        Modality.OPEN -> "OPEN"
        Modality.SEALED -> "SEALED"
        Modality.ABSTRACT -> "ABSTRACT"
        Modality.FINAL -> "FINAL"
    }

    private fun IrProperty.getPropertyInfo( // @formatter:off
        module: IrModuleFragment,
        file: IrFile,
        source: List<String>
    ): PropertyInfo = PropertyInfo.getOrCreate(
        location = getLocation(module, file, source),
        qualifiedName = requireNotNull(fqNameWhenAvailable) {
            "Could not obtain fully qualified name of property ${this@getPropertyInfo}"
        }.asString(),
        name = name.asString(),
        type = requireNotNull(getter?.returnType) {
            "Could not determine property type for ${name.asString()}"
        },
        isMutable = isVar,
        visibility = visibility.delegate,
        modality = modality,
        getter = requireNotNull(getter) {
            "Property requires at least a getter"
        }.getFunctionInfo(module, file, source),
        setter = setter?.getFunctionInfo(module, file, source)
    ) // @formatter:on

    private fun IrClass.getClassModifier(): ClassModifier? = when {
        isData -> ClassModifier.DATA
        isValue -> ClassModifier.VALUE
        isEnumClass -> ClassModifier.ENUM
        else -> null
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrClass.getCompanionObjects(
        module: IrModuleFragment, file: IrFile, source: List<String>
    ): List<ClassInfo> =
        declarations.filterIsInstance<IrClass>().filter { it.isCompanion }.map { it.getClassInfo(module, file, source) }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    fun IrClass.getClassInfo( // @formatter:off
        module: IrModuleFragment,
        file: IrFile,
        source: List<String>
    ): ClassInfo = ClassInfo.getOrCreate(
        location = getLocation(module, file, source),
        type = symbol.defaultType,
        typeParameterNames = typeParameters.map { it.name.asString() },
        companionObjects = getCompanionObjects(module, file, source),
        isInterface = isInterface,
        isObject = isObject,
        isCompanionObject = isCompanion,
        visibility = visibility.delegate,
        modality = modality,
        classType = getClassModifier()
    ) {
        functions = this@getClassInfo.functions.map { it.getFunctionInfo(module, file, source) }.toList()
        properties = this@getClassInfo.properties.map { it.getPropertyInfo(module, file, source) }.toList()
        annotations = this@getClassInfo.annotations.toAnnotationMap(module, file, source)
    }
}