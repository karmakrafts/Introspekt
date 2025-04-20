# Trakkit

[![](https://git.karmakrafts.dev/kk/trakkit/badges/master/pipeline.svg)](https://git.karmakrafts.dev/kk/trakkit/-/pipelines)
[![](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo.maven.apache.org%2Fmaven2%2Fdev%2Fkarmakrafts%2Ftrakkit%2Ftrakkit-runtime%2Fmaven-metadata.xml
)](https://git.karmakrafts.dev/kk/trakkit/-/packages)
[![](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Fcentral.sonatype.com%2Frepository%2Fmaven-snapshots%2Fdev%2Fkarmakrafts%2Ftrakkit%2Ftrakkit-runtime%2Fmaven-metadata.xml
)](https://git.karmakrafts.dev/kk/trakkit/-/packages)

Trakkit is a positional code API and introspection framework for Kotlin Multiplatform.  
It currently adds the following features:

* `SourceLocation` type with access to module name, file path, function name, line and column
* `FunctionInfo` type for introspecting current/caller function signature and location
* `ClassInfo` type for introspecting current/caller classes including functions
* `AnnotationInfo` type for introspecting annotation types and their parameters
* Default-parameter inlining for intrinsic types listed above (like `std::source_location` in C++)
* Support for any function type and constructors
* Compile-time evaluation of location hashes to improve runtime performance for positional memoization

### How to use it

First, add the official maven repositories to your `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        maven("https://central.sonatype.com/repository/maven-snapshots")
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        maven("https://central.sonatype.com/repository/maven-snapshots")
        mavenCentral()
    }
}
```

Then add a dependency on the plugin in your root buildscript:

```kotlin
plugins {
    id("dev.karmakrafts.trakkit.trakkit-gradle-plugin") version "<version>"
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation("dev.karmakrafts.trakkit:trakkit-runtime:<version>")
            }
        }
    }
}
```