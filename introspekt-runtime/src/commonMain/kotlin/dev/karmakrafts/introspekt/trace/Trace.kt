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
import dev.karmakrafts.introspekt.util.SourceLocation
import kotlin.uuid.Uuid

/**
 * Injects trace callbacks for [TraceCollector] into the annotated function or the constructors associated initializers.
 */
@Retention(AnnotationRetention.BINARY)
@Target( // @formatter:off
    AnnotationTarget.FUNCTION,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.CLASS
) // @formatter:on
annotation class Trace( // @formatter:off
    vararg val targets: Target
) { // @formatter:on
    companion object {
        /**
         * Creates and records a trace event with the specified parameters.
         *
         * This function creates a [TraceEvent] with the provided information and passes it to the
         * [TraceCollector] for processing. It can be used to record discrete events that occur during
         * code execution, such as important state changes, user actions, or significant milestones.
         *
         * @param message A descriptive message for the event
         * @param id A unique identifier for the event (defaults to a random UUID)
         * @param data Additional data associated with the event as key-value pairs (defaults to empty map)
         * @param location The source location where the event occurred (defaults to the current location)
         */
        @OptIn(GeneratedIntrospektApi::class)
        @InlineDefaults( // @formatter:off
            InlineDefaults.Mode.NONE,
            InlineDefaults.Mode.NONE,
            InlineDefaults.Mode.NONE,
            InlineDefaults.Mode.SL_HERE
        ) // @formatter:on
        @IntrospektCompilerApi
        fun event(
            message: String,
            id: Uuid = Uuid.random(),
            data: Map<String, Any> = emptyMap(),
            location: SourceLocation = SourceLocation.here()
        ) {
            // @formatter:off
            TraceCollector.event(TraceEvent(
                location = location,
                id = id,
                message = message,
                data = data
            ))
            // @formatter:on
        }
    }

    enum class Target {
        // @formatter:off
        SPAN_ENTER,
        SPAN_LEAVE,
        FUNCTION_ENTER,
        FUNCTION_LEAVE,
        BEFORE_CALL,
        AFTER_CALL,
        SUSPENSION_POINT,
        EVENT;
        // @formatter:on
    }
}
