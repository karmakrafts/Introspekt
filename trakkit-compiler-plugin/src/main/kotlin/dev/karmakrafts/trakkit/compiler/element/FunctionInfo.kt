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

package dev.karmakrafts.trakkit.compiler.element

import dev.karmakrafts.trakkit.compiler.TrakkitPluginContext
import dev.karmakrafts.trakkit.compiler.util.SourceLocation
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImplWithShape
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.starProjectedType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.toIrConst
import org.jetbrains.kotlin.it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap

internal data class FunctionInfo(
    val location: SourceLocation,
    override val qualifiedName: String,
    override val name: String,
    val typeParameterNames: List<String>,
    val returnType: IrType,
    val parameterTypes: List<IrType>,
    val parameterNames: List<String>,
    var annotations: Map<IrType, AnnotationUsageInfo> = emptyMap()
) : ElementInfo {
    companion object {
        private val cache: Int2ObjectOpenHashMap<FunctionInfo> = Int2ObjectOpenHashMap()

        private fun getCacheKey(
            qualifiedName: String,
            returnType: IrType,
            parameterTypes: List<IrType>,
        ): Int {
            var result = qualifiedName.hashCode()
            result = 31 * result + qualifiedName.hashCode()
            result = 31 * result + returnType.hashCode()
            result = 31 * result + parameterTypes.hashCode()
            return result
        }

        inline fun getOrCreate(
            location: SourceLocation,
            qualifiedName: String,
            name: String,
            typeParameterNames: List<String>,
            returnType: IrType,
            parameterTypes: List<IrType>,
            parameterNames: List<String>,
            hashTransform: (Int) -> Int = { it },
            createCallback: FunctionInfo.() -> Unit = {}
        ): FunctionInfo = cache.getOrPut(hashTransform(getCacheKey(qualifiedName, returnType, parameterTypes))) {
            FunctionInfo(
                location, qualifiedName, name, typeParameterNames, returnType, parameterTypes, parameterNames
            ).apply(createCallback)
        }
    }

    override fun instantiateCached(context: TrakkitPluginContext): IrCall = with(context) {
        return IrCallImplWithShape(
            startOffset = SYNTHETIC_OFFSET,
            endOffset = SYNTHETIC_OFFSET,
            type = functionInfoType.defaultType,
            symbol = functionInfoGetOrCreate,
            valueArgumentsCount = 8,
            typeArgumentsCount = 0,
            contextParameterCount = 0,
            hasDispatchReceiver = true,
            hasExtensionReceiver = false
        ).apply { // @formatter:off
            var index = 0
            // location
            putValueArgument(index++, location.instantiateCached())
            // qualifiedName
            putValueArgument(index++, qualifiedName.toIrConst(irBuiltIns.stringType))
            // name
            putValueArgument(index++, name.toIrConst(irBuiltIns.stringType))
            // typeParameterNames
            putValueArgument(
                index++, createListOf(
                type = irBuiltIns.stringType,
                values = typeParameterNames.map { it.toIrConst(irBuiltIns.stringType) }))
            // returnType
            putValueArgument(index++, returnType.toClassReference())
            // parameterTypes
            putValueArgument(
                index++, createListOf(
                type = irBuiltIns.kClassClass.starProjectedType,
                values = parameterTypes.map { it.type.toIrValueOrType() }))
            // parameterNames
            putValueArgument(
                index++, createListOf(
                type = irBuiltIns.stringType,
                values = parameterNames.map { it.toIrConst(irBuiltIns.stringType) }))
            // annotations
            putValueArgument(
                index, createMapOf(
                keyType = irBuiltIns.kClassClass.typeWith(annotationType.defaultType),
                valueType = annotationInfoType.defaultType,
                values = annotations.map { (type, info) ->
                    type.toIrValueOrType() to info.instantiate(context)
                }))
            dispatchReceiver = functionInfoCompanionType.getObjectInstance()
        } // @formatter:on
    }

    // Annotations are not relevant to identity hash
    override fun hashCode(): Int {
        var result = qualifiedName.hashCode()
        result = 31 * result + typeParameterNames.hashCode()
        result = 31 * result + returnType.hashCode()
        result = 31 * result + parameterTypes.hashCode()
        return result
    }

    // Annotations are not relevant to value equality
    override fun equals(other: Any?): Boolean { // @formatter:off
        return if (other !is FunctionInfo) false
        else qualifiedName == other.qualifiedName
            && typeParameterNames == other.typeParameterNames
            && returnType == other.returnType
            && parameterTypes == other.parameterTypes
    } // @formatter:on
}