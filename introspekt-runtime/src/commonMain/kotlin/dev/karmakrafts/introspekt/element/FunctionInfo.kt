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

package dev.karmakrafts.introspekt.element

import co.touchlab.stately.collections.ConcurrentMutableMap
import dev.karmakrafts.introspekt.IntrospektCompilerApi
import dev.karmakrafts.introspekt.IntrospektIntrinsic
import dev.karmakrafts.introspekt.IntrospektPluginNotAppliedException
import dev.karmakrafts.introspekt.util.ModalityModifier
import dev.karmakrafts.introspekt.util.SourceLocation
import dev.karmakrafts.introspekt.util.VisibilityModifier
import kotlin.reflect.KFunction

/**
 * Represents information about a function or method in a class, interface, or file.
 *
 * This class provides access to metadata about a specific function, including its
 * location in source code, qualified name, simple name, type parameters, return type,
 * parameters, visibility, modality, local variables, and annotations.
 * It extends [AnnotatedElementInfo] to include annotation information and
 * [ExpectableElementInfo] to support Kotlin's expect/actual mechanism.
 */
@ConsistentCopyVisibility
data class FunctionInfo private constructor( // @formatter:off
    override val location: SourceLocation,
    override val qualifiedName: String,
    override val name: String,

    /**
     * The names of type parameters declared by this function.
     *
     * For example, in `fun <T, U> process(input: T): U`, this would contain ["T", "U"].
     */
    val typeParameterNames: List<String>,

    /**
     * The return type information for this function.
     */
    val returnType: TypeInfo,

    /**
     * The list of parameters this function accepts.
     */
    val parameters: List<ParameterInfo>,

    /**
     * The visibility modifier of this function (public, private, protected, internal).
     */
    val visibility: VisibilityModifier,

    /**
     * The modality of this function (final, open, abstract, sealed).
     */
    val modality: ModalityModifier,

    /**
     * The list of local variables declared within this function.
     */
    val locals: List<LocalInfo>,
    override val isExpect: Boolean,
    override val annotations: Map<TypeInfo, List<AnnotationUsageInfo>>
) : AnnotatedElementInfo, ExpectableElementInfo { // @formatter:on
    companion object {
        private val cache: ConcurrentMutableMap<Int, FunctionInfo> = ConcurrentMutableMap()

        /**
         * Gets information about the currently executing function.
         *
         * This method is an intrinsic that is replaced by the Introspekt compiler plugin
         * with code that returns the [FunctionInfo] for the function where this call appears.
         *
         * @return The [FunctionInfo] for the currently executing function
         * @throws IntrospektPluginNotAppliedException if the Introspekt compiler plugin is not applied
         */
        @IntrospektIntrinsic(IntrospektIntrinsic.Type.FI_CURRENT)
        fun current(): FunctionInfo = throw IntrospektPluginNotAppliedException()

        /**
         * Gets information about the specified Kotlin function.
         *
         * This method is an intrinsic that is replaced by the Introspekt compiler plugin
         * with code that returns the [FunctionInfo] for the given [KFunction].
         *
         * @param function The Kotlin function to get information about
         * @return The [FunctionInfo] for the specified function
         * @throws IntrospektPluginNotAppliedException if the Introspekt compiler plugin is not applied
         */
        @IntrospektIntrinsic(IntrospektIntrinsic.Type.FI_OF)
        fun of(function: KFunction<*>): FunctionInfo = throw IntrospektPluginNotAppliedException()

        private fun getCacheKey( // @formatter:off
            qualifiedName: String,
            returnType: TypeInfo,
            parameters: List<ParameterInfo>
        ): Int { // @formatter:on
            var result = qualifiedName.hashCode()
            result = 31 * result + returnType.hashCode()
            result = 31 * result + parameters.hashCode()
            return result
        }

        @IntrospektCompilerApi
        internal fun getOrCreate(
            location: SourceLocation,
            qualifiedName: String,
            name: String,
            typeParameterNames: List<String>,
            returnType: TypeInfo,
            parameters: List<ParameterInfo>,
            visibility: VisibilityModifier,
            modality: ModalityModifier,
            locals: List<LocalInfo>,
            isExpect: Boolean,
            annotations: Map<TypeInfo, List<AnnotationUsageInfo>>
        ): FunctionInfo {
            return cache.getOrPut(getCacheKey(qualifiedName, returnType, parameters)) {
                FunctionInfo(
                    location = location,
                    qualifiedName = qualifiedName,
                    name = name,
                    typeParameterNames = typeParameterNames,
                    returnType = returnType,
                    parameters = parameters,
                    visibility = visibility,
                    modality = modality,
                    locals = locals,
                    isExpect = isExpect,
                    annotations = annotations
                )
            }
        }
    }

    /**
     * Returns a formatted string representation of this function.
     *
     * The string includes the function's annotations, visibility, modality, name,
     * type parameters, parameters, return type, and local variables.
     *
     * @param indent The number of tab characters to prepend to the string for indentation
     * @return A formatted string representation of this function
     */
    fun toFormattedString(indent: Int = 0): String {
        // Annotations
        var result = if (annotations.isEmpty()) ""
        else "${annotations.values.flatten().joinToString("\n") { it.toFormattedString(indent) }}\n"
        // Parameters
        val parameters = if (parameters.isEmpty()) ""
        else parameters.mapIndexed { index, name ->
            "$name: ${parameters[index].qualifiedName}"
        }.joinToString(", ")
        // Function
        result += "${"\t".repeat(indent)}fun "
        if (typeParameterNames.isNotEmpty()) {
            result += "<${typeParameterNames.joinToString(", ")}> "
        }
        val escapedName = if (' ' in name) "`$name`" else name
        result += "$escapedName($parameters): ${returnType.qualifiedName}"
        for (local in locals) {
            result += "\n${local.toFormattedString(indent + 1)}"
        }
        return result
    }
}
