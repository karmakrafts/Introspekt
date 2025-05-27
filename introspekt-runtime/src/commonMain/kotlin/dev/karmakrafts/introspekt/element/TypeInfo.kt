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
import dev.karmakrafts.introspekt.util.SourceLocation
import kotlin.reflect.KClass

sealed interface TypeInfo : ElementInfo {
    val reflectType: KClass<*>
}

data class SimpleTypeInfo(
    override val location: SourceLocation,
    override val reflectType: KClass<*>,
    override val qualifiedName: String,
    override val name: String
) : TypeInfo {
    companion object {
        private val cache: ConcurrentMutableMap<KClass<*>, SimpleTypeInfo> = ConcurrentMutableMap()

        @IntrospektCompilerApi
        fun getOrCreate( // @formatter:off
            location: SourceLocation,
            reflectType: KClass<*>,
            qualifiedName: String,
            name: String
        ): SimpleTypeInfo = cache.getOrPut(reflectType) { // @formatter:on
            SimpleTypeInfo(location, reflectType, qualifiedName, name)
        }
    }

    override fun equals(other: Any?): Boolean {
        return if (other !is TypeInfo) false
        else reflectType == other.reflectType
    }

    override fun hashCode(): Int = reflectType.hashCode()
}