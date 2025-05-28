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
import dev.karmakrafts.introspekt.util.ModalityModifier
import dev.karmakrafts.introspekt.util.SourceLocation
import dev.karmakrafts.introspekt.util.VisibilityModifier

/**
 * Represents information about a property in a class or interface.
 *
 * This class provides access to metadata about a specific property, including its
 * location in source code, qualified name, simple name, mutability, visibility,
 * modality, and associated elements like backing field, getter, and setter.
 * It extends [AnnotatedElementInfo] to include annotation information.
 */
@ConsistentCopyVisibility
data class PropertyInfo private constructor( // @formatter:off
    override val location: SourceLocation,
    override val qualifiedName: String,
    override val name: String,

    /**
     * Indicates whether this property is mutable (var) or immutable (val).
     */
    val isMutable: Boolean,

    /**
     * The visibility modifier of this property (public, private, protected, internal).
     */
    val visibility: VisibilityModifier,

    /**
     * The modality of this property (final, open, abstract, sealed).
     */
    val modality: ModalityModifier,

    /**
     * Indicates whether this property is marked with the 'expect' keyword.
     */
    val isExpect: Boolean,

    /**
     * Indicates whether this property uses delegation (by keyword).
     */
    val isDelegated: Boolean,

    /**
     * The backing field information for this property, if it has one.
     */
    val backingField: FieldInfo?,

    /**
     * The getter function information for this property.
     */
    val getter: FunctionInfo?,

    /**
     * The setter function information for this property, if it's mutable.
     */
    val setter: FunctionInfo?, override val annotations: Map<TypeInfo, List<AnnotationUsageInfo>>
) : AnnotatedElementInfo { // @formatter:on
    companion object {
        private val cache: ConcurrentMutableMap<String, PropertyInfo> = ConcurrentMutableMap()

        @IntrospektCompilerApi
        internal fun getOrCreate(
            location: SourceLocation,
            qualifiedName: String,
            name: String,
            isMutable: Boolean,
            visibility: VisibilityModifier,
            modality: ModalityModifier,
            isExpect: Boolean,
            isDelegated: Boolean,
            backingField: FieldInfo?,
            getter: FunctionInfo?,
            setter: FunctionInfo?,
            annotations: Map<TypeInfo, List<AnnotationUsageInfo>>
        ): PropertyInfo {
            return cache.getOrPut(qualifiedName) {
                PropertyInfo(
                    location = location,
                    qualifiedName = qualifiedName,
                    name = name,
                    isMutable = isMutable,
                    visibility = visibility,
                    modality = modality,
                    isExpect = isExpect,
                    isDelegated = isDelegated,
                    backingField = backingField,
                    getter = getter,
                    setter = setter,
                    annotations = annotations
                )
            }
        }
    }

    /**
     * The type of this property, or null if the type cannot be determined.
     *
     * This property attempts to determine the type from the getter's return type,
     * the setter's first parameter type, or the backing field's type, in that order.
     */
    inline val typeOrNull: TypeInfo?
        get() = getter?.returnType// @formatter:off
            ?: setter?.parameters?.first()?.type
            ?: backingField?.type // @formatter:on

    /**
     * The type of this property.
     *
     * This property returns the type determined by [typeOrNull] or throws an
     * [IllegalStateException] if the type cannot be determined.
     *
     * @throws IllegalStateException if the property type cannot be determined
     */
    inline val type: TypeInfo
        get() = typeOrNull ?: throw IllegalStateException("Could not determine type of property $qualifiedName")

    /**
     * Returns a formatted string representation of this property.
     *
     * The string includes the property's visibility, modality, mutability (var/val),
     * name, and type.
     *
     * @param indent The number of tab characters to prepend to the string for indentation
     * @return A formatted string representation of this property
     */
    fun toFormattedString(indent: Int = 0): String {
        val indentString = "\t".repeat(indent)
        var result = "$indentString$visibility $modality "
        result += if (isMutable) "var " else "val "
        result += "$name: ${type.qualifiedName}"
        return result
    }
}
