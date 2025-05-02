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

import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal object IntrospektNames {
    val packageName: FqName = FqName("dev.karmakrafts.introspekt")

    object Functions {
        val listOf: Name = Name.identifier("listOf")
        val mapOf: Name = Name.identifier("mapOf")
        val of: Name = Name.identifier("of")
        val getOrCreate: Name = Name.identifier("getOrCreate")
        val current: Name = Name.identifier("current")
        val create: Name = Name.identifier("create")

        val ofClass: Name = Name.identifier("ofClass")
        val ofFunction: Name = Name.identifier("ofFunction")
        val here: Name = Name.identifier("here")
        val hereHash: Name = Name.identifier("hereHash")

        val currentFunction: Name = Name.identifier("currentFunction")
        val currentFunctionHash: Name = Name.identifier("currentFunctionHash")

        val currentClass: Name = Name.identifier("currentClass")
        val currentClassHash: Name = Name.identifier("currentClassHash")

        val enter: Name = Name.identifier("enter")
        val leave: Name = Name.identifier("leave")

        val event: Name = Name.identifier("event")

        val call: Name = Name.identifier("call")
        val enterFunction: Name = Name.identifier("enterFunction")
        val leaveFunction: Name = Name.identifier("leaveFunction")
        val loadProperty: Name = Name.identifier("loadProperty")
        val storeProperty: Name = Name.identifier("storeProperty")
        val loadLocal: Name = Name.identifier("loadLocal")
        val storeLocal: Name = Name.identifier("storeLocal")
    }

    object Kotlin {
        val rootPackageName: FqName = FqName("kotlin")
        val collectionsPackageName: FqName = FqName("kotlin.collections")
        val listOf: CallableId = CallableId(collectionsPackageName, Functions.listOf)
        val mapOf: CallableId = CallableId(collectionsPackageName, Functions.mapOf)

        object List {
            val name: Name = Name.identifier("List")
            val id: ClassId = ClassId(collectionsPackageName, name)
        }

        object Map {
            val name: Name = Name.identifier("Map")
            val id: ClassId = ClassId(collectionsPackageName, name)
        }

        object Pair {
            val name: Name = Name.identifier("Pair")
            val id: ClassId = ClassId(rootPackageName, name)
        }

        object Annotation {
            val name: Name = Name.identifier("Annotation")
            val id: ClassId = ClassId(rootPackageName, name)
        }
    }

    object VisibilityModifier {
        val name: Name = Name.identifier("VisibilityModifier")
        val id: ClassId = ClassId(packageName, name)
    }

    object ModalityModifier {
        val name: Name = Name.identifier("ModalityModifier")
        val id: ClassId = ClassId(packageName, name)
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

    object FunctionInfo {
        val name: Name = Name.identifier("FunctionInfo")
        val id: ClassId = ClassId(packageName, name)

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

    object FieldInfo {
        val name: Name = Name.identifier("FieldInfo")
        val id: ClassId = ClassId(packageName, name)
        val fqName: FqName = id.asSingleFqName()

        object Companion {
            val fqName: FqName = FqName("FieldInfo.Companion")
            val id: ClassId = ClassId(packageName, fqName, false)

            val getOrCreate: CallableId = CallableId(packageName, fqName, Functions.getOrCreate)
        }
    }

    object LocalInfo {
        val name: Name = Name.identifier("LocalInfo")
        val id: ClassId = ClassId(packageName, name)

        object Companion {
            val fqName: FqName = FqName("LocalInfo.Companion")
            val id: ClassId = ClassId(packageName, fqName, false)

            val getOrCreate: CallableId = CallableId(packageName, fqName, Functions.getOrCreate)
        }
    }

    object IntrospektIntrinsic {
        val name: Name = Name.identifier("IntrospektIntrinsic")
        val id: ClassId = ClassId(packageName, name)
        val fqName: FqName = id.asSingleFqName()
    }

    object IntrospektCompilerApi {
        val name: Name = Name.identifier("IntrospektCompilerApi")
        val id: ClassId = ClassId(packageName, name)
        val fqName: FqName = id.asSingleFqName()
    }

    object CaptureCaller {
        val name: Name = Name.identifier("CaptureCaller")
        val id: ClassId = ClassId(packageName, name)
        val fqName: FqName = id.asSingleFqName()
    }

    object TraceCollector {
        val name: Name = Name.identifier("TraceCollector")
        val id: ClassId = ClassId(packageName, name)

        object Companion {
            val fqName: FqName = FqName("TraceCollector.Companion")
            val id: ClassId = ClassId(packageName, fqName, false)

            val enterFunction: CallableId = CallableId(packageName, fqName, Functions.enterFunction)
            val leaveFunction: CallableId = CallableId(packageName, fqName, Functions.leaveFunction)
            val call: CallableId = CallableId(packageName, fqName, Functions.call)
            val loadProperty: CallableId = CallableId(packageName, fqName, Functions.loadProperty)
            val storeProperty: CallableId = CallableId(packageName, fqName, Functions.storeProperty)
            val loadLocal: CallableId = CallableId(packageName, fqName, Functions.loadLocal)
            val storeLocal: CallableId = CallableId(packageName, fqName, Functions.storeLocal)
        }
    }

    object TraceSpan {
        val name: Name = Name.identifier("TraceSpan")
        val id: ClassId = ClassId(packageName, name)

        object Companion {
            val fqName: FqName = FqName("TraceSpan.Companion")

            val enter: CallableId = CallableId(packageName, fqName, Functions.enter)
            val leave: CallableId = CallableId(packageName, fqName, Functions.leave)
        }
    }

    object FrameSnapshot {
        val name: Name = Name.identifier("FrameSnapshot")
        val id: ClassId = ClassId(packageName, name)

        object Companion {
            val fqName: FqName = FqName("FrameSnapshot.Companion")
            val id: ClassId = ClassId(packageName, fqName, false)

            val create: CallableId = CallableId(packageName, fqName, Functions.create)
            val empty: CallableId = CallableId(packageName, fqName, Name.identifier("empty"))
        }
    }

    object Trace {
        val name: Name = Name.identifier("Trace")
        val id: ClassId = ClassId(packageName, name)
        val fqName: FqName = id.asSingleFqName()

        object Companion {
            val fqName: FqName = FqName("Trace.Companion")
            val id: ClassId = ClassId(packageName, fqName, false)

            val event: CallableId = CallableId(packageName, fqName, Functions.event)
        }
    }

    object NoFrameCapture {
        val name: Name = Name.identifier("NoFrameCapture")
        val id: ClassId = ClassId(packageName, name)
        val fqName: FqName = id.asSingleFqName()
    }
}