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

import dev.karmakrafts.introspekt.GeneratedIntrospektApi
import dev.karmakrafts.introspekt.InlineDefaults
import dev.karmakrafts.introspekt.IntrospektCompilerApi
import dev.karmakrafts.introspekt.element.FunctionInfo
import dev.karmakrafts.introspekt.trace.TraceSpan.Companion.enter
import dev.karmakrafts.introspekt.trace.TraceSpan.Companion.leave
import dev.karmakrafts.introspekt.util.SourceLocation
import kotlin.uuid.Uuid

internal expect fun pushTraceSpan(span: TraceSpan)

internal expect fun popTraceSpan(): TraceSpan

internal expect fun peekTraceSpan(): TraceSpan?

/**
 * Represents a trace span for tracking execution flow in code.
 *
 * A trace span captures information about a specific section of code execution,
 * including its name, unique identifier, source location, and function information.
 * Spans are typically created using [enter] and closed using [leave].
 */
@ConsistentCopyVisibility
data class TraceSpan private constructor( // @formatter:off
    /**
     * The name of this trace span, used for identification.
     */
    val name: String,

    /**
     * The unique identifier for this trace span.
     */
    val id: Uuid,

    /**
     * The source location where this trace span was created.
     */
    val start: SourceLocation,

    /**
     * Information about the function where this trace span was created.
     */
    val function: FunctionInfo
) { // @formatter:on
    /**
     * Companion object providing factory methods for creating and managing trace spans.
     */
    companion object {
        /**
         * Creates and enters a new trace span.
         *
         * This method creates a new trace span with the given parameters and pushes it onto the trace stack.
         * It also notifies the [TraceCollector] that a new span has started.
         *
         * @param name The name of the trace span.
         * @param id The unique identifier for the trace span. Defaults to a random UUID.
         * @param start The source location where the trace span starts. Defaults to the current location.
         * @param function Information about the function where the trace span is created. Defaults to the current function.
         */
        @OptIn(GeneratedIntrospektApi::class)
        @InlineDefaults(
            InlineDefaults.Mode.NONE,
            InlineDefaults.Mode.NONE,
            InlineDefaults.Mode.SL_HERE,
            InlineDefaults.Mode.FI_CURRENT
        )
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

        /**
         * Leaves the current trace span.
         *
         * This method pops the current trace span from the trace stack and notifies
         * the [TraceCollector] that the span has ended.
         *
         * @param end The source location where the trace span ends. Defaults to the current location.
         */
        @OptIn(GeneratedIntrospektApi::class)
        @InlineDefaults(InlineDefaults.Mode.SL_HERE)
        @IntrospektCompilerApi
        fun leave( // @formatter:off
            end: SourceLocation = SourceLocation.here()
        ) { // @formatter:on
            TraceCollector.leaveSpan(popTraceSpan(), end)
        }

        /**
         * Returns the current active trace span, if any.
         *
         * @return The current trace span, or null if no span is active.
         */
        fun current(): TraceSpan? = peekTraceSpan()
    }
}
