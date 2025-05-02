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

sealed interface FrameChange {
    data class Define( // @formatter:off
        val local: LocalInfo,
        @PublishedApi internal val rawValue: Any?
    ) : FrameChange { // @formatter:on
        inline fun <reified T> getValue(): T? = rawValue as? T
    }

    data class Change( // @formatter:off
        val local: LocalInfo,
        @PublishedApi internal val rawOldValue: Any?,
        @PublishedApi internal val rawNewValue: Any?
    ) : FrameChange { // @formatter:on
        inline fun <reified T> getOldValue(): T? = rawOldValue as? T
        inline fun <reified T> getNewValue(): T? = rawNewValue as? T
    }
}

@ConsistentCopyVisibility
data class FrameSnapshot @IntrospektCompilerApi internal constructor( // @formatter:off
    val location: SourceLocation,
    val locals: Map<LocalInfo, Any?>
) { // @formatter:on
    companion object {
        val empty: FrameSnapshot = FrameSnapshot(SourceLocation.undefined, emptyMap())

        @IntrospektIntrinsic(IntrospektIntrinsic.Type.FS_CREATE)
        fun create(): FrameSnapshot = throw IntrospektPluginNotAppliedException()
    }

    fun getLocal(name: String): LocalInfo? = locals.keys.find { it.name == name }

    inline fun <reified T> getLocalValue(name: String): T? = locals.entries.find { it.key.name == name }?.value as? T

    fun diff(next: FrameSnapshot): ArrayList<FrameChange> {
        val newLocals = next.locals
        val changes = ArrayList<FrameChange>()
        for ((local, value) in locals) {
            val newValue = newLocals[local]
            if (local in newLocals) {
                if (value == newValue) {
                    changes += FrameChange.Change(local, value, newValue)
                    continue
                }
            }
            else {
                changes += FrameChange.Define(local, newValue)
            }
        }
        return changes
    }

    fun toFormattedString(indent: Int = 0): String {
        var result = ""
        for ((local, value) in locals) {
            result += "${local.toFormattedString(indent)} = $value\n"
        }
        return result
    }
}