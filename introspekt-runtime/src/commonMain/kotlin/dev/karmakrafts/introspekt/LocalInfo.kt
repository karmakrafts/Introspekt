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

class LocalInfo(
    override val location: SourceLocation,
    override val qualifiedName: String,
    override val name: String,
    val type: KClass<*>,
    val isMutable: Boolean,
    override val annotations: Map<KClass<out Annotation>, List<AnnotationUsageInfo>>
) : AnnotatedElementInfo {
    companion object {
        private val cache: SharedHashMap<String, LocalInfo> = SharedHashMap()

        @IntrospektCompilerApi
        internal fun getOrCreate( // @formatter:off
            location: SourceLocation,
            qualifiedName: String,
            name: String,
            type: KClass<*>,
            isMutable: Boolean,
            annotations: Map<KClass<out Annotation>, List<AnnotationUsageInfo>>
        ): LocalInfo = cache.getOrPut(qualifiedName) {
            LocalInfo(
                location = location,
                qualifiedName = qualifiedName,
                name = name,
                type = type,
                isMutable = isMutable,
                annotations = annotations
            )
        } // @formatter:on
    }

    fun toFormattedString(indent: Int = 0): String {
        var result = "\t".repeat(indent)
        result += if (isMutable) "var " else "val "
        result += "$name: ${type.getQualifiedName()}"
        return result
    }
}