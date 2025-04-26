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

import co.touchlab.stately.concurrency.ThreadLocalRef
import co.touchlab.stately.concurrency.value

@ConsistentCopyVisibility
data class TraceSpan @TrakkitCompilerApi internal constructor(
    val name: String,
    val start: SourceLocation,
    val end: SourceLocation,
    val function: FunctionInfo
) {
    companion object {
        internal val stack: ThreadLocalRef<ArrayList<TraceSpan>> = ThreadLocalRef<ArrayList<TraceSpan>>().apply {
            value = ArrayList()
        }

        @TrakkitIntrinsic(TrakkitIntrinsic.TS_PUSH)
        fun push(name: String): TraceSpan = throw TrakkitPluginNotAppliedException()

        @TrakkitCompilerApi
        internal fun push(span: TraceSpan): TraceSpan {
            stack.value!!.add(span)
            return span
        }
    }

    @TrakkitIntrinsic(TrakkitIntrinsic.TS_POP)
    fun pop() = stack.value!!.remove(this)
}