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
import dev.karmakrafts.introspekt.compiler.element.LocalInfo
import dev.karmakrafts.introspekt.compiler.element.getLocalInfo
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImplWithShape
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.utils.filterIsInstanceAnd

internal data class FrameSnapshot( // @formatter:off
    val location: SourceLocation,
    val locals: Map<LocalInfo, IrVariable>
) { // @formatter:on
    companion object {
        @OptIn(UnsafeDuringIrConstructionAPI::class)
        fun empty(context: IntrospektPluginContext): IrCallImpl = with(context) {
            IrCallImplWithShape(
                startOffset = SYNTHETIC_OFFSET,
                endOffset = SYNTHETIC_OFFSET,
                type = frameSnapshotType.defaultType,
                symbol = frameSnapshotEmpty.owner.getter!!.symbol,
                typeArgumentsCount = 0,
                valueArgumentsCount = 0,
                contextParameterCount = 0,
                hasDispatchReceiver = true,
                hasExtensionReceiver = false
            ).apply {
                dispatchReceiver = frameSnapshotCompanionType.getObjectInstance()
            }
        }
    }

    fun instantiate( // @formatter:off
        context: IntrospektPluginContext
    ): IrConstructorCallImpl = with(context) { // @formatter:on
        IrConstructorCallImpl(
            startOffset = SYNTHETIC_OFFSET,
            endOffset = SYNTHETIC_OFFSET,
            type = frameSnapshotType.defaultType,
            symbol = frameSnapshotConstructor,
            typeArgumentsCount = 0,
            constructorTypeArgumentsCount = 0
        ).apply {
            var index = 0
            putValueArgument(index++, location.instantiateCached(context))
            putValueArgument(index, createMapOf( // @formatter:off
                keyType = localInfoType.defaultType,
                valueType = irBuiltIns.anyType.makeNullable(),
                values = locals.map { (local, variable) ->
                    local.instantiateCached(context) to IrGetValueImpl(
                        startOffset = SYNTHETIC_OFFSET,
                        endOffset = SYNTHETIC_OFFSET,
                        type = variable.type,
                        symbol = variable.symbol
                    )
                }
            )) // @formatter:on
        }
    }
}

internal inline fun IrFunction.createFrameSnapshot( // @formatter:off
    module: IrModuleFragment,
    file: IrFile,
    source: List<String>,
    crossinline captureUntil: (IrElement) -> Boolean = { false },
    crossinline filter: (IrVariable) -> Boolean = { true }
): FrameSnapshot { // @formatter:on
    return FrameSnapshot( // @formatter:off
        location = getFunctionLocation(module, file, source),
        locals = body
            ?.bfsFilterInstanceUntil<IrVariable>(captureUntil) { element -> filter(element) }
            ?.associateBy { it.getLocalInfo(module, file, source, this) }
            ?: emptyMap()
    ) // @formatter:on
}

internal inline fun IrAnonymousInitializer.createFrameSnapshot( // @formatter:off
    module: IrModuleFragment,
    file: IrFile,
    source: List<String>,
    crossinline captureUntil: (IrElement) -> Boolean = { false },
    crossinline filter: (IrVariable) -> Boolean = { true }
): FrameSnapshot { // @formatter:on
    return FrameSnapshot( // @formatter:off
        location = getLocation(module, file, source),
        locals = body
            .bfsFilterInstanceUntil<IrVariable>(captureUntil) { element -> filter(element) }
            .associateBy { it.getLocalInfo(module, file, source, this) }
    ) // @formatter:on
}