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

import kotlin.uuid.Uuid

enum class TraceTarget {
    // @formatter:off
    SPAN_ENTER,
    SPAN_LEAVE,
    FUNCTION_ENTER,
    FUNCTION_LEAVE,
    PROPERTY_LOAD,
    PROPERTY_STORE,
    LOCAL_LOAD,
    LOCAL_STORE,
    CALL,
    EVENT;
    // @formatter:on
}

/**
 * Injects trace callbacks for [TraceCollector] into the annotated function or the constructors associated initializers.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.CLASS)
annotation class Trace( // @formatter:off
    vararg val targets: TraceTarget,
    val depth: Int = RECURSIVE_DEPTH
) { // @formatter:on
    companion object {
        const val RECURSIVE_DEPTH: Int = -1

        @OptIn(GeneratedIntrospektApi::class)
        @CaptureCaller("3:sl_here", "4:fi_current")
        @IntrospektCompilerApi
        fun event(
            message: String,
            id: Uuid = Uuid.random(),
            data: Map<String, Any> = emptyMap(),
            location: SourceLocation = SourceLocation.here(),
            caller: FunctionInfo = FunctionInfo.current()
        ) {
            TraceCollector.onEvent(TraceEvent( // @formatter:off
                location = location,
                id = id,
                message = message,
                data = data
            )
            ) // @formatter:on
        }
    }
}