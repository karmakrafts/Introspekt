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

package dev.karmakrafts.introspekt.compiler.util

import dev.karmakrafts.introspekt.compiler.IntrospektPluginContext
import dev.karmakrafts.introspekt.compiler.element.AnnotationUsageInfo
import dev.karmakrafts.introspekt.compiler.element.TypeInfo
import dev.karmakrafts.introspekt.compiler.element.getAnnotationUsageInfo
import dev.karmakrafts.introspekt.compiler.element.getTypeInfo
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrErrorExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrGetEnumValue
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetEnumValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetObjectValueImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrEnumEntrySymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.starProjectedType
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.isOverridable
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.filterIsInstanceAnd

private const val UNDEFINED_OFFSET: Int = -1

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal fun IrElement?.unwrapAnyAnnotationValue(): Any? {
    return when (this) {
        is IrErrorExpression -> error("Got IrErrorExpression in getConstType: $description")
        is IrExpressionBody -> expression
        is IrGetField -> symbol.owner.initializer
        is IrGetEnumValue -> symbol.owner.name.asString() // Enum values are unwrapped to their constant names
        is IrClassReference -> classType
        is IrConst -> value
        is IrVararg -> elements.map { element ->
            check(element is IrExpression) { "Annotation vararg element must be an expression" }
            element.unwrapAnyAnnotationValue()
        }.toList()

        else -> null
    }
}

@Suppress("UNCHECKED_CAST")
internal inline fun <reified T> IrElement?.unwrapAnnotationValue(): T? {
    val value = unwrapAnyAnnotationValue()
    val javaType = T::class.java
    return (if (javaType.isEnum) (javaType.enumConstants as Array<Enum<*>>).find { it.name == value as? String }
    else value) as? T
}

@Suppress("UNCHECKED_CAST")
internal inline fun <reified T> IrElement?.unwrapAnnotationValues(): List<T?> {
    val values = unwrapAnyAnnotationValue() as List<Any?>? ?: return emptyList()
    val javaType = T::class.java
    val isEnum = javaType.isEnum
    return values.map { value ->
        (if (isEnum) (javaType.enumConstants as Array<Enum<*>>).find { it.name == value as? String }
        else value) as? T
    }
}

internal fun IrAnnotationContainer.getAnnotation( // @formatter:off
    type: FqName,
    index: Int = 0
): IrConstructorCall? { // @formatter:on
    return annotations.filter { it.type.classFqName == type }.getOrNull(index)
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal fun IrAnnotationContainer.getRawAnnotationValue(
    type: FqName, name: String, index: Int = 0
): IrExpression? {
    val annotation = getAnnotation(type, index) ?: return null
    val constructor = annotation.symbol.owner
    // @formatter:off
    val parameter = constructor.parameters
        .filter { it.kind == IrParameterKind.Regular }
        .find { it.name.asString() == name }
        ?: return null
    // @formatter:on
    return annotation.getValueArgument(parameter.indexInOldValueParameters)
}

internal inline fun <reified T> IrAnnotationContainer.getAnnotationValue( // @formatter:off
    type: FqName,
    name: String,
    index: Int = 0
): T? = getRawAnnotationValue(type, name, index).unwrapAnnotationValue<T>() // @formatter:on

internal inline fun <reified T> IrAnnotationContainer.getAnnotationValues( // @formatter:off
    type: FqName,
    name: String,
    index: Int = 0
): List<T?> = getRawAnnotationValue(type, name, index).unwrapAnnotationValues<T>() // @formatter:on

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal fun IrConstructorCall.getAnnotationValues(): Map<String, Any?> {
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
        values[parameterNames[paramIndex]] = value.unwrapAnyAnnotationValue()
        paramIndex++
    }
    return values
}

internal fun List<IrConstructorCall>.toAnnotationMap( // @formatter:off
    module: IrModuleFragment,
    file: IrFile,
    source: List<String>
): HashMap<TypeInfo, ArrayList<AnnotationUsageInfo>> { // @formatter:on
    val annotationMap = HashMap<TypeInfo, ArrayList<AnnotationUsageInfo>>()
    for (call in this) {
        val type = call.type.getTypeInfo(module, file, source)
        annotationMap.getOrPut(type) { ArrayList() } += call.getAnnotationUsageInfo(module, file, source)
    }
    return annotationMap
}

internal fun IrType.toClassReference(context: IntrospektPluginContext): IrClassReferenceImpl = with(context) {
    IrClassReferenceImpl(
        startOffset = SYNTHETIC_OFFSET,
        endOffset = SYNTHETIC_OFFSET,
        type = irBuiltIns.kClassClass.starProjectedType,
        symbol = classOrFail,
        classType = this@toClassReference
    )
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal fun IrClass.getCompanionObjects(): List<IrClass> = declarations.filterIsInstanceAnd<IrClass> { it.isCompanion }

internal fun getLineNumber(source: List<String>, startOffset: Int, endOffset: Int = startOffset): Int {
    when {
        startOffset == UNDEFINED_OFFSET || endOffset == UNDEFINED_OFFSET -> return SourceLocation.UNDEFINED_OFFSET
        startOffset == SYNTHETIC_OFFSET || endOffset == SYNTHETIC_OFFSET -> return SourceLocation.SYNTHETIC_OFFSET
    }
    var currentOffset = 0
    for ((index, line) in source.withIndex()) {
        val lineLength = line.length
        if (currentOffset + lineLength >= startOffset) return index + 1
        currentOffset += lineLength + 1 // Include newline character
    }
    return 0
}

internal fun getColumnNumber(source: List<String>, startOffset: Int, endOffset: Int = startOffset): Int {
    when {
        startOffset == UNDEFINED_OFFSET || endOffset == UNDEFINED_OFFSET -> return SourceLocation.UNDEFINED_OFFSET
        startOffset == SYNTHETIC_OFFSET || endOffset == SYNTHETIC_OFFSET -> return SourceLocation.SYNTHETIC_OFFSET
    }
    var currentOffset = 0
    for (line in source) {
        val lineLength = line.length
        if (currentOffset + lineLength >= startOffset) return startOffset - currentOffset + 1
        currentOffset += lineLength + 1 // Include newline character
    }
    return 0
}

internal fun Visibility.getVisibilityName(): String = when (this) {
    Visibilities.Public -> "PUBLIC"
    Visibilities.Protected -> "PROTECTED"
    Visibilities.Internal -> "INTERNAL"
    else -> "PRIVATE"
}

internal fun IrFunction.getModality(): Modality = when {
    isOverridable -> Modality.OPEN
    else -> Modality.FINAL
}

internal fun IrClassSymbol.getObjectInstance(): IrGetObjectValueImpl = IrGetObjectValueImpl( // @formatter:off
    startOffset = SYNTHETIC_OFFSET,
    endOffset = SYNTHETIC_OFFSET,
    type = defaultType,
    symbol = this@getObjectInstance
) // @formatter:on

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal fun IrClassSymbol.getEnumConstant(name: String): IrEnumEntrySymbol {
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
    symbol = type.getEnumConstant(mapper())
)