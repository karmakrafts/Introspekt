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

import dev.karmakrafts.conventions.configureJava
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.div
import kotlin.io.path.writeText

plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-gradle-plugin`
    signing
    `maven-publish`
}

configureJava(libs.versions.java)

java {
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    compileOnly(gradleApi())
    compileOnly(libs.kotlin.gradle.plugin)
}

kotlin {
    sourceSets {
        main {
            resources.srcDir("build/generated")
        }
    }
}

tasks {
    val sourcesJar by getting {
        dependsOn(compileJava)
    }
    val createVersionFile by registering {
        doFirst {
            val path = (layout.buildDirectory.asFile.get().toPath() / "generated" / "introspekt.version")
            path.deleteIfExists()
            path.parent.createDirectories()
            path.writeText(rootProject.version.toString())
        }
        outputs.upToDateWhen { false } // Always re-generate this file
    }
    processResources { dependsOn(createVersionFile) }
    compileKotlin { dependsOn(processResources) }
}

gradlePlugin {
    System.getenv("CI_PROJECT_URL")?.let {
        website = it
        vcsUrl = it
    }
    plugins {
        create("gradlePlugin") {
            id = "$group.${rootProject.name}-gradle-plugin"
            implementationClass = "$group.gradle.IntrospektGradlePlugin"
            displayName = "Introspekt Gradle Plugin"
            description = "Gradle plugin for applying the Introspekt Kotlin compiler plugin"
            tags.addAll("kotlin", "native", "interop", "codegen")
        }
    }
}