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
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImplWithShape
import org.jetbrains.kotlin.ir.types.defaultType
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

    fun instantiateCached(context: IntrospektPluginContext): IrCallImpl = with(context) {
        IrCallImplWithShape(
            startOffset = KOTLIN_SYNTHETIC_OFFSET,
            endOffset = KOTLIN_SYNTHETIC_OFFSET,
            type = sourceLocationType.defaultType,
            symbol = sourceLocationGetOrCreate,
            typeArgumentsCount = 0,
            valueArgumentsCount = 4,
            contextParameterCount = 0,
            hasDispatchReceiver = true,
            hasExtensionReceiver = false
        ).apply {
            var index = 0
            putValueArgument(index++, module.toIrConst(irBuiltIns.stringType))
            putValueArgument(index++, file.toIrConst(irBuiltIns.stringType))
            putValueArgument(index++, line.toIrConst(irBuiltIns.intType))
            putValueArgument(index, column.toIrConst(irBuiltIns.intType))
            dispatchReceiver = sourcLocationCompanionType.getObjectInstance()
        }
    }

    fun createHashSum(context: IntrospektPluginContext): IrConst {
        return hashCode().toIrConst(context.irBuiltIns.intType)
    }
}