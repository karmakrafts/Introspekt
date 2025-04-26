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

package dev.karmakrafts.trakkit

import co.touchlab.stately.collections.SharedHashMap
import kotlin.reflect.KClass

data class PropertyInfo(
    override val location: SourceLocation,
    val qualifiedName: String,
    val name: String,
    val type: KClass<*>,
    val isMutable: Boolean,
    val visibility: VisibilityModifier,
    val modality: ModalityModifier,
    val getter: FunctionInfo,
    val setter: FunctionInfo?
) : ElementInfo {
    companion object {
        private val cache: SharedHashMap<Int, PropertyInfo> = SharedHashMap()

        private fun getCacheKey(
            qualifiedName: String, type: KClass<*>
        ): Int {
            var result = qualifiedName.hashCode()
            result = 31 * result + type.hashCode()
            return result
        }

        @TrakkitCompilerApi
        internal fun getOrCreate(
            location: SourceLocation,
            qualifiedName: String,
            name: String,
            type: KClass<*>,
            isMutable: Boolean,
            visibility: VisibilityModifier,
            modality: ModalityModifier,
            getter: FunctionInfo,
            setter: FunctionInfo?
        ): PropertyInfo {
            return cache.getOrPut(getCacheKey(qualifiedName, type)) {
                PropertyInfo(location, qualifiedName, name, type, isMutable, visibility, modality, getter, setter)
            }
        }
    }

    fun toFormattedString(indent: Int = 0): String {
        val indentString = "\t".repeat(indent)
        var result = "$indentString$visibility $modality "
        result += if (isMutable) "var " else "val "
        result += "$name: ${type.getQualifiedName()}"
        return result
    }
}