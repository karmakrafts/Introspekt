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

package dev.karmakrafts.trakkit.compiler.util

import org.jetbrains.kotlin.name.CallableId

internal enum class IntrinsicResultType {
    // @formatter:off
    SOURCE_LOCATION,
    FUNCTION_INFO,
    CLASS_INFO,
    HASH
    // @formatter:on
}

internal enum class TrakkitIntrinsic( // @formatter:off
    val supportsInlining: Boolean,
    val resultType: IntrinsicResultType,
    val functionId: CallableId
) { // @formatter:on
    // @formatter:off
    SL_HERE                 (true,  IntrinsicResultType.SOURCE_LOCATION,    TrakkitNames.SourceLocation.Companion.here),
    SL_HERE_HASH            (true,  IntrinsicResultType.HASH,               TrakkitNames.SourceLocation.Companion.hereHash),
    SL_CURRENT_FUNCTION     (true,  IntrinsicResultType.SOURCE_LOCATION,    TrakkitNames.SourceLocation.Companion.currentFunction),
    SL_CURRENT_FUNCTION_HASH(true,  IntrinsicResultType.HASH,               TrakkitNames.SourceLocation.Companion.currentFunctionHash),
    SL_CURRENT_CLASS        (true,  IntrinsicResultType.SOURCE_LOCATION,    TrakkitNames.SourceLocation.Companion.currentClass),
    SL_CURRENT_CLASS_HASH   (true,  IntrinsicResultType.HASH,               TrakkitNames.SourceLocation.Companion.currentClassHash),
    SL_OF_CLASS             (false, IntrinsicResultType.SOURCE_LOCATION,    TrakkitNames.SourceLocation.Companion.ofClass),
    SL_OF_FUNCTION          (false, IntrinsicResultType.SOURCE_LOCATION,    TrakkitNames.SourceLocation.Companion.ofFunction),
    FI_CURRENT              (true,  IntrinsicResultType.FUNCTION_INFO,      TrakkitNames.FunctionInfo.Companion.current),
    FI_OF                   (false, IntrinsicResultType.FUNCTION_INFO,      TrakkitNames.FunctionInfo.Companion.of),
    CI_CURRENT              (true,  IntrinsicResultType.CLASS_INFO,         TrakkitNames.ClassInfo.Companion.current),
    CI_OF                   (false, IntrinsicResultType.CLASS_INFO,         TrakkitNames.ClassInfo.Companion.of);
    // @formatter:on

    companion object {
        fun byName(name: String): TrakkitIntrinsic? = entries.find { it.name.equals(name, true) }
    }
}