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

import co.touchlab.stately.collections.ConcurrentMutableList
import dev.karmakrafts.introspekt.GeneratedIntrospektApi
import dev.karmakrafts.introspekt.InlineDefaults
import dev.karmakrafts.introspekt.IntrospektCompilerApi
import dev.karmakrafts.introspekt.element.FunctionInfo
import dev.karmakrafts.introspekt.util.SourceLocation

interface TraceCollector {
    companion object {
        private val collectors: ConcurrentMutableList<TraceCollector> = ConcurrentMutableList()

        fun register(collector: TraceCollector) {
            collectors.add(collector)
        }

        fun unregister(collector: TraceCollector) {
            collectors.remove(collector)
        }

        internal fun enterSpan(span: TraceSpan) {
            for (collector in collectors) {
                collector.enterSpan(span)
            }
        }

        internal fun leaveSpan(span: TraceSpan, end: SourceLocation) {
            for (collector in collectors) {
                collector.leaveSpan(span, end)
            }
        }

        internal fun event(event: TraceEvent) {
            for (collector in collectors) {
                collector.event(event)
            }
        }

        @IntrospektCompilerApi
        @OptIn(GeneratedIntrospektApi::class)
        @InlineDefaults(InlineDefaults.Mode.FI_CURRENT)
        internal fun enterFunction(function: FunctionInfo = FunctionInfo.current()) {
            for (collector in collectors) {
                collector.enterFunction(function)
            }
        }

        @IntrospektCompilerApi
        @OptIn(GeneratedIntrospektApi::class)
        @InlineDefaults(InlineDefaults.Mode.FI_CURRENT)
        internal fun leaveFunction(function: FunctionInfo = FunctionInfo.current()) {
            for (collector in collectors) {
                collector.leaveFunction(function)
            }
        }

        @IntrospektCompilerApi
        @OptIn(GeneratedIntrospektApi::class)
        @InlineDefaults( // @formatter:off
            InlineDefaults.Mode.NONE,
            InlineDefaults.Mode.FI_CURRENT,
            InlineDefaults.Mode.SL_HERE
        ) // @formatter:on
        internal fun call(
            callee: FunctionInfo,
            caller: FunctionInfo = FunctionInfo.current(),
            location: SourceLocation = SourceLocation.here()
        ) {
            for (collector in collectors) {
                collector.call(callee, caller, location)
            }
        }
    }

    fun enterSpan(span: TraceSpan)

    fun leaveSpan(span: TraceSpan, end: SourceLocation)

    fun enterFunction(function: FunctionInfo)

    fun leaveFunction(function: FunctionInfo)

    fun call(callee: FunctionInfo, caller: FunctionInfo, location: SourceLocation)

    fun event(event: TraceEvent)
}