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

import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.util.target
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal enum class TraceType( // @formatter:off
    val className: FqName,
    val functionName: Name
) { // @formatter:on
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

internal fun IrCall.getTraceType(): TraceType? {
    val function = target
    val functionName = function.name
    val parentClass = function.parentClassOrNull ?: return null
    val className = parentClass.kotlinFqName
    return TraceType.entries.find { it.className == className && it.functionName == functionName }
}

internal fun IrAnnotationContainer.isTraceable(): Boolean = hasAnnotation(IntrospektNames.Trace.id)

internal fun IrAnnotationContainer.getTraceType(): List<TraceType> {
    return if (!isTraceable()) emptyList()
    else getAnnotationValues<TraceType>(IntrospektNames.Trace.fqName, "targets").filterNotNull()
}