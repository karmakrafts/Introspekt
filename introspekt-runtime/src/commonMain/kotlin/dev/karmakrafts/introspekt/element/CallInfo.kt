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

import dev.karmakrafts.introspekt.util.SourceLocation

/**
 * Represents information about a function call in the source code.
 *
 * This class provides metadata about a specific function call, including its
 * location in the source code, the function making the call (caller), and
 * the function being called (callee). It is used for introspection of function
 * call relationships and call site analysis.
 */
data class CallInfo( // @formatter:off
    /**
     * The source code location where this function call occurs.
     *
     * This property provides information about where the call is made in the source code,
     * including the module, file, line, and column.
     */
    val location: SourceLocation,

    /**
     * The function that is making the call (the caller).
     *
     * This property represents the function in which this call appears, providing
     * access to all metadata about the calling function.
     */
    val caller: FunctionInfo,

    /**
     * The function that is being called (the callee).
     *
     * This property represents the target function of this call, providing
     * access to all metadata about the function being invoked.
     */
    val callee: FunctionInfo
) // @formatter:on
