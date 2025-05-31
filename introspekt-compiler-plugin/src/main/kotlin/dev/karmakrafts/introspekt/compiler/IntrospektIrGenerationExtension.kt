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
import dev.karmakrafts.introspekt.compiler.transformer.FunctionInfoTransformer
import dev.karmakrafts.introspekt.compiler.transformer.InlineDefaultCalleeTransformer
import dev.karmakrafts.introspekt.compiler.transformer.InlineDefaultCallerTransformer
import dev.karmakrafts.introspekt.compiler.transformer.IntrinsicContext
import dev.karmakrafts.introspekt.compiler.transformer.SourceLocationTransformer
import dev.karmakrafts.introspekt.compiler.transformer.TraceContext
import dev.karmakrafts.introspekt.compiler.transformer.TraceInjectionTransformer
import dev.karmakrafts.introspekt.compiler.transformer.TraceRemovalTransformer
import dev.karmakrafts.introspekt.compiler.transformer.TypeInfoTransformer
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import kotlin.io.path.Path
import kotlin.io.path.readLines

internal class IntrospektIrGenerationExtension(
    private val sourceProvider: (String) -> List<String> = {
        runCatching { Path(it).readLines() }.getOrNull() ?: emptyList()
    }
) : IrGenerationExtension {
    override fun generate( // @formatter:off
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext
    ) { // @formatter:on
        val introspektContext = IntrospektPluginContext(pluginContext)
        val traceContext = TraceContext(introspektContext)

        val injectionTransformer = TraceInjectionTransformer()
        moduleFragment.accept(injectionTransformer, traceContext)
        injectionTransformer.injectCallbacks(introspektContext)

        val removalTransformer = TraceRemovalTransformer()
        moduleFragment.accept(removalTransformer, traceContext)
        removalTransformer.removeCalls()

        for (file in moduleFragment.files) {
            val source = sourceProvider(file.path)
            val context = IntrinsicContext(introspektContext)
            file.acceptVoid(InlineDefaultCalleeTransformer(introspektContext))
            file.acceptVoid(InlineDefaultCallerTransformer(introspektContext))
            // Reduce intrinsics in type dependence order
            file.transform(ClassInfoTransformer(introspektContext, moduleFragment, file, source), context)
            file.transform(FunctionInfoTransformer(introspektContext, moduleFragment, file, source), context)
            file.transform(TypeInfoTransformer(introspektContext, moduleFragment, file, source), context)
            file.transform(SourceLocationTransformer(introspektContext, moduleFragment, file, source), context)
        }
    }
}