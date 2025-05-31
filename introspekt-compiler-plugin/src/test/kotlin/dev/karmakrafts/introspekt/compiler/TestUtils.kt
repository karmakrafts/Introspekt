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

package dev.karmakrafts.introspekt.compiler

import dev.karmakrafts.introspekt.IntrospektIntrinsic
import dev.karmakrafts.introspekt.compiler.util.IntrospektNames
import dev.karmakrafts.iridium.CompilerAssertionDsl
import dev.karmakrafts.iridium.CompilerTestDsl
import dev.karmakrafts.iridium.CompilerTestScope
import dev.karmakrafts.iridium.matcher.IrElementMatcher
import dev.karmakrafts.iridium.matcher.IrTypeMatcher
import dev.karmakrafts.iridium.pipeline.addJvmClasspathRootByType
import dev.karmakrafts.iridium.pipeline.defaultPipelineSpec
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.util.target

@CompilerTestDsl
internal fun CompilerTestScope.introspektPipeline(moduleName: String = "test") {
    pipeline {
        defaultPipelineSpec(moduleName)
        config {
            addJvmClasspathRootByType<IntrospektIntrinsic>()
        }
    }
}

@CompilerTestDsl
internal fun CompilerTestScope.introspektTransformerPipeline(moduleName: String = "test") {
    introspektPipeline(moduleName)
    pipeline {
        irExtension(IntrospektIrGenerationExtension { sourceLines })
    }
}

@CompilerAssertionDsl
@Suppress("NOTHING_TO_INLINE")
internal inline fun IrElementMatcher<out IrCall>.isCachedSourceLocation( // @formatter:off
    module: String,
    file: String,
    line: Int,
    column: Int
) { // @formatter:on
    val function = element.target
    function.kotlinFqName shouldBe IntrospektNames.SourceLocation.Companion.getOrCreate.asSingleFqName()

    val moduleParam = function.valueParameters.first { it.name.asString() == "module" }
    moduleParam.type matches { string() }
    val moduleArg = element.arguments[moduleParam]!!
    moduleArg::class shouldBe IrConstImpl::class
    (moduleArg as IrConstImpl).value shouldBe module

    val fileParam = function.valueParameters.first { it.name.asString() == "file" }
    fileParam.type matches { string() }
    val fileArg = element.arguments[moduleParam]!!
    fileArg::class shouldBe IrConstImpl::class
    (fileArg as IrConstImpl).value shouldBe file

    val lineParam = function.valueParameters.first { it.name.asString() == "line" }
    lineParam.type matches { int() }
    val lineArg = element.arguments[lineParam]!!
    lineArg::class shouldBe IrConstImpl::class
    (lineArg as IrConstImpl).value shouldBe line

    val columnParam = function.valueParameters.first { it.name.asString() == "column" }
    columnParam.type matches { int() }
    val columnArg = element.arguments[columnParam]!!
    columnArg::class shouldBe IrConstImpl::class
    (columnArg as IrConstImpl).value shouldBe column
}

@CompilerAssertionDsl
internal inline fun IrElementMatcher<out IrCall>.isCachedTypeInfo( // @formatter:off
    module: String,
    file: String,
    line: Int,
    column: Int,
    qualifiedName: String,
    name: String,
    crossinline typeMatcher: IrTypeMatcher<IrType>.() -> Unit
) { // @formatter:on
    val function = element.target
    function.kotlinFqName shouldBe IntrospektNames.TypeInfo.Companion.getOrCreate.asSingleFqName()

    val locationParam = function.parameters.first { it.name.asString() == "location" }
    locationParam.type matches { type(IntrospektNames.SourceLocation.id) }
    val locationArg = element.arguments[locationParam]!!
    locationArg::class shouldBe IrCallImpl::class
    (locationArg as IrCallImpl) matches { isCachedSourceLocation(module, file, line, column) }

    val reflectTypeParam = function.parameters.first { it.name.asString() == "reflectType" }
    reflectTypeParam.type matches { type("kotlin/reflect/KClass") }
    val reflectTypeArg = element.arguments[reflectTypeParam]!!
    reflectTypeArg::class shouldBe IrClassReferenceImpl::class
    (reflectTypeArg as IrClassReferenceImpl).type matches typeMatcher

    val qualifiedNameParam = function.parameters.first { it.name.asString() == "qualifiedName" }
    qualifiedNameParam.type matches { string() }
    val qualifiedNameArg = element.arguments[qualifiedNameParam]!!
    qualifiedNameArg::class shouldBe IrConstImpl::class
    (qualifiedNameArg as IrConstImpl).value shouldBe qualifiedName

    val nameParam = function.parameters.first { it.name.asString() == "name" }
    nameParam.type matches { string() }
    val nameArg = element.arguments[nameParam]!!
    nameArg::class shouldBe IrConstImpl::class
    (nameArg as IrConstImpl).value shouldBe name
}

@CompilerAssertionDsl
internal fun IrElementMatcher<out IrCall>.isCachedFunctionInfo( // @formatter:off
    module: String,
    file: String,
    line: Int,
    column: Int,
    qualifiedName: String,
    name: String
) {// @formatter:on
    val function = element.target
    function.kotlinFqName shouldBe IntrospektNames.FunctionInfo.Companion.getOrCreate.asSingleFqName()

    val locationParam = function.parameters.first { it.name.asString() == "location" }
    locationParam.type matches { type(IntrospektNames.SourceLocation.id) }
    val locationArg = element.arguments[locationParam]!!
    locationArg::class shouldBe IrCallImpl::class
    (locationArg as IrCallImpl) matches { isCachedSourceLocation(module, file, line, column) }

    val qualifiedNameParam = function.parameters.first { it.name.asString() == "qualifiedName" }
    qualifiedNameParam.type matches { string() }
    val qualifiedNameArg = element.arguments[qualifiedNameParam]!!
    qualifiedNameArg::class shouldBe IrConstImpl::class
    (qualifiedNameArg as IrConstImpl).value shouldBe qualifiedName

    val nameParam = function.parameters.first { it.name.asString() == "name" }
    nameParam.type matches { string() }
    val nameArg = element.arguments[nameParam]!!
    nameArg::class shouldBe IrConstImpl::class
    (nameArg as IrConstImpl).value shouldBe name
}

@CompilerAssertionDsl
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal inline fun IrElementMatcher<out IrCall>.isCachedClassInfo( // @formatter:off
    module: String,
    file: String,
    line: Int,
    column: Int,
    qualifiedName: String,
    name: String,
    properties: Map<String, IrType> = emptyMap(),
    functions: Map<String, List<IrType>> = emptyMap(),
    crossinline typeMatcher: IrTypeMatcher<IrType>.() -> Unit
) { // @formatter:on
    val function = element.target
    function.kotlinFqName shouldBe IntrospektNames.ClassInfo.Companion.getOrCreate.asSingleFqName()

    val typeParam = function.parameters.first { it.name.asString() == "type" }
    typeParam.type matches { type(IntrospektNames.TypeInfo.id) }
    val typeArg = element.arguments[typeParam]!!
    typeArg::class shouldBe IrCallImpl::class

    val typeCall = typeArg as IrCallImpl
    typeCall matches { isCachedTypeInfo(module, file, line, column, qualifiedName, name, typeMatcher) }
    val typeFunction = typeCall.target
    val reflectTypeParam = typeFunction.parameters.first { it.name.asString() == "reflectType" }
    val reflectArg = typeCall.arguments[reflectTypeParam]!!
    reflectArg::class shouldBe IrClassReferenceImpl::class
    val underlyingType = (reflectArg as IrClassReferenceImpl).classType

    val clazz = underlyingType.getClass()
    clazz shouldNotBe null
    if (properties.isNotEmpty()) {
        for (property in clazz!!.properties) {
            (property.backingField?.type ?: property.getter?.returnType
            ?: continue) shouldBe properties[property.name.asString()]
        }
    }
    if (functions.isNotEmpty()) {
        val namedFunctions = HashMap<String, ArrayList<IrFunction>>()
        for (function in clazz!!.functions) {
            namedFunctions.getOrPut(function.name.asString()) { ArrayList() } += function
        }
        for ((name, types) in functions) {
            var targetFunction: IrFunction? = null
            for (candidate in namedFunctions[name]!!) {
                if (candidate.returnType != types.first()) continue
                val paramTypes = types.drop(1)
                // @formatter:off
                if (candidate.parameters
                    .filter { it.kind == IrParameterKind.Regular }
                    .map { it.type } != paramTypes) continue
                // @formatter:on
                targetFunction = candidate
                break
            }
            targetFunction shouldNotBe null
        }
    }
}