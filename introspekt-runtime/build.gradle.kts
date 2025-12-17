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

import com.android.build.api.dsl.KotlinMultiplatformAndroidCompilation
import com.android.build.api.dsl.androidLibrary
import dev.karmakrafts.conventions.configureJava
import dev.karmakrafts.conventions.setProjectInfo
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import java.time.ZonedDateTime

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.dokka)
    signing
    `maven-publish`
}

configureJava(libs.versions.java)

@Suppress("UnstableApiUsage")
@OptIn(ExperimentalWasmDsl::class, ExperimentalKotlinGradlePluginApi::class)
kotlin {
    withSourcesJar(true)
    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlin.uuid.ExperimentalUuidApi")
    }
    mingwX64()
    linuxX64()
    linuxArm64()
    macosX64()
    macosArm64()
    androidLibrary {
        namespace = "$group.${rootProject.name}"
        compileSdk = libs.versions.androidCompileSDK.get().toInt()
        minSdk = libs.versions.androidMinimalSDK.get().toInt()
    }
    androidNativeArm32()
    androidNativeArm64()
    androidNativeX64()
    androidNativeX86()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    tvosArm64()
    tvosX64()
    tvosSimulatorArm64()
    watchosArm32()
    watchosArm64()
    watchosX64()
    watchosSimulatorArm64()
    jvm()
    js {
        browser {
            useEsModules()
        }
        nodejs {
            useEsModules()
        }
    }
    wasmJs {
        browser {
            useEsModules()
        }
        nodejs {
            useEsModules()
        }
    }
    applyDefaultHierarchyTemplate {
        common {
            group("jvmAndAndroid") {
                withJvm()
                // withAndroidTarget() does not work with androidLibrary {} DSL..
                withCompilations { it is KotlinMultiplatformAndroidCompilation }
            }
        }
    }
    sourceSets {
        commonMain {
            dependencies {
                api(libs.stately.common)
                api(libs.stately.concurrent.collections)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}

dokka {
    moduleName = project.name
    pluginsConfiguration {
        html {
            footerMessage = "&copy; ${ZonedDateTime.now().year} Karma Krafts & associates"
        }
    }
}

val dokkaJar by tasks.registering(Jar::class) {
    group = "dokka"
    from(tasks.dokkaGeneratePublicationHtml.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}

tasks {
    System.getProperty("publishDocs.root")?.let { docsDir ->
        register("publishDocs", Copy::class) {
            dependsOn(dokkaJar)
            mustRunAfter(dokkaJar)
            from(zipTree(dokkaJar.get().outputs.files.first()))
            into(docsDir)
        }
    }
}

publishing {
    setProjectInfo(
        name = "Introspekt Runtime",
        description = "Positional code and compile time introspection API for Kotlin/Multiplatform",
        url = "https://git.karmakrafts.dev/kk/introspekt"
    )
    publications.withType<MavenPublication> {
        artifact(dokkaJar)
    }
}