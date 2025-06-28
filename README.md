# Introspekt

[![](https://git.karmakrafts.dev/kk/introspekt/badges/master/pipeline.svg)](https://git.karmakrafts.dev/kk/introspekt/-/pipelines)
[![](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo.maven.apache.org%2Fmaven2%2Fdev%2Fkarmakrafts%2Fintrospekt%2Fintrospekt-runtime%2Fmaven-metadata.xml
)](https://git.karmakrafts.dev/kk/introspekt/-/packages)
[![](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Fcentral.sonatype.com%2Frepository%2Fmaven-snapshots%2Fdev%2Fkarmakrafts%2Fintrospekt%2Fintrospekt-runtime%2Fmaven-metadata.xml
)](https://git.karmakrafts.dev/kk/introspekt/-/packages)

Introspekt is a positional code API and introspection framework for Kotlin Multiplatform.  
It currently adds the following features:

* `SourceLocation` type with access to module name, file path, function name, line and column
* `FunctionInfo` type for introspecting current/caller function signature and location
* `ClassInfo` type for introspecting current/caller classes including functions
* `AnnotationUsageInfo` type for introspecting annotation types and their parameters
* `TypeInfo` type for fundamental RTTI integrated with **kotlin.reflect** to fix gaps on Kotlin/JS and other platforms
* Default-parameter inlining for intrinsic types listed above (like `std::source_location` in C++)
* Compile-time evaluation of location hashes to improve runtime performance for positional memoization
* Tracing API for automatically injecting trace callbacks into code, including manual spans and events

### How to use it

First, add the official Maven Central repository to your `settings.gradle.kts`:

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
    id("dev.karmakrafts.introspekt.introspekt-gradle-plugin") version "<version>"
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation("dev.karmakrafts.introspekt:introspekt-runtime:<version>")
            }
        }
    }
}
```

### Tracing

Tracing can be used to automatically inject callbacks into the code to collect certain events.  
The events can be processed/collected using an implementation of `TraceCollector`.  
The following example illustrates a simple use-case for logging:

```kotlin
object MyCollector : TraceCollector {
    private val logger: MyLogger = MyLogger() // Some logger from another library
    
    fun enterSpan(span: TraceSpan) = logger.trace("Entering span ${span.name}")
    fun leaveSpan(span: TraceSpan, end: SourceLocation) = logger.trace("Leaving span ${span.name}")
    fun enterFunction(function: FunctionInfo) { /* ... */ }
    fun leaveFunction(function: FunctionInfo) { /* ... */ }
    fun call(callee: FunctionInfo, caller: FunctionInfo, location: SourceLocation) { /* ... */ }
    fun event(event: TraceEvent) { /* ... */ }
}

fun main() {
    // Register the collector before any code we want to trace is executed
    TraceCollector.register(MyCollector)
    // Enter code we want to trace
    functionIWantToTrace()
}

@Trace // This will inject callbacks after every call, and when entering/leaving functions
fun functionIWantToTrace() {
    TraceSpan.enter("My awesome span")
    // ...
    TraceSpan.leave()
    // ...
}
```