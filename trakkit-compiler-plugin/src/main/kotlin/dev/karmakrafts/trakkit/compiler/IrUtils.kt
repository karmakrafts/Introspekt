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

import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression

internal fun getLineNumber(source: List<String>, startOffset: Int): Int {
    var currentOffset = 0
    for ((index, line) in source.withIndex()) {
        val lineLength = line.length
        if (currentOffset + lineLength >= startOffset) return index + 1
        currentOffset += lineLength + 1 // Include newline character
    }
    return 0
}

internal fun getColumnNumber(source: List<String>, startOffset: Int): Int {
    var currentOffset = 0
    for (line in source) {
        val lineLength = line.length
        if (currentOffset + lineLength >= startOffset) return startOffset - currentOffset + 1
        currentOffset += lineLength + 1 // Include newline character
    }
    return 0
}

internal fun getCallLocation( // @formatter:off
    module: IrModuleFragment,
    file: IrFile,
    source: List<String>,
    expression: IrFunctionAccessExpression,
    function: IrSimpleFunction?
): SourceLocation { // @formatter:on
    return SourceLocation(
        module.name.asString(),
        file.path,
        function?.name?.asString() ?: "unknown",
        getLineNumber(source, expression.startOffset),
        getColumnNumber(source, expression.startOffset)
    )
}

internal fun getFunctionLocation( // @formatter:off
    module: IrModuleFragment,
    file: IrFile,
    source: List<String>,
    function: IrSimpleFunction?
): SourceLocation { // @formatter:on
    return SourceLocation(
        module.name.asString(),
        file.path,
        function?.name?.asString() ?: "unknown",
        function?.let { getLineNumber(source, it.startOffset) } ?: 0,
        function?.let { getColumnNumber(source, it.startOffset) } ?: 0)
}