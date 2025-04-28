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

/**
 * Injects trace callbacks for [TraceCollector] into the annotated function or the constructors associated initializers.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CONSTRUCTOR)
annotation class Trace( // @formatter:off
    val types: Int = ALL,
    val depth: Int = RECURSIVE_DEPTH
) { // @formatter:on
    companion object {
        const val RECURSIVE_DEPTH: Int = -1

        // @formatter:off
        const val SPAN_ENTER: Int     = 0b0000_0000_0000_0001
        const val SPAN_LEAVE: Int     = 0b0000_0000_0000_0010
        const val FUNCTION_ENTER: Int = 0b0000_0000_0000_0100
        const val FUNCTION_LEAVE: Int = 0b0000_0000_0000_1000
        const val PROPERTY_LOAD: Int  = 0b0000_0000_0001_0000
        const val PROPERTY_STORE: Int = 0b0000_0000_0010_0000
        const val LOCAL_LOAD: Int     = 0b0000_0000_0100_0000
        const val LOCAL_STORE: Int    = 0b0000_0000_1000_0000
        const val CALL: Int           = 0b0000_0001_0000_0000
        const val EVENT: Int          = 0b0000_0010_0000_0000

        const val ALL: Int = SPAN_ENTER or
            SPAN_LEAVE or
            FUNCTION_ENTER or
            FUNCTION_LEAVE or
            PROPERTY_LOAD or
            PROPERTY_STORE or
            LOCAL_LOAD or
            LOCAL_STORE or
            CALL or
            EVENT
        // @formatter:on

        private fun FunctionInfo.shouldTrace(): Boolean {
            if (!hasAnnotation<Trace>()) return false
            val traceTypes = getAnnotation<Trace>().getValueOrNull<Int>("types") ?: ALL
            return traceTypes and EVENT != 0
        }

        @OptIn(GeneratedTrakkitApi::class)
        @CaptureCaller("3:sl_here", "4:fi_current")
        fun event(
            message: String,
            id: Uuid = Uuid.random(),
            data: Map<String, Any> = emptyMap(),
            location: SourceLocation = SourceLocation.here(),
            caller: FunctionInfo = FunctionInfo.current()
        ) {
            if (!caller.shouldTrace()) return
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