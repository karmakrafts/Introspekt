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
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.CallableId

internal enum class IntrinsicResultType( // @formatter:off
    private val typeProvider: (IntrospektPluginContext) -> IrType
) { // @formatter:on
    // @formatter:off
    SOURCE_LOCATION ({ it.sourceLocationType.defaultType }),
    FUNCTION_INFO   ({ it.functionInfoType.defaultType }),
    CLASS_INFO      ({ it.classInfoType.defaultType }),
    TYPE_INFO       ({ it.typeInfoType.defaultType }),
    INT             ({ it.irBuiltIns.intType });
    // @formatter:on

    operator fun invoke(context: IntrospektPluginContext): IrType = typeProvider(context)
}

internal enum class IntrospektIntrinsic( // @formatter:off
    val supportsInlining: Boolean,
    val resultType: IntrinsicResultType,
    val functionId: CallableId
) { // @formatter:on
    // @formatter:off
    SL_HERE                 (true,  IntrinsicResultType.SOURCE_LOCATION,    IntrospektNames.SourceLocation.Companion.here),
    SL_HERE_HASH            (true,  IntrinsicResultType.INT,                IntrospektNames.SourceLocation.Companion.hereHash),
    SL_CURRENT_FUNCTION     (true,  IntrinsicResultType.SOURCE_LOCATION,    IntrospektNames.SourceLocation.Companion.currentFunction),
    SL_CURRENT_FUNCTION_HASH(true,  IntrinsicResultType.INT,                IntrospektNames.SourceLocation.Companion.currentFunctionHash),
    SL_CURRENT_CLASS        (true,  IntrinsicResultType.SOURCE_LOCATION,    IntrospektNames.SourceLocation.Companion.currentClass),
    SL_CURRENT_CLASS_HASH   (true,  IntrinsicResultType.INT,                IntrospektNames.SourceLocation.Companion.currentClassHash),
    SL_OF_CLASS             (false, IntrinsicResultType.SOURCE_LOCATION,    IntrospektNames.SourceLocation.Companion.ofClass),
    SL_OF_FUNCTION          (false, IntrinsicResultType.SOURCE_LOCATION,    IntrospektNames.SourceLocation.Companion.ofFunction),
    FI_CURRENT              (true,  IntrinsicResultType.FUNCTION_INFO,      IntrospektNames.FunctionInfo.Companion.current),
    FI_OF                   (false, IntrinsicResultType.FUNCTION_INFO,      IntrospektNames.FunctionInfo.Companion.of),
    CI_CURRENT              (true,  IntrinsicResultType.CLASS_INFO,         IntrospektNames.ClassInfo.Companion.current),
    CI_OF                   (false, IntrinsicResultType.CLASS_INFO,         IntrospektNames.ClassInfo.Companion.of),
    TI_OF                   (false, IntrinsicResultType.TYPE_INFO,          IntrospektNames.TypeInfo.Companion.of);
    // @formatter:on

    private fun IntrospektPluginContext.getType(): IrType = resultType(this)

    private fun IntrospektPluginContext.getSymbol(): IrSimpleFunctionSymbol {
        return referenceFunctions(functionId).first()
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    fun createCall( // @formatter:off
        context: IntrospektPluginContext,
        startOffset: Int = SYNTHETIC_OFFSET,
        endOffset: Int = SYNTHETIC_OFFSET
    ): IrCallImpl = with(context) { // @formatter:on
        IrCallImpl( // @formatter:off
            startOffset = startOffset,
            endOffset = endOffset,
            type = getType(),
            symbol = getSymbol()
        ).apply { // @formatter:on
            functionId.classId?.let(::referenceClass)?.let { classSymbol ->
                check(classSymbol.owner.isCompanion) { "Intrinsic parent must be a companion object or the top level scope" }
                dispatchReceiver = classSymbol.getObjectInstance()
            }
        }
    }
}

internal fun IrFunction.getIntrinsicType(): IntrospektIntrinsic? {
    if (!hasAnnotation(IntrospektNames.IntrospektIntrinsic.id)) return null
    return getAnnotationValue<IntrospektIntrinsic>(IntrospektNames.IntrospektIntrinsic.fqName, "type")
}