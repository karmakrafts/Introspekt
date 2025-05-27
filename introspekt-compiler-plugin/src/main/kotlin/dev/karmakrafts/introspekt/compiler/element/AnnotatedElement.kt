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

package dev.karmakrafts.introspekt.compiler.element

import dev.karmakrafts.introspekt.compiler.IntrospektPluginContext
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.typeWith

internal sealed interface AnnotatedElement {
    val annotations: Map<IrType, List<AnnotationUsageInfo>>

    fun instantiateAnnotations(
        module: IrModuleFragment, file: IrFile, source: List<String>, context: IntrospektPluginContext
    ): IrExpression = with(context) {
        createMapOf( // @formatter:off
            keyType = irBuiltIns.kClassClass.typeWith(annotationType.defaultType),
            valueType = annotationUsageInfoType.defaultType,
            values = annotations.map { (type, infos) ->
                type.toIrValue() to createListOf(
                    type = annotationUsageInfoType.defaultType,
                    values = infos.map { it.instantiate(module, file, source, context) }
                )
            }
        ) // @formatter:on
    }
}