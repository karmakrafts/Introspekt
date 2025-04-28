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
        private fun FunctionInfo.shouldTraceEnter(): Boolean {
            if (!hasAnnotation<Trace>()) return false
            val traceTypes = getAnnotation<Trace>().getValueOrNull<Int>("types") ?: Trace.ALL
            return traceTypes and Trace.SPAN_ENTER != 0
        }

        private fun FunctionInfo.shouldTraceLeave(): Boolean {
            if (!hasAnnotation<Trace>()) return false
            val traceTypes = getAnnotation<Trace>().getValueOrNull<Int>("types") ?: Trace.ALL
            return traceTypes and Trace.SPAN_LEAVE != 0
        }

        @OptIn(GeneratedTrakkitApi::class)
        @CaptureCaller("2:sl_here", "3:fi_current")
        fun enter(
            name: String,
            id: Uuid = Uuid.random(),
            start: SourceLocation = SourceLocation.here(),
            function: FunctionInfo = FunctionInfo.current()
        ) {
            if (!function.shouldTraceEnter()) return
            val span = TraceSpan(name, id, start, function)
            TraceCollector.enterSpan(span)
            pushTraceSpan(span)
        }

        @OptIn(GeneratedTrakkitApi::class)
        @CaptureCaller("0:sl_here", "1:fi_current")
        fun leave( // @formatter:off
            end: SourceLocation = SourceLocation.here(),
            function: FunctionInfo = FunctionInfo.current()
        ) { // @formatter:on
            if (!function.shouldTraceLeave()) return
            TraceCollector.leaveSpan(popTraceSpan(), end)
        }

        fun current(): TraceSpan? = peekTraceSpan()
    }
}