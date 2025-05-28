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

package dev.karmakrafts.introspekt.element

import dev.karmakrafts.introspekt.util.SourceLocation

/**
 * Base interface for all element information in the Introspekt system.
 *
 * This sealed interface provides common properties for all code elements that can be
 * introspected, such as types, properties, functions, and fields. It contains basic
 * information about the element's location in source code and its naming.
 */
sealed interface ElementInfo {
    /**
     * The source code location of this element.
     *
     * This property provides information about where the element is defined in the source code,
     * including the module, file, line, and column.
     */
    val location: SourceLocation

    /**
     * The fully qualified name of this element.
     *
     * This includes the package name and any parent class names, followed by the element's name.
     * For example: "com.example.MyClass.myProperty"
     */
    val qualifiedName: String

    /**
     * The simple name of this element without any package or parent class qualifiers.
     *
     * For example, if the qualified name is "com.example.MyClass.myProperty", the name would be "myProperty".
     */
    val name: String

    /**
     * The package name of this element, derived from the qualified name.
     *
     * This property extracts the package portion from the qualified name by removing the element's name
     * and any parent class names.
     */
    val packageName: String
        get() = qualifiedName.substring(0..<qualifiedName.lastIndexOf(name) - 1)
}
