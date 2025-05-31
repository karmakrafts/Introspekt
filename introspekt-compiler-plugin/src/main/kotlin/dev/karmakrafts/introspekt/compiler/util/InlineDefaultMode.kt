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

import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.util.hasAnnotation

internal sealed interface InlineDefaultMode {
    val enumName: String

    data object None : InlineDefaultMode {
        override val enumName: String = "NONE"
    }

    data class Intrinsic(val type: IntrospektIntrinsic) : InlineDefaultMode {
        override val enumName: String = type.name
    }
}

internal fun IrFunction.getInlineDefaultModes(): List<InlineDefaultMode> {
    if (!hasAnnotation(IntrospektNames.InlineDefaults.id)) return emptyList()
    return getAnnotationValues<String>(IntrospektNames.InlineDefaults.fqName, "modes").map { modeName ->
        when (modeName) {
            null, "NONE" -> InlineDefaultMode.None
            else -> InlineDefaultMode.Intrinsic(IntrospektIntrinsic.valueOf(modeName))
        }
    }
}