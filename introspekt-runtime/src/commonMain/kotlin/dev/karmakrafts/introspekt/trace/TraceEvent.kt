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

import dev.karmakrafts.introspekt.util.SourceLocation
import kotlin.uuid.Uuid

/**
 * Represents a discrete event that occurs during code execution.
 *
 * A trace event captures information about a specific point in code execution,
 * including its source location, unique identifier, message, and associated data.
 * Events are typically collected by [TraceCollector] implementations.
 */
@ConsistentCopyVisibility
data class TraceEvent internal constructor( // @formatter:off
    /**
     * The source location where this event occurred.
     */
    val location: SourceLocation,

    /**
     * The unique identifier for this event.
     */
    val id: Uuid,

    /**
     * A descriptive message for this event.
     */
    val message: String,

    /**
     * Additional data associated with this event, stored as key-value pairs.
     */
    val data: Map<String, Any>
) // @formatter:on
