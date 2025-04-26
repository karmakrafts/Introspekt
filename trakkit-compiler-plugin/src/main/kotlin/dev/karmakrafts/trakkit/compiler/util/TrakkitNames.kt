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

package dev.karmakrafts.trakkit.compiler.util

import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal object TrakkitNames {
    val packageName: FqName = FqName("dev.karmakrafts.trakkit")

    object Functions {
        val listOf: Name = Name.identifier("listOf")
        val mapOf: Name = Name.identifier("mapOf")
        val of: Name = Name.identifier("of")
        val ofClass: Name = Name.identifier("ofClass")
        val ofFunction: Name = Name.identifier("ofFunction")
        val here: Name = Name.identifier("here")
        val hereHash: Name = Name.identifier("hereHash")
        val current: Name = Name.identifier("current")
        val currentFunction: Name = Name.identifier("currentFunction")
        val currentFunctionHash: Name = Name.identifier("currentFunctionHash")
        val currentClass: Name = Name.identifier("currentClass")
        val currentClassHash: Name = Name.identifier("currentClassHash")
        val getOrCreate: Name = Name.identifier("getOrCreate")
    }

    object Kotlin {
        val rootPackageName: FqName = FqName("kotlin")
        val collectionsPackageName: FqName = FqName("kotlin.collections")
        val listOf: CallableId = CallableId(collectionsPackageName, Functions.listOf)
        val mapOf: CallableId = CallableId(collectionsPackageName, Functions.mapOf)

        object List {
            val name: Name = Name.identifier("List")
            val id: ClassId = ClassId(collectionsPackageName, name)
            val fqName: FqName = id.asSingleFqName()
        }

        object Map {
            val name: Name = Name.identifier("Map")
            val id: ClassId = ClassId(collectionsPackageName, name)
            val fqName: FqName = id.asSingleFqName()
        }

        object Pair {
            val name: Name = Name.identifier("Pair")
            val id: ClassId = ClassId(rootPackageName, name)
            val fqName: FqName = id.asSingleFqName()
        }

        object Annotation {
            val name: Name = Name.identifier("Annotation")
            val id: ClassId = ClassId(rootPackageName, name)
            val fqName: FqName = id.asSingleFqName()
        }
    }

    object VisibilityModifier {
        val name: Name = Name.identifier("VisibilityModifier")
        val id: ClassId = ClassId(packageName, name)
        val fqName: FqName = id.asSingleFqName()
    }

    object ModalityModifier {
        val name: Name = Name.identifier("ModalityModifier")
        val id: ClassId = ClassId(packageName, name)
        val fqName: FqName = id.asSingleFqName()
    }

    object ClassModifier {
        val name: Name = Name.identifier("ClassModifier")
        val id: ClassId = ClassId(packageName, name)
        val fqName: FqName = id.asSingleFqName()
    }

    object SourceLocation {
        val name: Name = Name.identifier("SourceLocation")
        val id: ClassId = ClassId(packageName, name)
        val fqName: FqName = id.asSingleFqName()

        object Companion {
            val fqName: FqName = FqName("SourceLocation.Companion")
            val id: ClassId = ClassId(packageName, fqName, false)

            val here: CallableId = CallableId(packageName, fqName, Functions.here)
            val hereHash: CallableId = CallableId(packageName, fqName, Functions.hereHash)
            val currentFunction: CallableId = CallableId(packageName, fqName, Functions.currentFunction)
            val currentFunctionHash: CallableId = CallableId(packageName, fqName, Functions.currentFunctionHash)
            val currentClass: CallableId = CallableId(packageName, fqName, Functions.currentClass)
            val currentClassHash: CallableId = CallableId(packageName, fqName, Functions.currentClassHash)
            val ofClass: CallableId = CallableId(packageName, fqName, Functions.ofClass)
            val ofFunction: CallableId = CallableId(packageName, fqName, Functions.ofFunction)
            val getOrCreate: CallableId = CallableId(packageName, fqName, Functions.getOrCreate)
        }
    }

    object ElementInfo {
        val name: Name = Name.identifier("ElementInfo")
        val id: ClassId = ClassId(packageName, name)
        val fqName: FqName = id.asSingleFqName()
    }

    object FunctionInfo {
        val name: Name = Name.identifier("FunctionInfo")
        val id: ClassId = ClassId(packageName, name)
        val fqName: FqName = id.asSingleFqName()

        object Companion {
            val fqName: FqName = FqName("FunctionInfo.Companion")
            val id: ClassId = ClassId(packageName, fqName, false)

            val current: CallableId = CallableId(packageName, fqName, Functions.current)
            val of: CallableId = CallableId(packageName, fqName, Functions.of)
            val getOrCreate: CallableId = CallableId(packageName, fqName, Functions.getOrCreate)
        }
    }

    object ClassInfo {
        val name: Name = Name.identifier("ClassInfo")
        val id: ClassId = ClassId(packageName, name)
        val fqName: FqName = id.asSingleFqName()

        object Companion {
            val fqName: FqName = FqName("ClassInfo.Companion")
            val id: ClassId = ClassId(packageName, fqName, false)

            val current: CallableId = CallableId(packageName, fqName, Functions.current)
            val of: CallableId = CallableId(packageName, fqName, Functions.of)
            val getOrCreate: CallableId = CallableId(packageName, fqName, Functions.getOrCreate)
        }
    }

    object AnnotationUsageInfo {
        val name: Name = Name.identifier("AnnotationUsageInfo")
        val id: ClassId = ClassId(packageName, name)
        val fqName: FqName = id.asSingleFqName()
    }

    object PropertyInfo {
        val name: Name = Name.identifier("PropertyInfo")
        val id: ClassId = ClassId(packageName, name)
        val fqName: FqName = id.asSingleFqName()

        object Companion {
            val fqName: FqName = FqName("PropertyInfo.Companion")
            val id: ClassId = ClassId(packageName, fqName, false)

            val getOrCreate: CallableId = CallableId(packageName, fqName, Functions.getOrCreate)
        }
    }

    object TrakkitIntrinsic {
        val name: Name = Name.identifier("TrakkitIntrinsic")
        val id: ClassId = ClassId(packageName, name)
        val fqName: FqName = id.asSingleFqName()
    }

    object CaptureCaller {
        val name: Name = Name.identifier("CaptureCaller")
        val id: ClassId = ClassId(packageName, name)
        val fqName: FqName = id.asSingleFqName()
    }
}