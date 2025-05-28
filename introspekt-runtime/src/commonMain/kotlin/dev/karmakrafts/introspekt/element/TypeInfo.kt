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
import dev.karmakrafts.introspekt.util.SourceLocation
import kotlin.reflect.KClass

/**
 * Represents type information for a class or interface.
 *
 * This interface provides access to metadata about a specific type, including its
 * location in source code, qualified name, simple name, and the corresponding KClass.
 * It extends [ElementInfo] to include basic element information.
 */
sealed interface TypeInfo : ElementInfo {
    companion object {
        private val cache: ConcurrentMutableMap<KClass<*>, SimpleTypeInfo> = ConcurrentMutableMap()

        /**
         * Creates a [TypeInfo] instance for the specified type.
         *
         * This function is an intrinsic that will be replaced by the Introspekt compiler plugin.
         * If the plugin is not applied, an [IntrospektPluginNotAppliedException] will be thrown.
         *
         * @return A [TypeInfo] instance representing the type [T]
         * @throws IntrospektPluginNotAppliedException if the Introspekt compiler plugin is not applied
         */
        @IntrospektIntrinsic(IntrospektIntrinsic.Type.TI_OF)
        fun <T : Any> of(): TypeInfo = throw IntrospektPluginNotAppliedException()

        @IntrospektCompilerApi
        internal fun getOrCreate( // @formatter:off
            location: SourceLocation,
            reflectType: KClass<*>,
            qualifiedName: String,
            name: String
        ): TypeInfo = cache.getOrPut(reflectType) { // @formatter:on
            SimpleTypeInfo(location, reflectType, qualifiedName, name)
        }
    }

    /**
     * The Kotlin reflection class ([KClass]) corresponding to this type.
     *
     * This property provides access to the runtime reflection capabilities for this type.
     */
    val reflectType: KClass<*>
}

private data class SimpleTypeInfo(
    override val location: SourceLocation,
    override val reflectType: KClass<*>,
    override val qualifiedName: String,
    override val name: String
) : TypeInfo {
    override fun equals(other: Any?): Boolean {
        return if (other !is TypeInfo) false
        else reflectType == other.reflectType
    }

    override fun hashCode(): Int = reflectType.hashCode()
}
