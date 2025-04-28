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

import dev.karmakrafts.introspekt.compiler.transformer.ClassInfoTransformer
import dev.karmakrafts.introspekt.compiler.transformer.CompilerApiTransformer
import dev.karmakrafts.introspekt.compiler.transformer.FunctionInfoTransformer
import dev.karmakrafts.introspekt.compiler.transformer.IntrinsicCalleeParameterTransformer
import dev.karmakrafts.introspekt.compiler.transformer.IntrinsicCallerParameterTransformer
import dev.karmakrafts.introspekt.compiler.transformer.IntrinsicContext
import dev.karmakrafts.introspekt.compiler.transformer.SourceLocationTransformer
import dev.karmakrafts.introspekt.compiler.transformer.TraceContext
import dev.karmakrafts.introspekt.compiler.transformer.TraceTransformer
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import kotlin.io.path.Path
import kotlin.io.path.readLines

internal class IntrospektIrGenerationExtension : IrGenerationExtension {
    override fun generate(
        moduleFragment: IrModuleFragment, pluginContext: IrPluginContext
    ) {
        val introspektContext = IntrospektPluginContext(pluginContext)
        moduleFragment.acceptVoid(CompilerApiTransformer())
        for (file in moduleFragment.files) {
            val source = runCatching { Path(file.path).readLines() }.getOrNull() ?: continue
            val context = IntrinsicContext(introspektContext)
            file.acceptVoid(IntrinsicCalleeParameterTransformer(introspektContext))
            file.acceptVoid(IntrinsicCallerParameterTransformer(introspektContext))
            file.transform(SourceLocationTransformer(introspektContext, moduleFragment, file, source), context)
            file.transform(FunctionInfoTransformer(introspektContext, moduleFragment, file, source), context)
            file.transform(ClassInfoTransformer(introspektContext, moduleFragment, file, source), context)
        }
        moduleFragment.accept(TraceTransformer(), TraceContext(introspektContext))
    }
}