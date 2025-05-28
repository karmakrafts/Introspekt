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

/**
 * Annotation used to automatically inject intrinsic values into function parameters at compile time.
 * 
 * When a function or constructor is annotated with @CaptureCaller, the Introspekt compiler plugin
 * will automatically inject the specified intrinsic values into the function's parameters at call sites.
 * This allows for capturing compile-time information such as source location, caller information,
 * and other introspection data without requiring the caller to explicitly provide these values.
 *
 * @property intrinsics A vararg array of strings specifying which intrinsics to inject and where.
 *                     Each string should be in the format "index:intrinsicType" where:
 *                     - index: The parameter position (0-based) to inject the intrinsic value into
 *                     - intrinsicType: The name of the intrinsic type from IntrospektIntrinsic.Type enum
 *
 * @see IntrospektIntrinsic
 */
@GeneratedIntrospektApi
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CONSTRUCTOR)
annotation class CaptureCaller(vararg val intrinsics: String)