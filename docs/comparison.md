# Comparison with Gobley

[Gobley](https://github.com/gobley/gobley) also generates Kotlin Multiplatform bindings for Rust
libraries with UniFFI, and also comes with Gradle plugins.

!!! note "Work in progress"
    This page is a placeholder. The comparison still has to be written.

To cover:

- Supported UniFFI versions and targets
- Gradle setup: one plugin and DSL here, compared with Gobley's plugins
- Multi-module support: separate Kotlin libraries sharing types without copying
- Runtime library vs. helper code generated per crate
- Android setup (AGP 9, Android KMP library plugin)
- Build performance options (shared Cargo target directory, sccache, ABI selection)
- Maturity and production use
