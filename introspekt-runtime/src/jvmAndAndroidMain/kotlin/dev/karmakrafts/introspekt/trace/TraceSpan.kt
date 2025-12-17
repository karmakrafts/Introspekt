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

import java.util.*

private val traceSpanStack: ThreadLocal<Stack<TraceSpan>> = ThreadLocal.withInitial { Stack() }

internal actual fun pushTraceSpan(span: TraceSpan) {
    traceSpanStack.get()!!.push(span)
}

internal actual fun popTraceSpan(): TraceSpan {
    return traceSpanStack.get()!!.pop()
}

internal actual fun peekTraceSpan(): TraceSpan? {
    return traceSpanStack.get()!!.firstOrNull()
}