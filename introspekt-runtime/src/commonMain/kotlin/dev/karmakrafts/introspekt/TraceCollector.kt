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

import co.touchlab.stately.collections.SharedLinkedList

interface TraceCollector {
    companion object {
        private val collectors: SharedLinkedList<TraceCollector> = SharedLinkedList()

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
        internal fun enterFunction(function: FunctionInfo) {
            for (collector in collectors) {
                collector.enterFunction(function)
            }
        }

        @IntrospektCompilerApi
        internal fun leaveFunction(function: FunctionInfo) {
            for (collector in collectors) {
                collector.leaveFunction(function)
            }
        }

        @IntrospektCompilerApi
        internal fun call(callSite: CallInfo) {
            for (collector in collectors) {
                collector.call(callSite)
            }
        }

        @IntrospektCompilerApi
        internal fun loadProperty(property: PropertyInfo) {
            for (collector in collectors) {
                collector.loadProperty(property)
            }
        }

        @IntrospektCompilerApi
        internal fun storeProperty(property: PropertyInfo) {
            for (collector in collectors) {
                collector.storeProperty(property)
            }
        }

        @IntrospektCompilerApi
        internal fun loadLocal(local: LocalInfo) {
            for (collector in collectors) {
                collector.loadLocal(local)
            }
        }

        @IntrospektCompilerApi
        internal fun storeLocal(local: LocalInfo) {
            for (collector in collectors) {
                collector.storeLocal(local)
            }
        }
    }

    fun enterSpan(span: TraceSpan)

    fun leaveSpan(span: TraceSpan, end: SourceLocation)

    fun enterFunction(function: FunctionInfo)

    fun leaveFunction(function: FunctionInfo)

    fun call(call: CallInfo)

    fun event(event: TraceEvent)

    fun loadProperty(property: PropertyInfo)

    fun storeProperty(property: PropertyInfo)

    fun loadLocal(local: LocalInfo)

    fun storeLocal(local: LocalInfo)
}