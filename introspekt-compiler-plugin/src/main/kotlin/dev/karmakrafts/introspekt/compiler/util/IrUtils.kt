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
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.getAnnotationArgumentValue
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isFakeOverride

private const val UNDEFINED_OFFSET: Int = -1

internal fun IrFunction.getIntrinsicType(): TrakkitIntrinsic? {
    if (!hasAnnotation(IntrospektNames.IntrospektIntrinsic.id)) return null
    val intrinsicName = getAnnotationArgumentValue<String>(IntrospektNames.IntrospektIntrinsic.fqName, "value") ?: return null
    return TrakkitIntrinsic.byName(intrinsicName)
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