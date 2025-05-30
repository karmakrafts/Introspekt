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

package dev.karmakrafts.introspekt.trace

import dev.karmakrafts.introspekt.CaptureCaller
import dev.karmakrafts.introspekt.GeneratedIntrospektApi
import dev.karmakrafts.introspekt.IntrospektCompilerApi
import dev.karmakrafts.introspekt.element.FunctionInfo
import dev.karmakrafts.introspekt.util.SourceLocation
import kotlin.uuid.Uuid

internal expect fun pushTraceSpan(span: TraceSpan)

internal expect fun popTraceSpan(): TraceSpan

internal expect fun peekTraceSpan(): TraceSpan?

@ConsistentCopyVisibility
data class TraceSpan private constructor( // @formatter:off
    val name: String,
    val id: Uuid,
    val start: SourceLocation,
    val function: FunctionInfo
) { // @formatter:on
    companion object {
        @OptIn(GeneratedIntrospektApi::class)
        @CaptureCaller("2:SL_HERE", "3:FI_CURRENT")
        @IntrospektCompilerApi
        fun enter(
            name: String,
            id: Uuid = Uuid.random(),
            start: SourceLocation = SourceLocation.here(),
            function: FunctionInfo = FunctionInfo.current()
        ) {
            val span = TraceSpan(name, id, start, function)
            TraceCollector.enterSpan(span)
            pushTraceSpan(span)
        }

        @OptIn(GeneratedIntrospektApi::class)
        @CaptureCaller("0:SL_HERE")
        @IntrospektCompilerApi
        fun leave( // @formatter:off
            end: SourceLocation = SourceLocation.here()
        ) { // @formatter:on
            TraceCollector.leaveSpan(popTraceSpan(), end)
        }

        fun current(): TraceSpan? = peekTraceSpan()
    }
}