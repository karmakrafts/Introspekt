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

data class PropertyInfo(
    override val location: SourceLocation,
    override val qualifiedName: String,
    override val name: String,
    val isMutable: Boolean,
    val visibility: VisibilityModifier,
    val modality: ModalityModifier,
    val isExpect: Boolean,
    val isDelegated: Boolean,
    val backingField: FieldInfo?,
    val getter: FunctionInfo?,
    val setter: FunctionInfo?,
    override val annotations: Map<SimpleTypeInfo, List<AnnotationUsageInfo>>
) : AnnotatedElementInfo {
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
            annotations: Map<SimpleTypeInfo, List<AnnotationUsageInfo>>
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

    inline val typeOrNull: SimpleTypeInfo?
        get() = getter?.returnType// @formatter:off
            ?: setter?.parameterTypes?.first()
            ?: backingField?.type // @formatter:on

    inline val type: SimpleTypeInfo
        get() = typeOrNull ?: throw IllegalStateException("Could not determine type of property $qualifiedName")

    fun toFormattedString(indent: Int = 0): String {
        val indentString = "\t".repeat(indent)
        var result = "$indentString$visibility $modality "
        result += if (isMutable) "var " else "val "
        result += "$name: ${type.qualifiedName}"
        return result
    }
}