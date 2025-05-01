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

package dev.karmakrafts.introspekt

import co.touchlab.stately.collections.SharedHashMap
import kotlin.reflect.KClass

class FieldInfo(
    override val location: SourceLocation,
    override val qualifiedName: String,
    override val name: String,
    val type: KClass<*>,
    val visibility: VisibilityModifier,
    val isStatic: Boolean,
    val isExternal: Boolean,
    val isFinal: Boolean,
    override val annotations: Map<KClass<out Annotation>, List<AnnotationUsageInfo>>
) : AnnotatedElementInfo {
    companion object {
        private val cache: SharedHashMap<String, FieldInfo> = SharedHashMap()

        @IntrospektCompilerApi
        internal fun getOrCreate(
            location: SourceLocation,
            qualifiedName: String,
            name: String,
            type: KClass<*>,
            visibility: VisibilityModifier,
            isStatic: Boolean,
            isExternal: Boolean,
            isFinal: Boolean,
            annotations: Map<KClass<out Annotation>, List<AnnotationUsageInfo>>
        ): FieldInfo {
            return cache.getOrPut(qualifiedName) {
                FieldInfo(
                    location = location,
                    qualifiedName = qualifiedName,
                    name = name,
                    type = type,
                    visibility = visibility,
                    isStatic = isStatic,
                    isExternal = isExternal,
                    isFinal = isFinal,
                    annotations = annotations
                )
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        return if (other !is FieldInfo) false
        else qualifiedName == other.qualifiedName
    }

    override fun hashCode(): Int = qualifiedName.hashCode()
}