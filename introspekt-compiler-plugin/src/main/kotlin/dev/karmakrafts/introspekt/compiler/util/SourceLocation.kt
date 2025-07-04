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
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImplWithShape
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.util.isTypeParameter
import org.jetbrains.kotlin.ir.util.toIrConst
import org.jetbrains.kotlin.it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET as KOTLIN_SYNTHETIC_OFFSET

internal data class SourceLocation( // @formatter:off
    val module: String,
    val file: String,
    val line: Int,
    val column: Int
) { // @formatter:on
    companion object {
        const val FAKE_OVERRIDE_OFFSET = -1
        const val SYNTHETIC_OFFSET = -2
        const val UNDEFINED_OFFSET = -3
        val undefined: SourceLocation = SourceLocation("<unknown>", "<unknown>", UNDEFINED_OFFSET, UNDEFINED_OFFSET)
        private val cache: Int2ObjectOpenHashMap<SourceLocation> = Int2ObjectOpenHashMap()

        private fun getCacheKey(
            module: String, file: String, line: Int, column: Int
        ): Int {
            var result = module.hashCode()
            result = 31 * result + file.hashCode()
            result = 31 * result + line
            result = 31 * result + column
            return result
        }

        fun getOrCreate(
            module: String, file: String, line: Int, column: Int
        ): SourceLocation = cache.getOrPut(getCacheKey(module, file, line, column)) {
            SourceLocation(module, file, line, column)
        }
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    fun instantiateCached(context: IntrospektPluginContext): IrCallImpl = with(context) {
        IrCallImplWithShape(
            startOffset = KOTLIN_SYNTHETIC_OFFSET,
            endOffset = KOTLIN_SYNTHETIC_OFFSET,
            type = introspektSymbols.sourceLocationType.defaultType,
            symbol = introspektSymbols.sourceLocationGetOrCreate,
            typeArgumentsCount = 0,
            valueArgumentsCount = 4,
            contextParameterCount = 0,
            hasDispatchReceiver = true,
            hasExtensionReceiver = false
        ).apply {
            val function = symbol.owner
            arguments[function.parameters.first { it.name.asString() == "module" }] =
                module.toIrConst(irBuiltIns.stringType)
            arguments[function.parameters.first { it.name.asString() == "file" }] =
                file.toIrConst(irBuiltIns.stringType)
            arguments[function.parameters.first { it.name.asString() == "line" }] = line.toIrConst(irBuiltIns.intType)
            arguments[function.parameters.first { it.name.asString() == "column" }] =
                column.toIrConst(irBuiltIns.intType)
            dispatchReceiver = introspektSymbols.sourceLocationCompanionType.getObjectInstance()
        }
    }

    fun createHashSum(context: IntrospektPluginContext): IrConst {
        return hashCode().toIrConst(context.irBuiltIns.intType)
    }
}

internal fun IrFunction.getFunctionLocation( // @formatter:off
    module: IrModuleFragment,
    file: IrFile,
    source: List<String>
): SourceLocation { // @formatter:on
    val isFakeOverride = isFakeOverride
    return SourceLocation.getOrCreate(
        module = module.name.getCleanName(),
        file = file.path.trimStart('/'),
        line = if (isFakeOverride) SourceLocation.FAKE_OVERRIDE_OFFSET
        else getLineNumber(source, startOffset, endOffset),
        column = if (isFakeOverride) SourceLocation.FAKE_OVERRIDE_OFFSET
        else getColumnNumber(source, startOffset, endOffset)
    )
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal fun IrType.getLocation( // @formatter:off
    module: IrModuleFragment,
    file: IrFile,
    source: List<String>,
): SourceLocation { // @formatter:on
    if (isTypeParameter()) {
        val symbol = classifierOrFail as IrTypeParameterSymbol
        val typeParameter = symbol.owner
        return SourceLocation.getOrCreate(
            module = module.name.getCleanName(),
            file = file.path.trimStart('/'),
            line = getLineNumber(source, typeParameter.startOffset, typeParameter.endOffset),
            column = getColumnNumber(source, typeParameter.startOffset, typeParameter.endOffset)
        )
    }
    val clazz = classOrNull?.owner
    return SourceLocation.getOrCreate(
        module = module.name.getCleanName(),
        file = file.path.trimStart('/'),
        line = if (clazz == null) SourceLocation.UNDEFINED_OFFSET
        else getLineNumber(source, clazz.startOffset, clazz.endOffset),
        column = if (clazz == null) SourceLocation.UNDEFINED_OFFSET
        else getColumnNumber(source, clazz.startOffset, clazz.endOffset)
    )
}

internal fun IrElement.getLocation( // @formatter:off
    module: IrModuleFragment,
    file: IrFile,
    source: List<String>,
): SourceLocation = SourceLocation.getOrCreate( // @formatter:on
    module = module.name.getCleanName(),
    file = file.path.trimStart('/'),
    line = getLineNumber(source, startOffset, endOffset),
    column = getColumnNumber(source, startOffset, endOffset)
)