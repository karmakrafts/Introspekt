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

/**
 * Represents a location in source code.
 *
 * This class is used to track the exact position in the source code where a specific
 * operation or function call occurs. It contains information about the module, file,
 * function, line, and column of the source location.
 *
 * The [here] function in the companion object is an intrinsic that gets replaced by
 * the Trakkit compiler plugin with the actual source location information at compile time.
 */
data class SourceLocation(
    /**
     * The name of the module containing the source code.
     */
    val module: String,

    /**
     * The name of the file containing the source code.
     */
    val file: String,

    /**
     * The name of the function containing the source code.
     */
    val function: String,

    /**
     * The line number in the source file.
     */
    val line: Int,

    /**
     * The column number in the source file.
     */
    val column: Int
) {
    /**
     * Companion object containing utility functions for [SourceLocation].
     */
    companion object {
        /**
         * Returns a [SourceLocation] object representing the current location in the source code.
         *
         * This function is an intrinsic that gets replaced by the Trakkit compiler plugin
         * with the actual source location information at compile time. If the plugin is not
         * applied, this function will throw a [TrakkitPluginNotAppliedException].
         *
         * @return A [SourceLocation] object representing the current location in the source code.
         * @throws TrakkitPluginNotAppliedException if the Trakkit compiler plugin is not applied.
         */
        @TrakkitIntrinsic(TrakkitIntrinsic.SL_HERE)
        fun here(): SourceLocation = throw TrakkitPluginNotAppliedException()

        /**
         * Returns an int representing the compile-time calculated hash of the [SourceLocation]
         * obtained at this point in the code.
         *
         * @return The hash of the [SourceLocation] obtained at this point in the code.
         * @throws TrakkitPluginNotAppliedException if the Trakkit compiler plugin is not applied.
         */
        @TrakkitIntrinsic(TrakkitIntrinsic.SL_HERE_HASH)
        fun hereHash(): Int = throw TrakkitPluginNotAppliedException()

        /**
         * Returns a [SourceLocation] object representing the current function in the source code.
         *
         * This function is an intrinsic that gets replaced by the Trakkit compiler plugin
         * with the actual function location information at compile time. If the plugin is not
         * applied, this function will throw a [TrakkitPluginNotAppliedException].
         *
         * @return A [SourceLocation] object representing the current function in the source code.
         * @throws TrakkitPluginNotAppliedException if the Trakkit compiler plugin is not applied.
         */
        @TrakkitIntrinsic(TrakkitIntrinsic.SL_CURRENT_FUNCTION)
        fun currentFunction(): SourceLocation = throw TrakkitPluginNotAppliedException()

        /**
         * Returns an int representing the compile-time calculated hash of the current function's
         * [SourceLocation].
         *
         * This function is an intrinsic that gets replaced by the Trakkit compiler plugin
         * with the actual function location hash at compile time. If the plugin is not
         * applied, this function will throw a [TrakkitPluginNotAppliedException].
         *
         * @return The hash of the current function's [SourceLocation].
         * @throws TrakkitPluginNotAppliedException if the Trakkit compiler plugin is not applied.
         */
        @TrakkitIntrinsic(TrakkitIntrinsic.SL_CURRENT_FUNCTION_HASH)
        fun currentFunctionHash(): Int = throw TrakkitPluginNotAppliedException()
    }

    /**
     * Returns a simplified string representation of this [SourceLocation].
     *
     * The format of the string is: "file:line:column", which is more concise than
     * the full representation provided by [toString].
     *
     * @return A simplified string representation of this [SourceLocation].
     */
    fun toPrintableString(): String = "$file:$line:$column"

    /**
     * Returns a string representation of this [SourceLocation].
     *
     * The format of the string is: "file in module: function:line:column".
     *
     * @return A string representation of this [SourceLocation].
     */
    override fun toString(): String = "$module:$file:$function:$line:$column"
}
