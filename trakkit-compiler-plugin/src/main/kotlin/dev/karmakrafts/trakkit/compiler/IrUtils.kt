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

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.getAnnotationArgumentValue
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isFakeOverride

private const val UNDEFINED_OFFSET: Int = -1

internal fun IrFunction.getIntrinsicType(): TrakkitIntrinsic? {
    if (!hasAnnotation(TrakkitNames.TrakkitIntrinsic.id)) return null
    val intrinsicName = getAnnotationArgumentValue<String>(TrakkitNames.TrakkitIntrinsic.fqName, "value") ?: return null
    return TrakkitIntrinsic.byName(intrinsicName)
}

internal fun getLineNumber(source: List<String>, startOffset: Int, endOffset: Int): Int {
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

internal fun getColumnNumber(source: List<String>, startOffset: Int, endOffset: Int): Int {
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

internal fun getCallLocation(
    module: IrModuleFragment,
    file: IrFile,
    source: List<String>,
    expression: IrFunctionAccessExpression
): SourceLocation = SourceLocation(
    module = module.name.asString(),
    file = file.path,
    line = getLineNumber(source, expression.startOffset, expression.endOffset),
    column = getColumnNumber(source, expression.startOffset, expression.endOffset)
)

internal fun getFunctionLocation( // @formatter:off
    module: IrModuleFragment,
    file: IrFile,
    source: List<String>,
    function: IrFunction
): SourceLocation {
    val isFakeOverride = function.isFakeOverride
    return SourceLocation( // @formatter:on
        module = module.name.asString(),
        file = file.path,
        line = if (isFakeOverride) SourceLocation.FAKE_OVERRIDE_OFFSET
        else getLineNumber(
            source, function.startOffset, function.endOffset
        ),
        column = if (isFakeOverride) SourceLocation.FAKE_OVERRIDE_OFFSET
        else getColumnNumber(
            source, function.startOffset, function.endOffset
        )
    )
}

internal fun getClassLocation( // @formatter:off
    module: IrModuleFragment,
    file: IrFile,
    source: List<String>,
    clazz: IrClass
): SourceLocation = SourceLocation( // @formatter:on
    module = module.name.asString(),
    file = file.path,
    line = getLineNumber(source, clazz.startOffset, clazz.endOffset),
    column = getColumnNumber(source, clazz.startOffset, clazz.endOffset)
)