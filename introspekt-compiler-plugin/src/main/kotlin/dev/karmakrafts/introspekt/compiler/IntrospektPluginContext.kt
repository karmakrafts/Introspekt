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

package dev.karmakrafts.introspekt.compiler

import dev.karmakrafts.introspekt.compiler.util.InlineDefaultMode
import dev.karmakrafts.introspekt.compiler.util.IntrospektNames
import dev.karmakrafts.introspekt.compiler.util.getEnumValue
import dev.karmakrafts.introspekt.compiler.util.toClassReference
import dev.karmakrafts.introspekt.compiler.util.toStdPair
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImplWithShape
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.isTypeParameter
import org.jetbrains.kotlin.ir.util.isVararg
import org.jetbrains.kotlin.ir.util.toIrConst

internal data class IntrospektPluginContext(
    private val pluginContext: IrPluginContext
) : IrPluginContext by pluginContext {
    internal val annotationType: IrClassSymbol = pluginContext.referenceClass(IntrospektNames.Kotlin.Annotation.id)!!

    // listOf
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private val listOfFunction: IrSimpleFunctionSymbol = pluginContext.referenceFunctions(IntrospektNames.Kotlin.listOf)
        .find { symbol -> symbol.owner.parameters.filter { it.kind == IrParameterKind.Regular }.any { it.isVararg } }!!
    private val listType: IrClassSymbol = pluginContext.referenceClass(IntrospektNames.Kotlin.List.id)!!

    // mapOf
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private val mapOfFunction: IrSimpleFunctionSymbol = pluginContext.referenceFunctions(IntrospektNames.Kotlin.mapOf)
        .find { symbol -> symbol.owner.parameters.filter { it.kind == IrParameterKind.Regular }.any { it.isVararg } }!!
    private val mapType: IrClassSymbol = pluginContext.referenceClass(IntrospektNames.Kotlin.Map.id)!!

    // Pair
    internal val pairConstructor: IrConstructorSymbol =
        pluginContext.referenceConstructors(IntrospektNames.Kotlin.Pair.id).first()
    internal val pairType: IrClassSymbol = pluginContext.referenceClass(IntrospektNames.Kotlin.Pair.id)!!

    // AnnotationUsageInfo
    internal val annotationUsageInfoType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(IntrospektNames.AnnotationUsageInfo.id)) {
            "Cannot find AnnotationInfo type, Introspekt runtime library is most likely missing"
        }
    internal val annotationUsageInfoConstructor: IrConstructorSymbol =
        requireNotNull(pluginContext.referenceConstructors(IntrospektNames.AnnotationUsageInfo.id)).first()

    // SourceLocation
    internal val sourceLocationType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(IntrospektNames.SourceLocation.id)) {
            "Cannot find SourceLocation type, Introspekt runtime library is most likely missing"
        }
    internal val sourcLocationCompanionType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(IntrospektNames.SourceLocation.Companion.id)) {
            "Cannot find SourceLocation.Companion type, Introspekt runtime library is most likely missing"
        }
    internal val sourceLocationGetOrCreate: IrSimpleFunctionSymbol =
        pluginContext.referenceFunctions(IntrospektNames.SourceLocation.Companion.getOrCreate).first()

    // FunctionInfo
    internal val functionInfoType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(IntrospektNames.FunctionInfo.id)) {
            "Cannot find FunctionInfo type, Introspekt runtime library is most likely missing"
        }
    internal val functionInfoCompanionType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(IntrospektNames.FunctionInfo.Companion.id)) {
            "Cannot find FunctionInfo.Commpanion type, Introspekt runtime library is most likely missing"
        }
    internal val functionInfoGetOrCreate: IrSimpleFunctionSymbol =
        pluginContext.referenceFunctions(IntrospektNames.FunctionInfo.Companion.getOrCreate).first()

    // PropertyInfo
    internal val propertyInfoType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(IntrospektNames.PropertyInfo.id)) {
            "Cannot find PropertyInfo type, Introspekt runtime library is most likely missing"
        }
    internal val propertyInfoCompanionType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(IntrospektNames.PropertyInfo.Companion.id)) {
            "Cannot find PropertyInfo.Companion type, Introspekt runtime library is most likely missing"
        }
    internal val propertyInfoGetOrCreate: IrSimpleFunctionSymbol =
        pluginContext.referenceFunctions(IntrospektNames.PropertyInfo.Companion.getOrCreate).first()

    // FieldInfo
    internal val fieldInfoType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(IntrospektNames.FieldInfo.id)) {
            "Cannot find FieldInfo type, Introspekt runtime library is most likely missing"
        }
    internal val fieldInfoCompanionType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(IntrospektNames.FieldInfo.Companion.id)) {
            "Cannot find FieldInfo.Companion type, Introspekt runtime library is most likely missing"
        }
    internal val fieldInfoGetOrCreate: IrSimpleFunctionSymbol =
        pluginContext.referenceFunctions(IntrospektNames.FieldInfo.Companion.getOrCreate).first()

    // ClassInfo
    internal val classInfoType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(IntrospektNames.ClassInfo.id)) {
            "Cannot find ClassInfo type, Introspekt runtime library is most likely missing"
        }
    internal val classInfoCompanionType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(IntrospektNames.ClassInfo.Companion.id)) {
            "Cannot find ClassInfo.Companion type, Introspekt runtime library is most likely missing"
        }
    internal val classInfoGetOrCreate: IrSimpleFunctionSymbol =
        pluginContext.referenceFunctions(IntrospektNames.ClassInfo.Companion.getOrCreate).first()

    // LocalInfo
    internal val localInfoType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(IntrospektNames.LocalInfo.id)) {
            "Cannot find LocalInfo type, Introspekt runtime library is most likely missing"
        }
    internal val localInfoCompanionType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(IntrospektNames.LocalInfo.Companion.id)) {
            "Cannot find LocalInfo.Companion type, Introspekt runtime library is most likely missing"
        }
    internal val localInfoGetOrCreate: IrSimpleFunctionSymbol =
        pluginContext.referenceFunctions(IntrospektNames.LocalInfo.Companion.getOrCreate).first()

    // ParameterInfo
    internal val parameterInfoType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(IntrospektNames.ParameterInfo.id)) {
            "Cannot find LocalInfo type, Introspekt runtime library is most likely missing"
        }
    internal val parameterInfoCompanionType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(IntrospektNames.ParameterInfo.Companion.id)) {
            "Cannot find LocalInfo.Companion type, Introspekt runtime library is most likely missing"
        }
    internal val parameterInfoGetOrCreate: IrSimpleFunctionSymbol =
        pluginContext.referenceFunctions(IntrospektNames.ParameterInfo.Companion.getOrCreate).first()

    // TypeInfo
    internal val typeInfoType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(IntrospektNames.TypeInfo.id)) {
            "Cannot find TypeInfo type, Introspekt runtime library is most likely missing"
        }

    // SimpleTypeInfo
    internal val simpleTypeInfoType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(IntrospektNames.SimpleTypeInfo.id)) {
            "Cannot find SimpleTypeInfo type, Introspekt runtime library is most likely missing"
        }
    internal val typeInfoCompanionType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(IntrospektNames.TypeInfo.Companion.id)) {
            "Cannot find SimpleTypeInfo.Companion type, Introspekt runtime library is most likely missing"
        }
    internal val typeInfoGetOrCreate: IrSimpleFunctionSymbol =
        pluginContext.referenceFunctions(IntrospektNames.TypeInfo.Companion.getOrCreate).first()

    // InlineDefaults
    private val inlineDefaultsType: IrClassSymbol = pluginContext.referenceClass(IntrospektNames.InlineDefaults.id)!!
    private val inlineDefaultsConstructor: IrConstructorSymbol =
        pluginContext.referenceConstructors(IntrospektNames.InlineDefaults.id).first()
    private val inlineDefaultsModeType: IrClassSymbol =
        pluginContext.referenceClass(IntrospektNames.InlineDefaults.Mode.id)!!

    internal val visibilityModifierType: IrClassSymbol =
        pluginContext.referenceClass(IntrospektNames.VisibilityModifier.id)!!
    internal val modalityModifierType: IrClassSymbol =
        pluginContext.referenceClass(IntrospektNames.ModalityModifier.id)!!
    internal val classModifierType: IrClassSymbol = pluginContext.referenceClass(IntrospektNames.ClassModifier.id)!!

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    fun createInlineDefaults(modes: List<InlineDefaultMode>): IrConstructorCallImpl = IrConstructorCallImpl(
        startOffset = SYNTHETIC_OFFSET,
        endOffset = SYNTHETIC_OFFSET,
        type = inlineDefaultsType.defaultType,
        symbol = inlineDefaultsConstructor,
        typeArgumentsCount = 0,
        constructorTypeArgumentsCount = 0
    ).apply {
        arguments[symbol.owner.parameters.first { it.name.asString() == "modes" }] = IrVarargImpl(
            startOffset = SYNTHETIC_OFFSET,
            endOffset = SYNTHETIC_OFFSET,
            type = irBuiltIns.arrayClass.typeWith(inlineDefaultsModeType.defaultType),
            varargElementType = inlineDefaultsModeType.defaultType,
            elements = modes.map { mode ->
                mode.getEnumValue(inlineDefaultsModeType) { enumName }
            })
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    internal fun createListOf( // @formatter:off
        type: IrType,
        values: List<IrExpression>
    ): IrCallImpl = IrCallImplWithShape( // @formatter:on
        startOffset = SYNTHETIC_OFFSET,
        endOffset = SYNTHETIC_OFFSET,
        type = listType.typeWith(type),
        symbol = listOfFunction,
        typeArgumentsCount = 1,
        valueArgumentsCount = 1,
        contextParameterCount = 0,
        hasDispatchReceiver = false,
        hasExtensionReceiver = false
    ).apply {
        val function = listOfFunction.owner
        typeArguments[0] = type
        arguments[function.parameters.first { it.name.asString() == "elements" }] = IrVarargImpl(
            startOffset = SYNTHETIC_OFFSET,
            endOffset = SYNTHETIC_OFFSET,
            type = irBuiltIns.arrayClass.typeWith(type),
            varargElementType = type,
            elements = values
        )
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
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
    ).apply {
        val function = mapOfFunction.owner
        typeArguments[0] = keyType
        typeArguments[1] = valueType
        val pairType = pairType.typeWith(keyType, valueType)
        arguments[function.parameters.first { it.name.asString() == "pairs" }] = IrVarargImpl(
            startOffset = SYNTHETIC_OFFSET,
            endOffset = SYNTHETIC_OFFSET,
            type = irBuiltIns.arrayClass.typeWith(pairType),
            varargElementType = pairType,
            elements = values.map { it.toStdPair().instantiate(this@IntrospektPluginContext, keyType, valueType) })
    }

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

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun Any.toIrValueOrNull(): IrExpression? = when (this@toIrValueOrNull) {
        is IrClassReference -> this@toIrValueOrNull
        is IrType -> if (isTypeParameter()) (this@toIrValueOrNull.classifierOrFail.owner as IrTypeParameter).superTypes.first()
            .toClassReference(this@IntrospektPluginContext)
        else this@toIrValueOrNull.toClassReference(this@IntrospektPluginContext)

        is List<*> -> createListOf(irBuiltIns.anyType, this@toIrValueOrNull.mapNotNull { it?.toIrValue() })
        is Array<*> -> createListOf(irBuiltIns.anyType, this@toIrValueOrNull.mapNotNull { it?.toIrValue() })
        else -> this@toIrValueOrNull.getConstIrType()?.let { this@toIrValueOrNull.toIrConst(it) }
    }

    internal fun Any.toIrValue(): IrExpression = requireNotNull(toIrValueOrNull())
}