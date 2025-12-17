/*
 * Copyright 2025 Karma Krafts
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
import dev.karmakrafts.introspekt.compiler.util.getObjectInstance
import dev.karmakrafts.introspekt.compiler.util.getVisibilityName
import dev.karmakrafts.introspekt.compiler.util.toAnnotationMap
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrSyntheticBody
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImplWithShape
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.defaultType
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
    val returnType: TypeInfo,
    val parameters: List<ParameterInfo>,
    val visibility: Visibility,
    val modality: Modality,
    val isExpect: Boolean,
    var locals: List<LocalInfo> = emptyList(),
    override var annotations: Map<TypeInfo, List<AnnotationUsageInfo>> = emptyMap()
) : ElementInfo, AnnotatedElement {
    companion object {
        private val cache: Int2ObjectOpenHashMap<FunctionInfo> = Int2ObjectOpenHashMap()

        private fun getCacheKey(
            qualifiedName: String,
            returnType: TypeInfo,
            parameters: List<ParameterInfo>,
        ): Int {
            var result = qualifiedName.hashCode()
            result = 31 * result + qualifiedName.hashCode()
            result = 31 * result + returnType.hashCode()
            result = 31 * result + parameters.hashCode()
            return result
        }

        inline fun getOrCreate(
            location: SourceLocation,
            qualifiedName: String,
            name: String,
            typeParameterNames: List<String>,
            returnType: TypeInfo,
            parameters: List<ParameterInfo>,
            visibility: Visibility,
            modality: Modality,
            isExpect: Boolean,
            hashTransform: (Int) -> Int = { it },
            createCallback: FunctionInfo.() -> Unit = {}
        ): FunctionInfo = cache.getOrPut(hashTransform(getCacheKey(qualifiedName, returnType, parameters))) {
            FunctionInfo(
                location = location,
                qualifiedName = qualifiedName,
                name = name,
                typeParameterNames = typeParameterNames,
                returnType = returnType,
                parameters = parameters,
                visibility = visibility,
                modality = modality,
                isExpect = isExpect
            ).apply(createCallback)
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
            type = introspektSymbols.functionInfoType.defaultType,
            symbol = introspektSymbols.functionInfoGetOrCreate,
            valueArgumentsCount = 11,
            typeArgumentsCount = 0,
            contextParameterCount = 0,
            hasDispatchReceiver = true,
            hasExtensionReceiver = false
        ).apply {
            val function = symbol.owner
            arguments[function.parameters.first { it.name.asString() == "location" }] =
                location.instantiateCached(context)
            arguments[function.parameters.first { it.name.asString() == "qualifiedName" }] =
                qualifiedName.toIrConst(irBuiltIns.stringType)
            arguments[function.parameters.first { it.name.asString() == "name" }] =
                name.toIrConst(irBuiltIns.stringType)
            arguments[function.parameters.first { it.name.asString() == "typeParameterNames" }] = createListOf(
                irBuiltIns.stringType, typeParameterNames.map { it.toIrConst(irBuiltIns.stringType) })
            arguments[function.parameters.first { it.name.asString() == "returnType" }] =
                returnType.instantiateCached(module, file, source, context)
            arguments[function.parameters.first { it.name.asString() == "parameters" }] = createListOf(
                introspektSymbols.parameterInfoType.defaultType,
                parameters.map { it.instantiateCached(module, file, source, context) })
            arguments[function.parameters.first { it.name.asString() == "visibility" }] =
                visibility.getEnumValue(introspektSymbols.visibilityModifierType) { getVisibilityName() }
            arguments[function.parameters.first { it.name.asString() == "modality" }] =
                modality.getEnumValue(introspektSymbols.modalityModifierType) { name }
            arguments[function.parameters.first { it.name.asString() == "locals" }] = createListOf(
                introspektSymbols.localInfoType.defaultType,
                locals.map { it.instantiateCached(module, file, source, context) })
            arguments[function.parameters.first { it.name.asString() == "isExpect" }] =
                isExpect.toIrConst(irBuiltIns.booleanType)
            arguments[function.parameters.first { it.name.asString() == "annotations" }] =
                instantiateAnnotations(module, file, source, context)
            dispatchReceiver = introspektSymbols.functionInfoCompanionType.getObjectInstance()
        }
    }

    // Annotations are not relevant to identity hash
    override fun hashCode(): Int {
        var result = qualifiedName.hashCode()
        result = 31 * result + typeParameterNames.hashCode()
        result = 31 * result + returnType.hashCode()
        result = 31 * result + parameters.hashCode()
        return result
    }

    // Annotations are not relevant to value equality
    override fun equals(other: Any?): Boolean { // @formatter:off
        return if (other !is FunctionInfo) false
        else qualifiedName == other.qualifiedName
            && typeParameterNames == other.typeParameterNames
            && returnType == other.returnType
            && parameters == other.parameters
    } // @formatter:on
}

internal fun IrFunction.getFunctionInfo( // @formatter:off
    module: IrModuleFragment,
    file: IrFile,
    source: List<String>,
): FunctionInfo {
    val regularParams = parameters.filter { it.kind == IrParameterKind.Regular }
    return FunctionInfo.getOrCreate(
        location = getFunctionLocation(module, file, source),
        qualifiedName = kotlinFqName.asString(),
        name = name.asString(),
        typeParameterNames = typeParameters.map { it.name.asString() },
        returnType = returnType.getTypeInfo(module, file, source),
        parameters = regularParams.map { it.getParameterInfo(module, file, source) },
        visibility = visibility.delegate,
        modality = getModality(),
        isExpect = isExpect,
        hashTransform = { hash -> 31 * hash + parent.hashCode() }
    ) { // @formatter:on
        annotations = this@getFunctionInfo.annotations.toAnnotationMap(module, file, source)
        if (body is IrSyntheticBody) return@getOrCreate // We skip synthetic bodies
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
    val regularParams = constructor.parameters.filter { it.kind == IrParameterKind.Regular }
    return FunctionInfo.getOrCreate( // @formatter:off
        location = getLocation(module, file, source),
        qualifiedName = constructor.kotlinFqName.asString(),
        name = constructor.name.asString(),
        typeParameterNames = constructor.typeParameters.map { it.name.asString() },
        returnType = constructor.returnType.getTypeInfo(module, file, source),
        parameters = regularParams.map { it.getParameterInfo(module, file, source) },
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