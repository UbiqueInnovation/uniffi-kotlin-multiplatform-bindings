package dev.oxikt.gradle.plugin.utils

/** The rustc arguments that make it report the linker options for a static library's native dependencies. */
val PRINT_NATIVE_STATIC_LIBS: List<String> = listOf("--print", "native-static-libs")

/**
 * The linker options for a static library's native dependencies, from the output of a Cargo build
 * with [PRINT_NATIVE_STATIC_LIBS], or null if rustc reported none. Cargo repeats the report when
 * the library is up to date.
 */
fun nativeStaticLibs(cargoOutput: String): String? = cargoOutput.lineSequence()
    .map { it.trim().substringAfter("note: native-static-libs: ", "") }
    .firstOrNull(String::isNotEmpty)
