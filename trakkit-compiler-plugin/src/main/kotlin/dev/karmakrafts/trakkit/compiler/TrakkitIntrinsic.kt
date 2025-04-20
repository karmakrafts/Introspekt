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

package dev.karmakrafts.trakkit.compiler

import org.jetbrains.kotlin.name.CallableId

internal enum class TrakkitIntrinsic(
    val functionId: CallableId
) {
    // @formatter:off
    SL_HERE                 (TrakkitNames.SourceLocation.Companion.here),
    SL_HERE_HASH            (TrakkitNames.SourceLocation.Companion.hereHash),
    SL_CURRENT_FUNCTION     (TrakkitNames.SourceLocation.Companion.currentFunction),
    SL_CURRENT_FUNCTION_HASH(TrakkitNames.SourceLocation.Companion.currentFunctionHash),
    SL_CURRENT_CLASS        (TrakkitNames.SourceLocation.Companion.currentClass),
    SL_CURRENT_CLASS_HASH   (TrakkitNames.SourceLocation.Companion.currentClassHash),
    FI_CURRENT              (TrakkitNames.FunctionInfo.Companion.current),
    CI_CURRENT              (TrakkitNames.ClassInfo.Companion.current);
    // @formatter:on

    companion object {
        fun byName(name: String): TrakkitIntrinsic? = entries.find { it.name.equals(name, true) }
    }
}