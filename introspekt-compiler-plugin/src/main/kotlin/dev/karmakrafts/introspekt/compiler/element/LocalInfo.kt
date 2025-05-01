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
import dev.karmakrafts.introspekt.compiler.util.SourceLocation
import dev.karmakrafts.introspekt.compiler.util.getLocation
import dev.karmakrafts.introspekt.compiler.util.toAnnotationMap
import dev.karmakrafts.introspekt.compiler.util.toClassReference
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImplWithShape
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.toIrConst

internal class LocalInfo(
    val location: SourceLocation,
    val qualifiedName: String,
    val name: String,
    val type: IrType,
    val isMutable: Boolean,
    override val annotations: Map<IrType, List<AnnotationUsageInfo>>
) : ElementInfo, AnnotatedElement {
    override fun instantiateCached(context: IntrospektPluginContext): IrCall = with(context) {
        IrCallImplWithShape(
            startOffset = SYNTHETIC_OFFSET,
            endOffset = SYNTHETIC_OFFSET,
            type = localInfoType.defaultType,
            symbol = localInfoGetOrCreate,
            typeArgumentsCount = 0,
            valueArgumentsCount = 6,
            contextParameterCount = 0,
            hasDispatchReceiver = true,
            hasExtensionReceiver = false
        ).apply {
            var index = 0
            // location
            putValueArgument(index++, location.instantiateCached(context))
            // qualifiedName
            putValueArgument(index++, qualifiedName.toIrConst(irBuiltIns.stringType))
            // name
            putValueArgument(index++, name.toIrConst(irBuiltIns.stringType))
            // type
            putValueArgument(index++, this@LocalInfo.type.toClassReference(context))
            // isMutable
            putValueArgument(index++, isMutable.toIrConst(irBuiltIns.booleanType))
            // annotations
            putValueArgument(index, instantiateAnnotations(context))
            dispatchReceiver = localInfoCompanionType.getObjectInstance()
        }
    }
}

internal fun IrVariable.getLocalInfo( // @formatter:off
    module: IrModuleFragment,
    file: IrFile,
    source: List<String>,
    function: IrFunction
): LocalInfo = LocalInfo( // @formatter:on
    location = getLocation(module, file, source),
    qualifiedName = "${function.kotlinFqName.asString()}.${name.asString()}",
    name = name.asString(),
    type = type,
    isMutable = isVar,
    annotations = annotations.toAnnotationMap(module, file, source)
)

internal fun IrVariable.getLocalInfo( // @formatter:off
    module: IrModuleFragment,
    file: IrFile,
    source: List<String>,
    function: IrAnonymousInitializer
): LocalInfo = LocalInfo( // @formatter:on
    location = getLocation(module, file, source),
    qualifiedName = "${function.parentAsClass.name.asString()}.<init>.${name.asString()}",
    name = name.asString(),
    type = type,
    isMutable = isVar,
    annotations = annotations.toAnnotationMap(module, file, source)
)