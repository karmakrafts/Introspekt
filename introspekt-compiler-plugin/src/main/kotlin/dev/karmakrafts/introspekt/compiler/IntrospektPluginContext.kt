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
import dev.karmakrafts.introspekt.compiler.util.getEnumValue
import dev.karmakrafts.introspekt.compiler.util.toClassReference
import dev.karmakrafts.introspekt.compiler.util.toStdPair
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImplWithShape
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.isTypeParameter
import org.jetbrains.kotlin.ir.util.toIrConst

internal data class IntrospektPluginContext(
    private val pluginContext: IrPluginContext,
    val irModule: IrModuleFragment,
    val irFile: IrFile,
    val source: List<String>,
    val introspektSymbols: IntrospektSymbols
) : IrPluginContext by pluginContext {
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    fun createInlineDefaults(modes: List<InlineDefaultMode>): IrConstructorCallImpl = IrConstructorCallImpl(
        startOffset = SYNTHETIC_OFFSET,
        endOffset = SYNTHETIC_OFFSET,
        type = introspektSymbols.inlineDefaultsType.defaultType,
        symbol = introspektSymbols.inlineDefaultsConstructor,
        typeArgumentsCount = 0,
        constructorTypeArgumentsCount = 0
    ).apply {
        arguments[symbol.owner.parameters.first { it.name.asString() == "modes" }] = IrVarargImpl(
            startOffset = SYNTHETIC_OFFSET,
            endOffset = SYNTHETIC_OFFSET,
            type = irBuiltIns.arrayClass.typeWith(introspektSymbols.inlineDefaultsModeType.defaultType),
            varargElementType = introspektSymbols.inlineDefaultsModeType.defaultType,
            elements = modes.map { mode ->
                mode.getEnumValue(introspektSymbols.inlineDefaultsModeType) { enumName }
            })
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    internal fun createListOf( // @formatter:off
        type: IrType,
        values: List<IrExpression>
    ): IrCallImpl = IrCallImplWithShape( // @formatter:on
        startOffset = SYNTHETIC_OFFSET,
        endOffset = SYNTHETIC_OFFSET,
        type = introspektSymbols.listType.typeWith(type),
        symbol = introspektSymbols.listOfFunction,
        typeArgumentsCount = 1,
        valueArgumentsCount = 1,
        contextParameterCount = 0,
        hasDispatchReceiver = false,
        hasExtensionReceiver = false
    ).apply {
        val function = introspektSymbols.listOfFunction.owner
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
        type = introspektSymbols.mapType.typeWith(keyType, valueType),
        symbol = introspektSymbols.mapOfFunction,
        typeArgumentsCount = 2,
        valueArgumentsCount = 1,
        contextParameterCount = 0,
        hasDispatchReceiver = false,
        hasExtensionReceiver = false
    ).apply {
        val function = introspektSymbols.mapOfFunction.owner
        typeArguments[0] = keyType
        typeArguments[1] = valueType
        val pairType = introspektSymbols.pairType.typeWith(keyType, valueType)
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