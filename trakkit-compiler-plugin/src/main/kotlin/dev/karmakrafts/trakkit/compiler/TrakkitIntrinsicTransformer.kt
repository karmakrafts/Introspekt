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
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import kotlin.io.path.Path
import kotlin.io.path.readLines

internal class TrakkitIntrinsicTransformer(
    pluginContext: IrPluginContext,
    val moduleFragment: IrModuleFragment,
    val file: IrFile
) : IrElementTransformer<IrSimpleFunction?> {
    val irBuiltIns: IrBuiltIns = pluginContext.irBuiltIns
    val constructor: IrConstructorSymbol = pluginContext.referenceConstructors(TrakkitNames.SourceLocation.id).first()

    override fun visitElement(element: IrElement, data: IrSimpleFunction?): IrElement {
        element.transformChildren(this, data)
        return element
    }

    override fun visitSimpleFunction(
        declaration: IrSimpleFunction,
        data: IrSimpleFunction?
    ): IrStatement {
        return super.visitSimpleFunction(declaration, declaration) // Pass down the parent function
    }

    private fun getLineNumber(source: List<String>, startOffset: Int): Int {
        var currentOffset = 0
        for ((index, line) in source.withIndex()) {
            val lineLength = line.length
            if (currentOffset + lineLength >= startOffset) return index + 1
            currentOffset += lineLength + 1 // Include newline character
        }
        return 0
    }

    private fun getColumnNumber(source: List<String>, startOffset: Int): Int {
        var currentOffset = 0
        for (line in source) {
            val lineLength = line.length
            if (currentOffset + lineLength >= startOffset) return startOffset - currentOffset + 1
            currentOffset += lineLength + 1 // Include newline character
        }
        return 0
    }

    private fun getHereLocation(expression: IrCall, data: IrSimpleFunction?, source: List<String>?): SourceLocation {
        return SourceLocation(
            moduleFragment.name.asString(),
            file.path,
            data?.name?.asString() ?: "unknown",
            source?.let { getLineNumber(it, expression.startOffset) } ?: 0,
            source?.let { getColumnNumber(it, expression.startOffset) } ?: 0
        )
    }

    private fun getCurrentFunctionLocation(data: IrSimpleFunction?, source: List<String>?): SourceLocation {
        return SourceLocation(
            moduleFragment.name.asString(),
            file.path,
            data?.name?.asString() ?: "unknown",
            source?.let { src -> data?.let { getLineNumber(src, it.startOffset) } } ?: 0,
            source?.let { src -> data?.let { getColumnNumber(src, it.startOffset) } } ?: 0
        )
    }

    private fun SourceLocation.createConstructorCall(expression: IrCall): IrElement {
        return IrConstructorCallImpl(
            startOffset = SYNTHETIC_OFFSET,
            endOffset = SYNTHETIC_OFFSET,
            type = expression.type,
            symbol = constructor,
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

    private fun SourceLocation.createHashSum(): IrElement {
        return hashCode().toIrConst(irBuiltIns.intType)
    }

    override fun visitCall(expression: IrCall, data: IrSimpleFunction?): IrElement {
        val function = expression.target

        if (!function.hasAnnotation(TrakkitNames.TrakkitIntrinsic.id)) return super.visitCall(expression, data)
        val intrinsicName = function.getAnnotationArgumentValue<String>(TrakkitNames.TrakkitIntrinsic.fqName, "value")
            ?: return super.visitCall(expression, data)
        val intrinsicType = TrakkitIntrinsics.byName(intrinsicName)
            ?: return super.visitCall(expression, data)

        // companion object {} if matched
        val companionObject = function.parentClassOrNull
            ?: return super.visitCall(expression, data)

        // class SourceLocation {} if matched
        val parentClass = companionObject.parentClassOrNull
            ?: return expression
        if (parentClass.kotlinFqName != TrakkitNames.SourceLocation.fqName) return super.visitCall(expression, data)

        val source = runCatching { Path(file.path).readLines() }.getOrNull()

        return when (intrinsicType) { // @formatter:off
            TrakkitIntrinsics.HERE ->
                getHereLocation(expression, data, source).createConstructorCall(expression)
            TrakkitIntrinsics.HERE_HASH ->
                getHereLocation(expression, data, source).createHashSum()
            TrakkitIntrinsics.CURRENT_FUNCTION ->
                getCurrentFunctionLocation(data, source).createConstructorCall(expression)
            TrakkitIntrinsics.CURRENT_FUNCTION_HASH ->
                getCurrentFunctionLocation(data, source).createHashSum()
        } // @formatter:on
    }
}