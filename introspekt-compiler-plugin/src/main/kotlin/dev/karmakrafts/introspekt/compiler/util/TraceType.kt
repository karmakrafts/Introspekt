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
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImplWithShape
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.util.target
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import java.util.*

internal enum class TraceType( // @formatter:off
    private val valueArgumentsCount: Int,
    val classId: ClassId,
    val functionName: Name
) { // @formatter:on
    // @formatter:off
    SPAN_ENTER      (4, IntrospektNames.TraceSpan.Companion.id,        IntrospektNames.Functions.enter),
    SPAN_LEAVE      (1, IntrospektNames.TraceSpan.Companion.id,        IntrospektNames.Functions.leave),
    FUNCTION_ENTER  (1, IntrospektNames.TraceCollector.Companion.id,   IntrospektNames.Functions.enterFunction),
    FUNCTION_LEAVE  (1, IntrospektNames.TraceCollector.Companion.id,   IntrospektNames.Functions.leaveFunction),
    CALL            (3, IntrospektNames.TraceCollector.Companion.id,   IntrospektNames.Functions.call),
    EVENT           (4, IntrospektNames.Trace.Companion.id,            IntrospektNames.Functions.event);
    // @formatter:on

    companion object {
        private val functionSymbolCache: EnumMap<TraceType, IrSimpleFunctionSymbol> = EnumMap(TraceType::class.java)
        private val classSymbolCache: EnumMap<TraceType, IrClassSymbol> = EnumMap(TraceType::class.java)
    }

    fun createCall(context: IntrospektPluginContext): IrCallImpl = with(context) {
        val symbol = functionSymbolCache.getOrPut(this@TraceType) {
            referenceFunctions(CallableId(classId, functionName)).first()
        }
        IrCallImplWithShape(
            startOffset = SYNTHETIC_OFFSET,
            endOffset = SYNTHETIC_OFFSET,
            type = irBuiltIns.unitType,
            symbol = symbol,
            typeArgumentsCount = 0,
            valueArgumentsCount = valueArgumentsCount,
            contextParameterCount = 0,
            hasDispatchReceiver = true,
            hasExtensionReceiver = false
        ).apply {
            dispatchReceiver = classSymbolCache.getOrPut(this@TraceType) {
                referenceClass(classId)!!
            }.getObjectInstance()
        }
    }
}

internal fun IrCall.getTraceType(): TraceType? {
    val function = target
    val functionName = function.name
    val parentClass = function.parentClassOrNull ?: return null
    val classId = parentClass.classId ?: return null
    return TraceType.entries.find { it.classId == classId && it.functionName == functionName }
}

internal fun IrAnnotationContainer.isTraceable(): Boolean = hasAnnotation(IntrospektNames.Trace.id)

internal fun IrAnnotationContainer.getTraceType(): List<TraceType> {
    return if (!isTraceable()) emptyList()
    else getAnnotationValues<TraceType>(IntrospektNames.Trace.fqName, "targets").filterNotNull()
}