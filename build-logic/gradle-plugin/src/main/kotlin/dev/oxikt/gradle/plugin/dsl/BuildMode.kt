package dev.oxikt.gradle.plugin.dsl

/** How the plugin runs Cargo, see [CargoExtension.buildMode]. */
enum class BuildMode {
    /**
     * The projects of a Cargo workspace share one `cargo build` per Rust target and profile, which
     * builds all their crates with the features of all of them. Each crate declares the crate types
     * its Kotlin targets need in `[lib] crate-type`.
     */
    Workspace,

    /**
     * Each project runs `cargo rustc` for its own crate, with exactly the crate types its Kotlin
     * targets need. With Cargo before 1.100, crates that other workspace crates depend on must not
     * declare a `cdylib`.
     */
    Individual,
}
