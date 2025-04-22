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

package dev.karmakrafts.trakkit.compiler

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.types.getClass

internal class ClassInfoTransformer(
    val pluginContext: TrakkitPluginContext,
    val moduleFragment: IrModuleFragment,
    val file: IrFile,
    val source: List<String>
) : TrakkitIntrinsicTransformer(
    setOf( // @formatter:off
        TrakkitIntrinsic.CI_CURRENT,
        TrakkitIntrinsic.CI_OF
    ) // @formatter:on
) {
    override fun visitIntrinsic(
        type: TrakkitIntrinsic, expression: IrCall, context: IntrinsicContext
    ): IrElement = with(pluginContext) {
        when (type) {
            TrakkitIntrinsic.CI_CURRENT -> requireNotNull(context.clazz) {
                "Not inside any class"
            }.getClassInfo(moduleFragment, file, source).instantiate()

            TrakkitIntrinsic.CI_OF -> requireNotNull(expression.typeArguments.first()?.getClass()) {
                "Missing class type parameter"
            }.getClassInfo(moduleFragment, file, source).instantiate()

            else -> error("Unsupported intrinsic for ClassInfoTransformer")
        }
    }
}