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

package dev.karmakrafts.introspekt.compiler.transformer

import dev.karmakrafts.introspekt.compiler.element.getFunctionInfo
import dev.karmakrafts.introspekt.compiler.util.TraceType
import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrReturnableBlock
import org.jetbrains.kotlin.ir.expressions.IrSuspensionPoint
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.types.isNullableNothing
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.isAnonymousObject
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.setDeclarationsParent
import org.jetbrains.kotlin.ir.util.target
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.name.Name
import java.util.*

internal class TraceInjectionTransformer : TraceTransformer() {
    companion object : GeneratedDeclarationKey() {
        private val declOrigin: IrDeclarationOrigin = IrDeclarationOrigin.GeneratedByPlugin(this)
        private val callTypes: EnumSet<TraceType> = EnumSet.of(TraceType.BEFORE_CALL, TraceType.AFTER_CALL)
        private val functionTypes: EnumSet<TraceType> = EnumSet.of(TraceType.FUNCTION_ENTER, TraceType.FUNCTION_LEAVE)
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrCall.withCallee(callee: IrFunctionAccessExpression, data: TraceContext): IrCall {
        val context = data.pluginContext
        val moduleFragment = context.irModule
        val file = context.irFile
        val source = context.source
        arguments[symbol.owner.parameters.single { it.name.asString() == "callee" }] =
            callee.target.getFunctionInfo(moduleFragment, file, source)
                .instantiateCached(moduleFragment, file, source, context)
        return this
    }

    private fun transformSimpleCall(call: IrFunctionAccessExpression, data: TraceContext): IrElement {
        return IrCompositeImpl( // @formatter:off
            startOffset = SYNTHETIC_OFFSET,
            endOffset = SYNTHETIC_OFFSET,
            type = call.type
        ).apply { // @formatter:on
            statements += TraceType.BEFORE_CALL.createCall(data.pluginContext).withCallee(call, data)
            statements += call
        }
    }

    private fun transformComplexCall(
        parent: IrDeclarationParent, call: IrFunctionAccessExpression, hasBefore: Boolean, data: TraceContext
    ): IrElement {
        val resultType = call.type
        if (resultType.isUnit() || resultType.isNothing() || resultType.isNullableNothing()) {
            // If the call result is a Unit or Nothing, we can omit storing the result
            return IrCompositeImpl( // @formatter:off
                startOffset = SYNTHETIC_OFFSET,
                endOffset = SYNTHETIC_OFFSET,
                type = data.pluginContext.irBuiltIns.unitType
            ).apply { // @formatter:on
                statements += TraceType.BEFORE_CALL.createCall(data.pluginContext).withCallee(call, data)
                statements += call
                statements += TraceType.AFTER_CALL.createCall(data.pluginContext).withCallee(call, data)
            }
        }
        val result = IrVariableImpl(
            startOffset = SYNTHETIC_OFFSET,
            endOffset = SYNTHETIC_OFFSET,
            origin = declOrigin,
            symbol = IrVariableSymbolImpl(),
            name = Name.identifier("__result_${call.hashCode()}__"),
            type = resultType,
            isVar = false,
            isConst = false,
            isLateinit = false
        ).apply {
            initializer = call
            setDeclarationsParent(parent)
        }
        return IrBlockImpl( // @formatter:off
            startOffset = SYNTHETIC_OFFSET,
            endOffset = SYNTHETIC_OFFSET,
            type = resultType
        ).apply { // @formatter:on
            if (hasBefore) statements += TraceType.BEFORE_CALL.createCall(data.pluginContext).withCallee(call, data)
            statements += result
            statements += TraceType.AFTER_CALL.createCall(data.pluginContext).withCallee(call, data)
            statements += IrGetValueImpl( // @formatter:off
                startOffset = SYNTHETIC_OFFSET,
                endOffset = SYNTHETIC_OFFSET,
                symbol = result.symbol
            ) // @formatter:on
        }
    }

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression, data: TraceContext): IrElement {
        if ((expression is IrConstructorCall && expression.target.parentClassOrNull?.isAnonymousObject == true)) return expression
        val transformedCall = super.visitFunctionAccess(expression, data)
        if (transformedCall is IrCall) {
            val allowedTraceTypes = data.traceType?.intersect(callTypes) ?: return transformedCall
            if (allowedTraceTypes.isEmpty()) return transformedCall
            val parent = data.declParentOrNull ?: return transformedCall
            return if (TraceType.AFTER_CALL !in allowedTraceTypes) transformSimpleCall(transformedCall, data)
            else transformComplexCall(parent, transformedCall, TraceType.BEFORE_CALL in allowedTraceTypes, data)
        }
        return transformedCall
    }

    private inline fun IrStatement.injectBeforeReturns(
        data: TraceContext, crossinline statements: () -> List<IrStatement>
    ): IrStatement {
        return transform(object : IrTransformer<TraceContext>() {
            override fun visitFunction(declaration: IrFunction, data: TraceContext): IrStatement {
                data.returnTargetStack.push(declaration.symbol)
                val transformedFunction = super.visitFunction(declaration, data)
                data.returnTargetStack.pop()
                return transformedFunction
            }

            override fun visitReturnableBlock(expression: IrReturnableBlock, data: TraceContext): IrExpression {
                data.returnTargetStack.push(expression.symbol)
                val transformedBlock = super.visitReturnableBlock(expression, data)
                data.returnTargetStack.pop()
                return transformedBlock
            }

            override fun visitReturn(expression: IrReturn, data: TraceContext): IrExpression {
                val transformedReturn = super.visitReturn(expression, data)
                if (transformedReturn is IrReturn) {
                    val composite = IrCompositeImpl(
                        startOffset = SYNTHETIC_OFFSET, endOffset = SYNTHETIC_OFFSET, type = transformedReturn.type
                    ).apply {
                        this.statements += statements()
                        this.statements += transformedReturn.value // Unfold expression from original return
                    }
                    return IrReturnImpl(
                        startOffset = SYNTHETIC_OFFSET,
                        endOffset = SYNTHETIC_OFFSET,
                        type = composite.type,
                        returnTargetSymbol = data.returnTargetOrNull!!,
                        value = composite
                    )
                }
                return transformedReturn
            }
        }, data) as IrStatement
    }

    private fun transformFunctionStatements( // @formatter:off
        function: IrFunction,
        statements: List<IrStatement>,
        traceTypes: Set<TraceType>,
        data: TraceContext
    ): List<IrStatement> { // @formatter:on
        val newStatements = ArrayList<IrStatement>()
        if (TraceType.FUNCTION_ENTER in traceTypes) {
            newStatements += TraceType.FUNCTION_ENTER.createCall(data.pluginContext)
        }
        newStatements += statements.map {
            it.injectBeforeReturns(data) {
                listOf(TraceType.FUNCTION_LEAVE.createCall(data.pluginContext))
            }
        }
        // If this is a unit function, we expect there to be an implicit return at the end of function scope
        if (function.returnType.isUnit()) {
            newStatements += TraceType.FUNCTION_LEAVE.createCall(data.pluginContext)
        }
        return newStatements
    }

    override fun visitFunction(declaration: IrFunction, data: TraceContext): IrStatement {
        data.returnTargetStack.push(declaration.symbol)
        val transformedFunction = super.visitFunction(declaration, data)
        data.returnTargetStack.pop()
        return transformedFunction
    }

    override fun visitReturnableBlock(expression: IrReturnableBlock, data: TraceContext): IrExpression {
        data.returnTargetStack.push(expression.symbol)
        val transformedBlock = super.visitReturnableBlock(expression, data)
        data.returnTargetStack.pop()
        return transformedBlock
    }

    override fun visitSuspensionPoint(expression: IrSuspensionPoint, data: TraceContext): IrExpression {
        val transformedPoint = super.visitSuspensionPoint(expression, data)
        if (transformedPoint is IrSuspensionPoint) {
            return IrCompositeImpl(
                startOffset = SYNTHETIC_OFFSET, endOffset = SYNTHETIC_OFFSET, type = expression.type
            ).apply {
                statements += TraceType.SUSPENSION_POINT.createCall(data.pluginContext)
                statements += expression
            }
        }
        return transformedPoint
    }

    override fun visitTraceableFunction(declaration: IrFunction, data: TraceContext) {
        val allowedTraceTypes = data.traceType?.intersect(functionTypes) ?: return
        if (allowedTraceTypes.isEmpty()) return // If we don't have any allowed tracing in the current scope, return
        when (val body = declaration.body) {
            // When we have an expression body, we need to turn it into a block-body to house multiple statements
            is IrExpressionBody -> {
                val originalExpression = body.expression
                body.expression = IrBlockImpl(
                    startOffset = SYNTHETIC_OFFSET, endOffset = SYNTHETIC_OFFSET, type = originalExpression.type
                ).apply {
                    // If we don't have anything to return, we don't need to create a temporary
                    if (type.isUnit()) {
                        statements += TraceType.FUNCTION_ENTER.createCall(data.pluginContext)
                        statements += originalExpression
                        statements += TraceType.FUNCTION_LEAVE.createCall(data.pluginContext)
                        return@apply
                    }
                    // Otherwise we need to create a new variable to hold the result until the end of the composite
                    if (TraceType.FUNCTION_ENTER in allowedTraceTypes) {
                        statements += TraceType.FUNCTION_ENTER.createCall(data.pluginContext)
                    }
                    val result = IrVariableImpl(
                        startOffset = SYNTHETIC_OFFSET,
                        endOffset = SYNTHETIC_OFFSET,
                        origin = declOrigin,
                        symbol = IrVariableSymbolImpl(),
                        name = Name.identifier("__result_${originalExpression.hashCode()}__"),
                        type = originalExpression.type,
                        isVar = false,
                        isConst = false,
                        isLateinit = false
                    ).apply {
                        initializer = originalExpression
                    }
                    statements += result
                    if (TraceType.FUNCTION_LEAVE in allowedTraceTypes) {
                        statements += TraceType.FUNCTION_LEAVE.createCall(data.pluginContext)
                    }
                    statements += IrGetValueImpl(
                        startOffset = SYNTHETIC_OFFSET, endOffset = SYNTHETIC_OFFSET, symbol = result.symbol
                    )
                    patchDeclarationParents(declaration)
                }
            }
            // When we have a block body in place already, we can just clear its statement list and re-add everything
            is IrBlockBody -> {
                val transformedStatements = transformFunctionStatements(
                    declaration, body.statements, allowedTraceTypes, data
                )
                body.statements.clear()
                body.statements.addAll(transformedStatements)
            }
            // Otherwise do nothing to the function
            else -> {}
        }
    }
}