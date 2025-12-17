/*
 * Copyright 2025 Karma Krafts
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

/**
 * Interface for collecting and processing trace information during code execution.
 *
 * A trace collector receives notifications about trace spans, function entries and exits,
 * function calls, and discrete events that occur during program execution. Implementations
 * can use this information for various purposes such as performance monitoring, debugging,
 * or generating execution reports.
 */
interface TraceCollector {
    companion object {
        private val collectors: ConcurrentMutableList<TraceCollector> = ConcurrentMutableList()

        /**
         * Registers a trace collector to receive trace notifications.
         *
         * @param collector The trace collector to register.
         */
        fun register(collector: TraceCollector) {
            collectors.add(collector)
        }

        /**
         * Unregisters a trace collector to stop it from receiving trace notifications.
         *
         * @param collector The trace collector to unregister.
         */
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
        internal fun beforeCall(
            callee: FunctionInfo,
            caller: FunctionInfo = FunctionInfo.current(),
            location: SourceLocation = SourceLocation.here()
        ) {
            for (collector in collectors) {
                collector.beforeCall(callee, caller, location)
            }
        }

        @IntrospektCompilerApi
        @OptIn(GeneratedIntrospektApi::class)
        @InlineDefaults( // @formatter:off
            InlineDefaults.Mode.NONE,
            InlineDefaults.Mode.FI_CURRENT,
            InlineDefaults.Mode.SL_HERE
        ) // @formatter:on
        internal fun afterCall(
            callee: FunctionInfo,
            caller: FunctionInfo = FunctionInfo.current(),
            location: SourceLocation = SourceLocation.here()
        ) {
            for (collector in collectors) {
                collector.afterCall(callee, caller, location)
            }
        }

        @IntrospektCompilerApi
        @OptIn(GeneratedIntrospektApi::class)
        @InlineDefaults( // @formatter:off
            InlineDefaults.Mode.FI_CURRENT,
            InlineDefaults.Mode.SL_HERE
        ) // @formatter:on
        internal fun onSuspensionPoint(
            function: FunctionInfo = FunctionInfo.current(),
            location: SourceLocation = SourceLocation.here()
        ) {
            for (collector in collectors) {
                collector.onSuspensionPoint(function, location)
            }
        }
    }

    /**
     * Called when a new trace span is entered.
     *
     * @param span The trace span that is being entered.
     */
    fun enterSpan(span: TraceSpan)

    /**
     * Called when a trace span is exited.
     *
     * @param span The trace span that is being exited.
     * @param end The source location where the span ends.
     */
    fun leaveSpan(span: TraceSpan, end: SourceLocation)

    /**
     * Called when a function is entered.
     *
     * @param function Information about the function being entered.
     */
    fun enterFunction(function: FunctionInfo)

    /**
     * Called when a function is exited.
     *
     * @param function Information about the function being exited.
     */
    fun leaveFunction(function: FunctionInfo)

    /**
     * Called before a function calls another function.
     *
     * @param callee Information about the function being called.
     * @param caller Information about the function making the call.
     * @param location The source location where the call occurs.
     */
    fun beforeCall(callee: FunctionInfo, caller: FunctionInfo, location: SourceLocation)

    /**
     * Called after a function calls another function.
     *
     * @param callee Information about the function being called.
     * @param caller Information about the function making the call.
     * @param location The source location where the call occurs.
     */
    fun afterCall(callee: FunctionInfo, caller: FunctionInfo, location: SourceLocation)

    // TODO: document this
    fun onSuspensionPoint(function: FunctionInfo, location: SourceLocation)

    /**
     * Called when a trace event occurs.
     *
     * @param event The trace event that occurred.
     */
    fun event(event: TraceEvent)
}
