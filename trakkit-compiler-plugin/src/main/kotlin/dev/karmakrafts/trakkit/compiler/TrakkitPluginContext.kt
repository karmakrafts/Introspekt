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
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.declarations.IrClass
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
import org.jetbrains.kotlin.ir.expressions.impl.IrGetObjectValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
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
import org.jetbrains.kotlin.ir.util.isVararg
import org.jetbrains.kotlin.ir.util.toIrConst

internal data class TrakkitPluginContext(
    private val pluginContext: IrPluginContext
) : IrPluginContext by pluginContext {
    val annotationType: IrClassSymbol = pluginContext.referenceClass(TrakkitNames.Kotlin.Annotation.id)!!

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    val listOfFunction: IrSimpleFunctionSymbol = pluginContext.referenceFunctions(TrakkitNames.Kotlin.listOf)
        .find { symbol -> symbol.owner.valueParameters.any { it.isVararg } }!!
    val listType: IrClassSymbol = pluginContext.referenceClass(TrakkitNames.Kotlin.List.id)!!

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    val mapOfFunction: IrSimpleFunctionSymbol = pluginContext.referenceFunctions(TrakkitNames.Kotlin.mapOf)
        .find { symbol -> symbol.owner.valueParameters.any { it.isVararg } }!!
    val mapType: IrClassSymbol = pluginContext.referenceClass(TrakkitNames.Kotlin.Map.id)!!

    val pairConstructor: IrConstructorSymbol = pluginContext.referenceConstructors(TrakkitNames.Kotlin.Pair.id).first()
    val pairType: IrClassSymbol = pluginContext.referenceClass(TrakkitNames.Kotlin.Pair.id)!!

    val annotationInfoType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(TrakkitNames.AnnotationInfo.id)) {
            "Cannot find AnnotationInfo type, Trakkit runtime library is most likely missing"
        }
    val annotationInfoConstructor: IrConstructorSymbol =
        pluginContext.referenceConstructors(TrakkitNames.AnnotationInfo.id).first()

    val sourceLocationType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(TrakkitNames.SourceLocation.id)) {
            "Cannot find SourceLocation type, Trakkit runtime library is most likely missing"
        }
    val sourceLocationConstructor: IrConstructorSymbol =
        pluginContext.referenceConstructors(TrakkitNames.SourceLocation.id).first()

    val functionInfoType: IrClassSymbol = requireNotNull(pluginContext.referenceClass(TrakkitNames.FunctionInfo.id)) {
        "Cannot find FunctionInfo type, Trakkit runtime library is most likely missing"
    }
    val functionInfoConstructor: IrConstructorSymbol =
        pluginContext.referenceConstructors(TrakkitNames.FunctionInfo.id).first()

    val classInfoType: IrClassSymbol = requireNotNull(pluginContext.referenceClass(TrakkitNames.ClassInfo.id)) {
        "Cannot find ClassInfo type, Trakkit runtime library is most likely missing"
    }
    val classInfoConstructor: IrConstructorSymbol =
        pluginContext.referenceConstructors(TrakkitNames.ClassInfo.id).first()

    val captureCallerType: IrClassSymbol = pluginContext.referenceClass(TrakkitNames.CaptureCaller.id)!!
    val captureCallerConstructor: IrConstructorSymbol =
        pluginContext.referenceConstructors(TrakkitNames.CaptureCaller.id).first()

    // intrinsics

    fun TrakkitIntrinsic.getType(): IrType = when (this) {
        TrakkitIntrinsic.SL_HERE, TrakkitIntrinsic.SL_CURRENT_FUNCTION, TrakkitIntrinsic.SL_CURRENT_CLASS -> sourceLocationType.defaultType
        TrakkitIntrinsic.FI_CURRENT -> functionInfoType.defaultType
        TrakkitIntrinsic.CI_CURRENT -> classInfoType.defaultType
        else -> irBuiltIns.intType // Hashsums
    }

    fun TrakkitIntrinsic.getSymbol(): IrSimpleFunctionSymbol {
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

    fun Pair<IrExpression?, IrExpression?>.instantiate(firstType: IrType, secondType: IrType): IrConstructorCallImpl {
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
        val firstParamIndex = parameters.first().indexInParameters
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
        source: List<String>,
        function: IrFunction?
    ): AnnotationInfo = AnnotationInfo( // @formatter:on
        location = getCallLocation(module, file, source, this@getAnnotationInfo, function),
        type = symbol.owner.constructedClassType,
        values = getAnnotationValues()
    )

    fun createListOf(type: IrType, values: List<IrExpression>): IrCallImpl {
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

    fun createListOf(type: IrType, vararg values: IrExpression): IrCallImpl = createListOf(type, values.toList())

    fun createMapOf( // @formatter:off
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

    fun createMapOf(keyType: IrType, valueType: IrType, vararg values: Pair<IrExpression?, IrExpression?>): IrCallImpl =
        createMapOf(keyType, valueType, values.toList())

    fun Any?.getConstType(): IrType = when (this) {
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

    fun Any?.toIrValueOrType(): IrExpression = when (this@toIrValueOrType) {
        is IrType -> this@toIrValueOrType.toClassReference()
        is List<*> -> createListOf(irBuiltIns.anyType, this@toIrValueOrType.map { it.toIrValueOrType() })
        // TODO: create actual arrays here?
        is Array<*> -> createListOf(irBuiltIns.anyType, this@toIrValueOrType.map { it.toIrValueOrType() })
        else -> toIrConst(this@toIrValueOrType.getConstType())
    }

    fun AnnotationInfo.instantiate(): IrConstructorCallImpl {
        return IrConstructorCallImpl(
            startOffset = SYNTHETIC_OFFSET,
            endOffset = SYNTHETIC_OFFSET,
            type = annotationInfoType.defaultType,
            symbol = annotationInfoConstructor,
            typeArgumentsCount = 0,
            constructorTypeArgumentsCount = 0
        ).apply {
            putValueArgument(0, location.instantiate())
            putValueArgument(1, this@instantiate.type.toClassReference())
            // @formatter:off
            putValueArgument(2, createMapOf(
                keyType = irBuiltIns.stringType,
                valueType = irBuiltIns.anyType,
                values = values.map { (key, value) ->
                    key.toIrConst(irBuiltIns.stringType) to value.toIrValueOrType()
                }
            ))
            // @formatter:on
        }
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
            putValueArgument(0, module.toIrConst(irBuiltIns.stringType))
            putValueArgument(1, file.toIrConst(irBuiltIns.stringType))
            putValueArgument(2, function.toIrConst(irBuiltIns.stringType))
            putValueArgument(3, line.toIrConst(irBuiltIns.intType))
            putValueArgument(4, column.toIrConst(irBuiltIns.intType))
        }
    }

    fun SourceLocation.createHashSum(): IrConst {
        return hashCode().toIrConst(irBuiltIns.intType)
    }

    fun IrType.toClassReference(): IrClassReferenceImpl {
        return IrClassReferenceImpl(
            startOffset = SYNTHETIC_OFFSET,
            endOffset = SYNTHETIC_OFFSET,
            type = this,
            symbol = classOrFail,
            classType = this
        )
    }

    fun List<IrConstructorCall>.toAnnotationMap( // @formatter:off
        module: IrModuleFragment,
        file: IrFile,
        source: List<String>,
        function: IrFunction?
    ): HashMap<IrType, AnnotationInfo> { // @formatter:on
        val annotations = HashMap<IrType, AnnotationInfo>()
        for (call in this) {
            annotations[call.type] = call.getAnnotationInfo(module, file, source, function)
        }
        return annotations
    }

    fun IrFunction.getFunctionInfo( // @formatter:off
        module: IrModuleFragment,
        file: IrFile,
        source: List<String>,
    ): FunctionInfo = FunctionInfo( // @formatter:on
        location = getFunctionLocation(module, file, source, this@getFunctionInfo),
        typeParameterNames = typeParameters.map { it.name.asString() },
        returnType = returnType,
        parameterTypes = valueParameters.filter { it.kind == IrParameterKind.Regular }.map { it.type },
        parameterNames = valueParameters.filter { it.kind == IrParameterKind.Regular }.map { it.name.asString() },
        annotations = annotations.toAnnotationMap(module, file, source, this@getFunctionInfo)
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
            // location
            putValueArgument(0, location.instantiate())
            // typeParameterNames
            putValueArgument(1, createListOf(
                type = irBuiltIns.stringType,
                values = typeParameterNames.map { it.toIrConst(irBuiltIns.stringType) }
            ))
            // returnType
            putValueArgument(2, returnType.toClassReference())
            // parameterTypes
            putValueArgument(3, createListOf(
                type = irBuiltIns.kClassClass.starProjectedType,
                values = parameterTypes.map { it.type.toIrValueOrType() }
            ))
            // parameterNames
            putValueArgument(4, createListOf(
                type = irBuiltIns.stringType,
                values = parameterNames.map { it.toIrConst(irBuiltIns.stringType) }
            ))
            // annotations
            putValueArgument(5, createMapOf(
                keyType = irBuiltIns.kClassClass.typeWith(annotationType.defaultType),
                valueType = annotationInfoType.defaultType,
                values = annotations.map { (type, info) ->
                    type.toIrValueOrType() to info.instantiate()
                }
            ))
        } // @formatter:on
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    fun IrClass.getClassInfo( // @formatter:off
        module: IrModuleFragment,
        file: IrFile,
        source: List<String>
    ): ClassInfo = ClassInfo( // @formatter:on
        location = getClassLocation(module, file, source, this),
        type = symbol.defaultType,
        typeParameterNames = typeParameters.map { it.name.asString() },
        annotations = annotations.toAnnotationMap(module, file, source, null),
        functions = functions.map { it.getFunctionInfo(module, file, source) }.toList()
    )

    fun ClassInfo.instantiate(): IrConstructorCallImpl {
        return IrConstructorCallImpl(
            startOffset = SYNTHETIC_OFFSET,
            endOffset = SYNTHETIC_OFFSET,
            type = classInfoType.defaultType,
            symbol = classInfoConstructor,
            typeArgumentsCount = 0,
            constructorTypeArgumentsCount = 0
        ).apply {
            // location
            putValueArgument(0, location.instantiate())
            // type
            putValueArgument(1, this@instantiate.type.toClassReference())
            // typeParameterNames
            putValueArgument(
                2, createListOf(
                type = irBuiltIns.stringType,
                values = typeParameterNames.map { it.toIrConst(irBuiltIns.stringType) }))
            // annotations
            putValueArgument(
                3, createMapOf(
                keyType = irBuiltIns.kClassClass.typeWith(annotationType.defaultType),
                valueType = annotationInfoType.defaultType,
                values = annotations.map { (type, info) ->
                    type.toIrValueOrType() to info.instantiate()
                }))
            // functions
            putValueArgument(
                4, createListOf(
                type = functionInfoType.defaultType, values = functions.map { it.instantiate() }))
        }
    }
}