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

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrErrorExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrGetEnumValue
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.name.FqName

private const val UNDEFINED_OFFSET: Int = -1

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal fun IrElement?.unwrapAnyAnnotationValue(): Any? {
    return when (this) {
        is IrErrorExpression -> error("Got IrErrorExpression in getConstType: $description")
        is IrExpressionBody -> expression
        is IrGetField -> symbol.owner.initializer
        is IrGetEnumValue -> symbol.owner.name.asString() // Enum values are unwrapped to their constant names
        is IrClassReference -> type
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
    return when {
        javaType.isEnum -> (javaType.enumConstants as Array<Enum<*>>).find { it.name == value }
        else -> value
    } as? T
}

internal fun IrAnnotationContainer.getAnnotation(
    type: FqName, index: Int = 0
): IrConstructorCall? {
    return annotations.filter { it.type.classFqName == type }.getOrNull(index)
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal inline fun <reified T> IrAnnotationContainer.getAnnotationValue(
    type: FqName, name: String, index: Int = 0
): T? {
    val annotation = getAnnotation(type, index) ?: return null
    val constructor = annotation.symbol.owner
    // @formatter:off
    val parameter = constructor.parameters
        .filter { it.kind == IrParameterKind.Regular }
        .find { it.name.asString() == name }
        ?: return null
    // @formatter:on
    return annotation.getValueArgument(parameter.indexInOldValueParameters).unwrapAnnotationValue<T>()
}

internal fun IrFunction.getIntrinsicType(): IntrospektIntrinsic? {
    if (!hasAnnotation(IntrospektNames.IntrospektIntrinsic.id)) return null
    return getAnnotationValue<IntrospektIntrinsic>(IntrospektNames.IntrospektIntrinsic.fqName, "type")
}

internal fun IrAnnotationContainer.isTraceable(): Boolean = hasAnnotation(IntrospektNames.Trace.id)

internal fun IrAnnotationContainer.getTraceType(): List<TraceType> {
    if (!isTraceable()) return emptyList()
    return emptyList() // TODO: implement this
}

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

internal fun IrFunction.getFunctionLocation( // @formatter:off
    module: IrModuleFragment,
    file: IrFile,
    source: List<String>
): SourceLocation { // @formatter:on
    val isFakeOverride = isFakeOverride
    return SourceLocation.getOrCreate(
        module = module.name.asString(),
        file = file.path,
        line = if (isFakeOverride) SourceLocation.FAKE_OVERRIDE_OFFSET
        else getLineNumber(source, startOffset, endOffset),
        column = if (isFakeOverride) SourceLocation.FAKE_OVERRIDE_OFFSET
        else getColumnNumber(source, startOffset, endOffset)
    )
}

internal fun IrElement.getLocation( // @formatter:off
    module: IrModuleFragment,
    file: IrFile,
    source: List<String>,
): SourceLocation = SourceLocation.getOrCreate( // @formatter:on
    module = module.name.asString(),
    file = file.path,
    line = getLineNumber(source, startOffset, endOffset),
    column = getColumnNumber(source, startOffset, endOffset)
)