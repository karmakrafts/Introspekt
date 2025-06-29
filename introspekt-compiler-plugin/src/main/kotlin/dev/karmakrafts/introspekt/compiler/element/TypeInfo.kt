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
import dev.karmakrafts.introspekt.compiler.util.getObjectInstance
import dev.karmakrafts.introspekt.compiler.util.toClassReference
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImplWithShape
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.starProjectedType
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.isTypeParameter
import org.jetbrains.kotlin.ir.util.toIrConst

internal data class TypeInfo( // @formatter:off
    val type: IrType,
    val location: SourceLocation
) : ElementInfo { // @formatter:on
    companion object {
        private val cache: HashMap<IrType, TypeInfo> = HashMap()

        fun getOrCreate( // @formatter:off
            type: IrType,
            module: IrModuleFragment,
            file: IrFile,
            source: List<String>
        ): TypeInfo = cache.getOrPut(type) { // @formatter:on
            TypeInfo( // @formatter:off
                type = type,
                location = type.getLocation(module, file, source)
            ) // @formatter:on
        }
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun instantiateCached( // @formatter:off
        module: IrModuleFragment,
        file: IrFile,
        source: List<String>,
        context: IntrospektPluginContext
    ): IrCall = with(context) { // @formatter:on
        IrCallImplWithShape(
            startOffset = SYNTHETIC_OFFSET,
            endOffset = SYNTHETIC_OFFSET,
            type = introspektSymbols.typeInfoType.defaultType,
            symbol = introspektSymbols.typeInfoGetOrCreate,
            typeArgumentsCount = 0,
            contextParameterCount = 0,
            valueArgumentsCount = 4,
            hasDispatchReceiver = true,
            hasExtensionReceiver = false
        ).apply {
            val function = symbol.owner
            arguments[function.parameters.first { it.name.asString() == "location" }] =
                location.instantiateCached(context)
            // Workaround to handle type parameters; TODO: implement a type parameter API in the runtime
            if(this@TypeInfo.type.isTypeParameter()) {
                arguments[function.parameters.first { it.name.asString() == "reflectType" }] =
                    null.toIrConst(irBuiltIns.kClassClass.starProjectedType)
            }
            else {
                arguments[function.parameters.first { it.name.asString() == "reflectType" }] =
                    this@TypeInfo.type.toClassReference(context)
            }
            arguments[function.parameters.first { it.name.asString() == "qualifiedName" }] =
                (this@TypeInfo.type.classFqName?.asString() ?: "<undefined>").toIrConst(irBuiltIns.stringType)
            arguments[function.parameters.first { it.name.asString() == "name" }] =
                (this@TypeInfo.type.classFqName?.shortNameOrSpecial()?.asString()
                    ?: "<undefined>").toIrConst(irBuiltIns.stringType)
            dispatchReceiver = introspektSymbols.typeInfoCompanionType.getObjectInstance()
        }
    }

    override fun equals(other: Any?): Boolean {
        return if (other !is TypeInfo) false
        else type == other.type
    }

    override fun hashCode(): Int = type.hashCode()
}

internal fun IrType.getTypeInfo( // @formatter:off
    module: IrModuleFragment,
    file: IrFile,
    source: List<String>
): TypeInfo { // @formatter:on
    return TypeInfo.getOrCreate(
        type = type,
        module = module,
        file = file,
        source = source
    )
}