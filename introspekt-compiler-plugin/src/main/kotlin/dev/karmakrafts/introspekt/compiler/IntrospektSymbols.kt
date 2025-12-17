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

package dev.karmakrafts.introspekt.compiler

import dev.karmakrafts.introspekt.compiler.util.IntrospektNames
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.isVararg

internal class IntrospektSymbols(
    pluginContext: IrPluginContext
) {
    internal val annotationType: IrClassSymbol = pluginContext.referenceClass(IntrospektNames.Kotlin.Annotation.id)!!

    // listOf
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    internal val listOfFunction: IrSimpleFunctionSymbol = pluginContext.referenceFunctions(IntrospektNames.Kotlin.listOf)
        .find { symbol -> symbol.owner.parameters.filter { it.kind == IrParameterKind.Regular }.any { it.isVararg } }!!
    internal val listType: IrClassSymbol = pluginContext.referenceClass(IntrospektNames.Kotlin.List.id)!!

    // mapOf
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    internal val mapOfFunction: IrSimpleFunctionSymbol = pluginContext.referenceFunctions(IntrospektNames.Kotlin.mapOf)
        .find { symbol -> symbol.owner.parameters.filter { it.kind == IrParameterKind.Regular }.any { it.isVararg } }!!
    internal val mapType: IrClassSymbol = pluginContext.referenceClass(IntrospektNames.Kotlin.Map.id)!!

    // Pair
    internal val pairConstructor: IrConstructorSymbol =
        pluginContext.referenceConstructors(IntrospektNames.Kotlin.Pair.id).first()
    internal val pairType: IrClassSymbol = pluginContext.referenceClass(IntrospektNames.Kotlin.Pair.id)!!

    // AnnotationUsageInfo
    internal val annotationUsageInfoType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(IntrospektNames.AnnotationUsageInfo.id)) {
            "Cannot find AnnotationInfo type, Introspekt runtime library is most likely missing"
        }
    internal val annotationUsageInfoConstructor: IrConstructorSymbol =
        requireNotNull(pluginContext.referenceConstructors(IntrospektNames.AnnotationUsageInfo.id)).first()

    // SourceLocation
    internal val sourceLocationType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(IntrospektNames.SourceLocation.id)) {
            "Cannot find SourceLocation type, Introspekt runtime library is most likely missing"
        }
    internal val sourceLocationCompanionType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(IntrospektNames.SourceLocation.Companion.id)) {
            "Cannot find SourceLocation.Companion type, Introspekt runtime library is most likely missing"
        }
    internal val sourceLocationGetOrCreate: IrSimpleFunctionSymbol =
        pluginContext.referenceFunctions(IntrospektNames.SourceLocation.Companion.getOrCreate).first()

    // FunctionInfo
    internal val functionInfoType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(IntrospektNames.FunctionInfo.id)) {
            "Cannot find FunctionInfo type, Introspekt runtime library is most likely missing"
        }
    internal val functionInfoCompanionType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(IntrospektNames.FunctionInfo.Companion.id)) {
            "Cannot find FunctionInfo.Commpanion type, Introspekt runtime library is most likely missing"
        }
    internal val functionInfoGetOrCreate: IrSimpleFunctionSymbol =
        pluginContext.referenceFunctions(IntrospektNames.FunctionInfo.Companion.getOrCreate).first()

    // PropertyInfo
    internal val propertyInfoType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(IntrospektNames.PropertyInfo.id)) {
            "Cannot find PropertyInfo type, Introspekt runtime library is most likely missing"
        }
    internal val propertyInfoCompanionType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(IntrospektNames.PropertyInfo.Companion.id)) {
            "Cannot find PropertyInfo.Companion type, Introspekt runtime library is most likely missing"
        }
    internal val propertyInfoGetOrCreate: IrSimpleFunctionSymbol =
        pluginContext.referenceFunctions(IntrospektNames.PropertyInfo.Companion.getOrCreate).first()

    // FieldInfo
    internal val fieldInfoType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(IntrospektNames.FieldInfo.id)) {
            "Cannot find FieldInfo type, Introspekt runtime library is most likely missing"
        }
    internal val fieldInfoCompanionType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(IntrospektNames.FieldInfo.Companion.id)) {
            "Cannot find FieldInfo.Companion type, Introspekt runtime library is most likely missing"
        }
    internal val fieldInfoGetOrCreate: IrSimpleFunctionSymbol =
        pluginContext.referenceFunctions(IntrospektNames.FieldInfo.Companion.getOrCreate).first()

    // ClassInfo
    internal val classInfoType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(IntrospektNames.ClassInfo.id)) {
            "Cannot find ClassInfo type, Introspekt runtime library is most likely missing"
        }
    internal val classInfoCompanionType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(IntrospektNames.ClassInfo.Companion.id)) {
            "Cannot find ClassInfo.Companion type, Introspekt runtime library is most likely missing"
        }
    internal val classInfoGetOrCreate: IrSimpleFunctionSymbol =
        pluginContext.referenceFunctions(IntrospektNames.ClassInfo.Companion.getOrCreate).first()

    // LocalInfo
    internal val localInfoType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(IntrospektNames.LocalInfo.id)) {
            "Cannot find LocalInfo type, Introspekt runtime library is most likely missing"
        }
    internal val localInfoCompanionType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(IntrospektNames.LocalInfo.Companion.id)) {
            "Cannot find LocalInfo.Companion type, Introspekt runtime library is most likely missing"
        }
    internal val localInfoGetOrCreate: IrSimpleFunctionSymbol =
        pluginContext.referenceFunctions(IntrospektNames.LocalInfo.Companion.getOrCreate).first()

    // ParameterInfo
    internal val parameterInfoType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(IntrospektNames.ParameterInfo.id)) {
            "Cannot find LocalInfo type, Introspekt runtime library is most likely missing"
        }
    internal val parameterInfoCompanionType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(IntrospektNames.ParameterInfo.Companion.id)) {
            "Cannot find LocalInfo.Companion type, Introspekt runtime library is most likely missing"
        }
    internal val parameterInfoGetOrCreate: IrSimpleFunctionSymbol =
        pluginContext.referenceFunctions(IntrospektNames.ParameterInfo.Companion.getOrCreate).first()

    // TypeInfo
    internal val typeInfoType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(IntrospektNames.TypeInfo.id)) {
            "Cannot find TypeInfo type, Introspekt runtime library is most likely missing"
        }

    // SimpleTypeInfo
    internal val simpleTypeInfoType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(IntrospektNames.SimpleTypeInfo.id)) {
            "Cannot find SimpleTypeInfo type, Introspekt runtime library is most likely missing"
        }
    internal val typeInfoCompanionType: IrClassSymbol =
        requireNotNull(pluginContext.referenceClass(IntrospektNames.TypeInfo.Companion.id)) {
            "Cannot find SimpleTypeInfo.Companion type, Introspekt runtime library is most likely missing"
        }
    internal val typeInfoGetOrCreate: IrSimpleFunctionSymbol =
        pluginContext.referenceFunctions(IntrospektNames.TypeInfo.Companion.getOrCreate).first()

    // InlineDefaults
    internal val inlineDefaultsType: IrClassSymbol = pluginContext.referenceClass(IntrospektNames.InlineDefaults.id)!!
    internal val inlineDefaultsConstructor: IrConstructorSymbol =
        pluginContext.referenceConstructors(IntrospektNames.InlineDefaults.id).first()
    internal val inlineDefaultsModeType: IrClassSymbol =
        pluginContext.referenceClass(IntrospektNames.InlineDefaults.Mode.id)!!

    internal val visibilityModifierType: IrClassSymbol =
        pluginContext.referenceClass(IntrospektNames.VisibilityModifier.id)!!
    internal val modalityModifierType: IrClassSymbol =
        pluginContext.referenceClass(IntrospektNames.ModalityModifier.id)!!
    internal val classModifierType: IrClassSymbol = pluginContext.referenceClass(IntrospektNames.ClassModifier.id)!!
}