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
import dev.karmakrafts.iridium.CompilerTestScope
import dev.karmakrafts.iridium.matcher.IrElementMatcher
import dev.karmakrafts.iridium.pipeline.addJvmClasspathRootByType
import dev.karmakrafts.iridium.pipeline.defaultPipelineSpec
import io.kotest.matchers.shouldBe
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.target

internal fun CompilerTestScope.introspektPipeline(moduleName: String = "test") {
    pipeline {
        defaultPipelineSpec(moduleName)
        config {
            addJvmClasspathRootByType<IntrospektIntrinsic>()
        }
    }
}

internal fun CompilerTestScope.introspektTransformerPipeline(moduleName: String = "test") {
    introspektPipeline(moduleName)
    pipeline {
        irExtension(IntrospektIrGenerationExtension { sourceLines })
    }
}

internal fun IrElementMatcher<out IrCall>.isCachedSourceLocation( // @formatter:off
    module: String,
    file: String,
    line: Int,
    column: Int
) { // @formatter:on
    val function = element.target
    function.kotlinFqName shouldBe IntrospektNames.SourceLocation.Companion.getOrCreate.asSingleFqName()

    val moduleParam = function.valueParameters.first { it.name.asString() == "module" }
    moduleParam.type matches { type("kotlin/String") }
    val moduleArg = element.arguments[moduleParam.indexInParameters]!!
    moduleArg::class shouldBe IrConstImpl::class
    (moduleArg as IrConstImpl).value shouldBe module

    val fileParam = function.valueParameters.first { it.name.asString() == "file" }
    fileParam.type matches { type("kotlin/String") }
    val fileArg = element.arguments[moduleParam.indexInParameters]!!
    fileArg::class shouldBe IrConstImpl::class
    (fileArg as IrConstImpl).value shouldBe file

    val lineParam = function.valueParameters.first { it.name.asString() == "line" }
    lineParam.type matches { int() }
    val lineArg = element.arguments[lineParam.indexInParameters]!!
    lineArg::class shouldBe IrConstImpl::class
    (lineArg as IrConstImpl).value shouldBe line

    val columnParam = function.valueParameters.first { it.name.asString() == "column" }
    columnParam.type matches { int() }
    val columnArg = element.arguments[columnParam.indexInParameters]!!
    columnArg::class shouldBe IrConstImpl::class
    (columnArg as IrConstImpl).value shouldBe column
}