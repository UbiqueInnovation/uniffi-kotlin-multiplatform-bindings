# Testing

## What there is

| Location | What it tests | How to run |
| --- | --- | --- |
| `tests/uniffi/*` | Generated bindings, one fixture per feature, on every target | `./gradlew :tests:uniffi:<name>:allTests` |
| `tests/runtime` | The runtime through generated bindings | `./gradlew :tests:runtime:allTests` |
| `runtime/src/*Test` | The runtime itself, including Android host and device tests | `./gradlew :runtime:allTests` |
| `examples/*` | That the examples build and their tests pass | `./gradlew :examples:<name>:allTests` |
| `build-logic/gradle-plugin/src/test` | Plugin unit tests | `./gradlew -p build-logic check` |
| `cargo test` | The Rust crates in the workspace, including the bindgen | `cargo test` |

CI (`.github/workflows/run-tests.yml`) runs `cargo test`, `./gradlew -p build-logic check` and
`./gradlew build` on Linux. It cross-compiles for Android, Windows (MinGW) and `aarch64` Linux.
Apple targets are not built in CI. Run them locally on a Mac before merging changes to native
code.

## Fixtures

Most fixtures in `tests/uniffi/` are ports of the fixtures in UniFFI's own repository, with the
Kotlin tests turned into common tests. Each fixture is a Rust crate and a Gradle module:

```
tests/uniffi/callbacks/
├── Cargo.toml            crate-type = ["cdylib", "staticlib"], or with "lib" when it is depended upon
├── build.rs              generate_scaffolding_from_current_dir() for UDL fixtures
├── build.gradle.kts      plugins { id("uniffi-tests-from-library") }
├── uniffi.toml           package_name = "callbacks"
└── src/
    ├── callbacks.udl
    ├── commonMain/rust/lib.rs
    └── commonTest/kotlin/CallbacksTest.kt
```

- `uniffi-tests-from-library` (`build-logic/conventions/`) applies the plugin with
  `bindgenFromPath(<repo>/bindgen)` and `generateFromLibrary()`, declares all targets the host can
  build, and adds the test dependencies (`kotlin("test")`, kotest assertions, coroutines-test). It
  sets `addRuntime = false` and depends on `project(":runtime")` instead, so tests always run
  against the runtime in the working tree.
- `tests/build-common` provides `generate_scaffolding_from_current_dir()`, which finds the UDL file
  from the directory name.
- Every fixture is a member of the Cargo workspace in the root `Cargo.toml` and is included in
  `settings.gradle.kts`.

### Adding a fixture

1. Create the directory with the files above. Copy a similar fixture as a starting point.
2. Add it to `members` in the root `Cargo.toml` and `include(":tests:uniffi:<name>")` to
   `settings.gradle.kts`.
3. Write tests in `src/commonTest/kotlin`. Use `jvmTest` or `nativeTest` only for platform-specific
   behaviour.

## Working on the generator

All fixtures use the bindgen from the working tree (`bindgenFromPath`), which is reinstalled when
its sources change. A typical loop:

```bash
./gradlew :tests:uniffi:coverall:jvmTest           # fast feedback
./gradlew :tests:uniffi:coverall:macosArm64Test    # check native too
```

Look at the generated code in `tests/uniffi/<name>/build/uniffi/bindings/`. Compile errors usually
point there.

!!! tip
    JVM and Native get their FFI declarations from different places (JNA classes vs. C headers).
    A change can compile on one and break the other. Run at least one JVM and one native target.

## Debugging crashes

- A test that crashes the process (SIGABRT, exit code 134) produces no console output, because the
  JVM dies before Gradle collects it. Read `build/test-results/<task>/TEST-*.xml`, or run the test
  class on its own with `--info`.
- For Rust panics, set `RUST_BACKTRACE=1` in the environment of the test task.
- On JVM, `-Djna.debug_load=true` shows where JNA looks for the library.
