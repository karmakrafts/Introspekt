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

package dev.karmakrafts.introspekt.compiler.util

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal enum class TraceType(
    val className: FqName,
    val functionName: Name
) {
    // @formatter:off
    SPAN_ENTER      (IntrospektNames.TraceSpan.Companion.fqName,        IntrospektNames.Functions.enter),
    SPAN_LEAVE      (IntrospektNames.TraceSpan.Companion.fqName,        IntrospektNames.Functions.leave),
    FUNCTION_ENTER  (IntrospektNames.TraceCollector.Companion.fqName,   IntrospektNames.Functions.enterFunction),
    FUNCTION_LEAVE  (IntrospektNames.TraceCollector.Companion.fqName,   IntrospektNames.Functions.leaveFunction),
    PROPERTY_LOAD   (IntrospektNames.TraceCollector.Companion.fqName,   IntrospektNames.Functions.loadProperty),
    PROPERTY_STORE  (IntrospektNames.TraceCollector.Companion.fqName,   IntrospektNames.Functions.storeProperty),
    LOCAL_LOAD      (IntrospektNames.TraceCollector.Companion.fqName,   IntrospektNames.Functions.loadLocal),
    LOCAL_STORE     (IntrospektNames.TraceCollector.Companion.fqName,   IntrospektNames.Functions.storeLocal),
    CALL            (IntrospektNames.TraceCollector.Companion.fqName,   IntrospektNames.Functions.call),
    EVENT           (IntrospektNames.Trace.Companion.fqName,            IntrospektNames.Functions.event);
    // @formatter:on
}