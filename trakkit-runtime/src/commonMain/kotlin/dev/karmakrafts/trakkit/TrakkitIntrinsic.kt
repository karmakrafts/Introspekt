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

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class TrakkitIntrinsic(val value: String) {
    companion object {
        internal const val SL_HERE: String = "sl_here"
        internal const val SL_HERE_HASH: String = "sl_here_hash"
        internal const val SL_CURRENT_FUNCTION: String = "sl_current_function"
        internal const val SL_CURRENT_FUNCTION_HASH: String = "sl_current_function_hash"
        internal const val SL_CURRENT_CLASS: String = "sl_current_class"
        internal const val SL_CURRENT_CLASS_HASH: String = "sl_current_class_hash"
        internal const val FI_CURRENT: String = "fi_current"
        internal const val CI_CURRENT: String = "ci_current"
        internal const val CI_OF: String = "ci_of"
    }
}
