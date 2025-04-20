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
import org.jetbrains.kotlin.ir.util.getAnnotationArgumentValue
import org.jetbrains.kotlin.ir.util.hasAnnotation

internal fun IrFunction.getIntrinsicType(): TrakkitIntrinsic? {
    if (!hasAnnotation(TrakkitNames.TrakkitIntrinsic.id)) return null
    val intrinsicName = getAnnotationArgumentValue<String>(TrakkitNames.TrakkitIntrinsic.fqName, "value") ?: return null
    return TrakkitIntrinsic.byName(intrinsicName)
}

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

internal fun getCallLocation(
    module: IrModuleFragment,
    file: IrFile,
    source: List<String>,
    expression: IrFunctionAccessExpression,
    function: IrFunction?
): SourceLocation = SourceLocation(
    module = module.name.asString(),
    file = file.path,
    function = function?.name?.asString() ?: "unknown",
    line = getLineNumber(source, expression.startOffset),
    column = getColumnNumber(source, expression.startOffset)
)

internal fun getFunctionLocation( // @formatter:off
    module: IrModuleFragment,
    file: IrFile,
    source: List<String>,
    function: IrFunction
): SourceLocation = SourceLocation( // @formatter:on
    module = module.name.asString(),
    file = file.path,
    function = function.name.asString(),
    line = getLineNumber(source, function.startOffset),
    column = getColumnNumber(source, function.startOffset)
)

internal fun getClassLocation( // @formatter:off
    module: IrModuleFragment,
    file: IrFile,
    source: List<String>,
    clazz: IrClass
): SourceLocation = SourceLocation( // @formatter:on
    module = module.name.asString(),
    file = file.path,
    function = "",
    line = getLineNumber(source, clazz.startOffset),
    column = getColumnNumber(source, clazz.startOffset)
)