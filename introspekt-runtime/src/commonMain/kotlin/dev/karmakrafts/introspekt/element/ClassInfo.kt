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

package dev.karmakrafts.introspekt.element

import co.touchlab.stately.collections.ConcurrentMutableMap
import dev.karmakrafts.introspekt.IntrospektCompilerApi
import dev.karmakrafts.introspekt.IntrospektIntrinsic
import dev.karmakrafts.introspekt.IntrospektPluginNotAppliedException
import dev.karmakrafts.introspekt.util.ClassModifier
import dev.karmakrafts.introspekt.util.ModalityModifier
import dev.karmakrafts.introspekt.util.VisibilityModifier

/**
 * Represents comprehensive information about a Kotlin class, interface, or object.
 *
 * This class provides detailed metadata about a class declaration, including its type parameters,
 * annotations, functions, properties, companion objects, and inheritance hierarchy. It implements
 * [AnnotatedElementInfo], [ExpectableElementInfo], and delegates to [TypeInfo] to provide a complete
 * view of a class's structure and characteristics.
 */
@ConsistentCopyVisibility
data class ClassInfo private constructor( // @formatter:off
    /**
     * The type information for this class.
     *
     * This property contains the basic type metadata like qualified name, simple name,
     * and source location. The [ClassInfo] class delegates to this [TypeInfo] for these properties.
     */
    val type: TypeInfo,

    /**
     * The names of type parameters declared on this class.
     *
     * For a generic class like `class Example<T, U>`, this list would contain ["T", "U"].
     * For non-generic classes, this list is empty.
     */
    val typeParameterNames: List<String>,
    override val annotations: Map<TypeInfo, List<AnnotationUsageInfo>>,

    /**
     * The list of functions declared in this class.
     *
     * This includes all member functions, extension functions, and constructors
     * declared within the class body, but excludes inherited functions.
     */
    val functions: List<FunctionInfo>,

    /**
     * The list of properties declared in this class.
     *
     * This includes all member properties and extension properties declared within
     * the class body, but excludes inherited properties.
     */
    val properties: List<PropertyInfo>,

    /**
     * The list of companion objects declared in this class.
     *
     * For classes with a companion object, this list will contain a single [ClassInfo]
     * representing that companion object. For classes without a companion object,
     * this list is empty.
     */
    val companionObjects: List<ClassInfo>,

    /**
     * The list of super types (parent classes and implemented interfaces) of this class.
     *
     * This includes all direct supertypes declared in the class header, including
     * the implicit [Any] supertype for classes that don't explicitly extend another class.
     */
    val superTypes: List<TypeInfo>,

    /**
     * Indicates whether this class is an interface.
     *
     * Returns `true` if this class was declared using the `interface` keyword,
     * `false` otherwise.
     */
    val isInterface: Boolean,

    /**
     * Indicates whether this class is an object.
     *
     * Returns `true` if this class was declared using the `object` keyword,
     * `false` otherwise. This includes both standalone objects and companion objects.
     */
    val isObject: Boolean,

    /**
     * Indicates whether this class is a companion object.
     *
     * Returns `true` if this class was declared using the `companion object` keywords,
     * `false` otherwise.
     */
    val isCompanionObject: Boolean,
    override val isExpect: Boolean,

    /**
     * The visibility modifier of this class.
     *
     * This property indicates the visibility level of the class, such as public, internal,
     * protected, or private.
     */
    val visibility: VisibilityModifier,
    /**
     * The modality modifier of this class.
     *
     * This property indicates whether the class is final, open, abstract, or sealed.
     */
    val modality: ModalityModifier,
    /**
     * The special class modifier applied to this class, if any.
     *
     * This property indicates whether the class has a special modifier such as
     * data, value, annotation, fun, enum, etc. If no special modifier is present,
     * this property is null.
     */
    val classModifier: ClassModifier?
) : AnnotatedElementInfo, ExpectableElementInfo, TypeInfo by type { // @formatter:on
    companion object {
        private val cache: ConcurrentMutableMap<TypeInfo, ClassInfo> = ConcurrentMutableMap()

        /**
         * Gets the [ClassInfo] for the class containing the call site.
         *
         * This function is an intrinsic that will be replaced by the Introspekt compiler plugin.
         * It allows you to get reflection information about the current class at compile time.
         *
         * @return A [ClassInfo] instance representing the class containing the call site
         * @throws IntrospektPluginNotAppliedException if the Introspekt compiler plugin is not applied
         */
        @IntrospektIntrinsic(IntrospektIntrinsic.Type.CI_CURRENT)
        fun current(): ClassInfo = throw IntrospektPluginNotAppliedException()

        /**
         * Gets the [ClassInfo] for the specified type.
         *
         * This function is an intrinsic that will be replaced by the Introspekt compiler plugin.
         * It allows you to get reflection information about any class at compile time.
         *
         * @param T The type to get class information for
         * @return A [ClassInfo] instance representing the class of type [T]
         * @throws IntrospektPluginNotAppliedException if the Introspekt compiler plugin is not applied
         */
        @IntrospektIntrinsic(IntrospektIntrinsic.Type.CI_OF)
        fun <T : Any> of(): ClassInfo = throw IntrospektPluginNotAppliedException()

        @IntrospektCompilerApi
        internal fun getOrCreate(
            type: TypeInfo,
            typeParameterNames: List<String>,
            annotations: Map<TypeInfo, List<AnnotationUsageInfo>>,
            functions: List<FunctionInfo>,
            properties: List<PropertyInfo>,
            companionObjects: List<ClassInfo>,
            superTypes: List<TypeInfo>,
            isInterface: Boolean,
            isObject: Boolean,
            isCompanionObject: Boolean,
            isExpect: Boolean,
            visibility: VisibilityModifier,
            modality: ModalityModifier,
            classModifier: ClassModifier?
        ): ClassInfo {
            return cache.getOrPut(type) {
                ClassInfo(
                    type = type,
                    typeParameterNames = typeParameterNames,
                    annotations = annotations,
                    functions = functions,
                    properties = properties,
                    companionObjects = companionObjects,
                    superTypes = superTypes,
                    isInterface = isInterface,
                    isObject = isObject,
                    isCompanionObject = isCompanionObject,
                    isExpect = isExpect,
                    visibility = visibility,
                    modality = modality,
                    classModifier = classModifier
                )
            }
        }
    }

    /**
     * Generates a formatted string representation of this class.
     *
     * This method creates a human-readable representation of the class, including its
     * annotations, visibility, modality, type parameters, properties, and functions.
     * The output is formatted with proper indentation to show the class structure.
     *
     * @param indent The number of tab characters to use for indentation
     * @return A formatted string representation of this class
     */
    fun toFormattedString(indent: Int = 0): String {
        val indentString = "\t".repeat(indent)
        // Annotations
        var result = if (annotations.isEmpty()) ""
        else "${annotations.values.flatten().joinToString("\n") { it.toFormattedString(indent) }}\n"
        // Class
        val qualifier = when {
            isInterface -> "interface"
            isCompanionObject -> "companion object"
            isObject -> "object"
            else -> "class"
        }
        val typeParams = if (typeParameterNames.isEmpty()) "" else "<${typeParameterNames.joinToString(", ")}>"
        val classModifier = classModifier?.toString()?.let { "$it " } ?: ""
        result += "$indentString$visibility $modality $classModifier$qualifier $qualifiedName$typeParams {\n"
        // Properties
        val hasProperties = properties.isNotEmpty()
        if (hasProperties) {
            result += "${properties.joinToString("\n") { it.toFormattedString(indent + 1) }}\n"
        }
        // Functions
        if (functions.isNotEmpty()) {
            if (hasProperties) result += '\n'
            result += "${functions.joinToString("\n\n") { it.toFormattedString(indent + 1) }}\n"
        }
        result += "$indentString}"
        return result
    }

    override fun hashCode(): Int = type.hashCode() // Use type identity

    override fun equals(other: Any?): Boolean {
        return if (other !is ClassInfo) false
        else type === other.type // Use type identity
    }
}
