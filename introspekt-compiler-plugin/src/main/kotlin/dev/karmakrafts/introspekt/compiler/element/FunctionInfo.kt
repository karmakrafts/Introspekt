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
import dev.karmakrafts.introspekt.compiler.util.getEnumValue
import dev.karmakrafts.introspekt.compiler.util.getFunctionLocation
import dev.karmakrafts.introspekt.compiler.util.getLocation
import dev.karmakrafts.introspekt.compiler.util.getModality
import dev.karmakrafts.introspekt.compiler.util.getModalityName
import dev.karmakrafts.introspekt.compiler.util.getObjectInstance
import dev.karmakrafts.introspekt.compiler.util.getVisibilityName
import dev.karmakrafts.introspekt.compiler.util.toAnnotationMap
import dev.karmakrafts.introspekt.compiler.util.toClassReference
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrParameterKind.Regular
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImplWithShape
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.starProjectedType
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.util.toIrConst
import org.jetbrains.kotlin.it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap

internal data class FunctionInfo(
    val location: SourceLocation,
    val qualifiedName: String,
    val name: String,
    val typeParameterNames: List<String>,
    val returnType: IrType,
    val parameterTypes: List<IrType>,
    val parameterNames: List<String>,
    val visibility: Visibility,
    val modality: Modality,
    val isExpect: Boolean,
    var locals: List<LocalInfo> = emptyList(),
    override var annotations: Map<IrType, List<AnnotationUsageInfo>> = emptyMap()
) : ElementInfo, AnnotatedElement {
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
            visibility: Visibility,
            modality: Modality,
            isExpect: Boolean,
            hashTransform: (Int) -> Int = { it },
            createCallback: FunctionInfo.() -> Unit = {}
        ): FunctionInfo = cache.getOrPut(hashTransform(getCacheKey(qualifiedName, returnType, parameterTypes))) {
            FunctionInfo(
                location = location,
                qualifiedName = qualifiedName,
                name = name,
                typeParameterNames = typeParameterNames,
                returnType = returnType,
                parameterTypes = parameterTypes,
                parameterNames = parameterNames,
                visibility = visibility,
                modality = modality,
                isExpect = isExpect
            ).apply(createCallback)
        }
    }

    override fun instantiateCached(context: IntrospektPluginContext): IrCall = with(context) {
        return IrCallImplWithShape(
            startOffset = SYNTHETIC_OFFSET,
            endOffset = SYNTHETIC_OFFSET,
            type = functionInfoType.defaultType,
            symbol = functionInfoGetOrCreate,
            valueArgumentsCount = 12,
            typeArgumentsCount = 0,
            contextParameterCount = 0,
            hasDispatchReceiver = true,
            hasExtensionReceiver = false
        ).apply { // @formatter:off
            var index = 0
            // location
            putValueArgument(index++, location.instantiateCached(context))
            // qualifiedName
            putValueArgument(index++, qualifiedName.toIrConst(irBuiltIns.stringType))
            // name
            putValueArgument(index++, name.toIrConst(irBuiltIns.stringType))
            // typeParameterNames
            putValueArgument(index++, createListOf(
                type = irBuiltIns.stringType,
                values = typeParameterNames.map { it.toIrConst(irBuiltIns.stringType) })
            )
            // returnType
            putValueArgument(index++, returnType.toClassReference(context))
            // parameterTypes
            putValueArgument(index++, createListOf(
                type = irBuiltIns.kClassClass.starProjectedType,
                values = parameterTypes.map { it.type.toIrValue() })
            )
            // parameterNames
            putValueArgument(index++, createListOf(
                type = irBuiltIns.stringType,
                values = parameterNames.map { it.toIrConst(irBuiltIns.stringType) })
            )
            // visibility
            putValueArgument(index++, visibility.getEnumValue(visibilityModifierType) { getVisibilityName() })
            // modality
            putValueArgument(index++, modality.getEnumValue(modalityModifierType) { getModalityName() })
            // locals
            putValueArgument(index++, createListOf(
                type = localInfoType.defaultType,
                values = locals.map { it.instantiateCached(context) }
            ))
            // isExpect
            putValueArgument(index++, isExpect.toIrConst(irBuiltIns.booleanType))
            // annotations
            putValueArgument(index, instantiateAnnotations(context))
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

internal fun IrFunction.getFunctionInfo( // @formatter:off
    module: IrModuleFragment,
    file: IrFile,
    source: List<String>,
): FunctionInfo {
    val regularParams = valueParameters.filter { it.kind == Regular }
    return FunctionInfo.getOrCreate(
        location = getFunctionLocation(module, file, source),
        qualifiedName = kotlinFqName.asString(),
        name = name.asString(),
        typeParameterNames = typeParameters.map { it.name.asString() },
        returnType = returnType,
        parameterTypes = regularParams.map { it.type },
        parameterNames = regularParams.map { it.name.asString() },
        visibility = visibility.delegate,
        modality = getModality(),
        isExpect = isExpect,
        hashTransform = { hash -> 31 * hash + parent.hashCode() }
    ) { // @formatter:on
        annotations = this@getFunctionInfo.annotations.toAnnotationMap(module, file, source)
        this@getFunctionInfo.body?.let { body ->
            locals = body.statements.filterIsInstance<IrVariable>().map { variable ->
                variable.getLocalInfo(module, file, source, this@getFunctionInfo)
            }
        }
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal fun IrAnonymousInitializer.getFunctionInfo( // @formatter:off
    module: IrModuleFragment,
    file: IrFile,
    source: List<String>
): FunctionInfo { // @formatter:on
    val constructor = requireNotNull(parentAsClass.primaryConstructor) { "Missing primary class constructor" }
    val regularParams = constructor.valueParameters.filter { it.kind == Regular }
    return FunctionInfo.getOrCreate( // @formatter:off
        location = getLocation(module, file, source),
        qualifiedName = constructor.kotlinFqName.asString(),
        name = constructor.name.asString(),
        typeParameterNames = constructor.typeParameters.map { it.name.asString() },
        returnType = constructor.returnType,
        parameterTypes = regularParams.map { it.type },
        parameterNames = regularParams.map { it.name.asString() },
        visibility = constructor.visibility.delegate,
        modality = constructor.getModality(),
        isExpect = false,
        hashTransform = { hash -> 31 * hash + parent.hashCode() }
    ) { // @formatter:on
        annotations = constructor.annotations.toAnnotationMap(module, file, source)
        locals = body.statements.filterIsInstance<IrVariable>().map { variable ->
            variable.getLocalInfo(module, file, source, this@getFunctionInfo)
        }
    }
}