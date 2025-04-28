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
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImplWithShape
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.toIrConst
import org.jetbrains.kotlin.it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap

internal data class PropertyInfo(
    val location: SourceLocation,
    override val qualifiedName: String,
    override val name: String,
    val type: IrType,
    val isMutable: Boolean,
    val visibility: Visibility,
    val modality: Modality,
    val getter: FunctionInfo,
    val setter: FunctionInfo?,
    var annotations: Map<IrType, List<AnnotationUsageInfo>> = emptyMap()
) : ElementInfo {
    companion object {
        private val cache: Int2ObjectOpenHashMap<PropertyInfo> = Int2ObjectOpenHashMap()

        private fun getCacheKey(
            qualifiedName: String, type: IrType
        ): Int {
            var result = qualifiedName.hashCode()
            result = 31 * result + type.hashCode()
            return result
        }

        inline fun getOrCreate(
            location: SourceLocation,
            qualifiedName: String,
            name: String,
            type: IrType,
            isMutable: Boolean,
            visibility: Visibility,
            modality: Modality,
            getter: FunctionInfo,
            setter: FunctionInfo?,
            createCallback: PropertyInfo.() -> Unit = {}
        ): PropertyInfo = cache.getOrPut(getCacheKey(qualifiedName, type)) {
            PropertyInfo(location, qualifiedName, name, type, isMutable, visibility, modality, getter, setter).apply(createCallback)
        }
    }

    override fun instantiateCached(context: IntrospektPluginContext): IrCall = with(context) {
        return IrCallImplWithShape(
            startOffset = SYNTHETIC_OFFSET,
            endOffset = SYNTHETIC_OFFSET,
            type = propertyInfoType.defaultType,
            symbol = propertyInfoGetOrCreate,
            typeArgumentsCount = 0,
            valueArgumentsCount = 10,
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
            // type
            putValueArgument(index++, this@PropertyInfo.type.toClassReference())
            // isMutable
            putValueArgument(index++, isMutable.toIrConst(irBuiltIns.booleanType))
            // visibility
            putValueArgument(index++, visibility.getEnumValue(visibilityModifierType) { getVisibilityName() })
            // modality
            putValueArgument(index++, modality.getEnumValue(modalityModifierType) { getModalityName() })
            // getter
            putValueArgument(index++, getter.instantiateCached(context))
            // setter
            putValueArgument(index++, setter?.instantiateCached(context) ?: null.toIrConst(functionInfoType.defaultType))
            // annotations
            putValueArgument(index, createMapOf(
                keyType = irBuiltIns.kClassClass.typeWith(annotationType.defaultType),
                valueType = annotationUsageInfoType.defaultType,
                values = annotations.map { (type, infos) ->
                    type.toIrValue() to createListOf(
                        type = annotationUsageInfoType.defaultType,
                        values = infos.map { it.instantiate(context) }
                    )
                })
            )
            dispatchReceiver = propertyInfoCompanionType.getObjectInstance()
        } // @formatter:on
    }

    // Modifiers are not relevant to identity hash
    override fun hashCode(): Int {
        var result = qualifiedName.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }

    // Modifiers are not relevant to value equality
    override fun equals(other: Any?): Boolean {
        return if (other !is PropertyInfo) false
        else qualifiedName == other.qualifiedName && type == other.type
    }
}