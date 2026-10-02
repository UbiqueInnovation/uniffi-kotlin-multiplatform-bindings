# Swift interop

On Apple targets you can combine this plugin with
[spmForKmp](https://github.com/frankois944/spm4Kmp), which lets Kotlin call Swift code through
cinterop. The same module can then call both Rust and Swift.

```kotlin
plugins {
    kotlin("multiplatform")
    id("io.github.frankois944.spmForKmp") version "1.9.5"
    id("ch.ubique.uniffi.plugin")
}

uniffi {
    generateFromLibrary()
}

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.swiftPackageConfig(cinteropName = "swiftGreeter") {
            minIos = "16.4"
        }
    }
}
```

```kotlin
greetFromRust("Kotlin")                 // from the generated bindings
swiftGreeter.Greeter.greetWithName("Kotlin")   // from Swift, via spmForKmp
```

Use spmForKmp `1.9.5` or newer. Older versions configured their cinterop in a way that conflicted
with this plugin, and needed a workaround in the build script.

## Fixtures

- [`examples/swift-interop`](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/tree/main/examples/swift-interop)
