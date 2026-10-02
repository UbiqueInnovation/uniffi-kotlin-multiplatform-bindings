# Gradle plugin

Source: `build-logic/gradle-plugin/src/main/kotlin/ch/ubique/uniffi/plugin/`.

| File | Role |
| --- | --- |
| `UniffiPlugin.kt` | Entry point. Registers the extensions and tasks and wires everything into the Kotlin source sets. |
| `dsl/UniffiExtension.kt`, `dsl/CargoExtension.kt` | The `uniffi { }` and `cargo { }` DSL. |
| `model/BuildTarget.kt` | Kotlin targets ↔ Rust targets, debug/release target sets, library file names. |
| `services/CargoMetadataService.kt` | A `ValueSource` running `cargo metadata --no-deps`. |
| `tasks/` | One class per task type. |
| `utils/CargoRunner.kt` | Runs `cargo` (or `cross`) and installs missing Rust targets with `rustup`. |
| `android/AndroidSupport.kt` | The only file that touches AGP types. |

## Configuration

`UniffiPlugin.apply()`:

1. Creates the `uniffi` and `cargo` extensions.
2. Reads `-PreleaseBuild` and `idea.sync.active` (set by IntelliJ during a sync).
3. Gets `cargo metadata` through `CargoMetadataService`. It runs with `--no-deps`, so configuration
   doesn't resolve or download the dependency graph. From the result it takes the package name,
   the library name and the target directory (`CargoInfo`).
4. Registers `installBindgen`, the host build and `buildBindings`.
5. Reacts to the Kotlin Multiplatform plugin. For every Kotlin target it recognises
   (`BuildTarget.fromTargetName`) it registers Cargo builds and wires the outputs, see below.
6. In `afterEvaluate`, checks that the KMP plugin is applied, that a `generateFrom*` call was made,
   and that cinterop commonization is on if there is a native target. It also makes
   `prepareKotlinIdeaImport` depend on `buildBindings`, so the IDE sees generated code after a sync.

Everything is configuration-cache compatible. Extensions hold `ObjectFactory`, not `Project`, and
tasks receive providers.

## Tasks

| Task | Type | Output |
| --- | --- | --- |
| `installBindgen` | `InstallBindgenTask` | the bindgen binary, plus a per-project `.ready` marker |
| `cargoBuild<RustTarget><Debug\|Release>` | `CargoBuildTask` | static and/or dynamic library in Cargo's target dir |
| `buildLibraryForBindings` | `CargoBuildAliasTask` | alias for the host `cargoBuild*Debug` |
| `buildBindings` | `BuildBindingsTask` | `build/uniffi/bindings/` |
| `mergeUniffiJvmResources` | `MergeLibrariesTask` | JVM resources tree |
| `mergeUniffiAndroidJniLibs` | `MergeLibrariesTask` | Android `jniLibs` tree |
| `mergeUniffiAndroidHostTestResources` | `MergeLibrariesTask` | resources for Android host tests |
| `generateDefFileFor<Target>` | `GenerateDefFileTask` | `build/uniffi/cinterop/uniffi-<Target>.def` |
| `generateDummyDefFile` | `GenerateDummyDefFileTask` | a def file without a library, during IDE sync |

Task dependencies follow from wiring one task's output provider into another task's input, so
Gradle infers them. The only explicit `dependsOn` is `prepareKotlinIdeaImport` → `buildBindings`.

### Cargo builds

`CargoBuildTask` runs

```
cargo rustc --lib [--target <triple>] --package <name> [--release] --crate-type <types>
```

with `CARGO_TARGET_DIR` set. Some details:

- **One task per Rust target and profile.** `registerCargoBuildTask` uses `maybeRegister`: if two
  Kotlin targets need the same Rust target (the host build for bindings and the JVM debug build),
  the second request reuses the task and adds its crate type. `--crate-type` makes Cargo build
  only what is needed, regardless of `Cargo.toml`.
- **Outputs in Cargo's own target directory.** Library paths are derived from Cargo's layout
  (`<target-dir>/<triple>/<profile>/`), not copied, so Cargo's incremental cache is shared
  between Gradle modules and with plain `cargo` invocations.
- **Android** targets get the NDK's clang as compiler and linker, and `llvm-ar`, through the usual
  `CC_<triple>` / `CARGO_TARGET_<TRIPLE>_LINKER` environment variables, computed in
  `AndroidSupport.ndkEnvironment` / `NdkUtil`. With `useCross` they get nothing, because `cross`
  brings its own toolchain.
- **Missing Rust targets.** `CargoRunner` recognises Cargo's "consider downloading the target with
  `rustup target add`" message, runs that command and retries.

### Installing the bindgen

`InstallBindgenTask` runs `cargo install --locked --force --root <dir>` from the configured
`BindgenSource` (path, git or registry). The installation is shared by all projects in the
build and lives in the root project's `build/uniffi/bindgen/<cacheKey>/`.

Because the executable is shared, it is not declared as a task output. Two projects can't own the
same output. Instead each project writes its own `.ready` marker, which `buildBindings` takes as
input. A file lock (`withCargoTargetLock`) serialises concurrent installations, and a
fingerprint file decides whether reinstalling is needed:

| Source | Fingerprint |
| --- | --- |
| path | hash of the sources and the workspace manifests and lockfile |
| git tag / revision, registry | the source definition itself |
| git branch / default branch | the commit from `git ls-remote`, so new commits trigger a reinstall |

### Generating bindings

`BuildBindingsTask` deletes the output directory, then runs

```
uniffi-bindgen-kotlin-multiplatform --library <host cdylib> --out-dir build/uniffi/bindings [--crate <lib name>]
```

or passes the UDL file instead of `--library`. `--crate` limits generation to the module's own crate,
and is left out when `generateBindingsForExternalCrates` is on. Its inputs are all `*.rs` files,
`Cargo.toml`, `Cargo.lock` and `uniffi.toml` in the package directory, the host library and the
bindgen marker. The five output directories are separate `@OutputDirectory` properties so that
each source set can depend on exactly one of them.

### Native targets

For each Kotlin/Native target, `GenerateDefFileTask` writes:

```
staticLibraries = libfoo.a
libraryPaths = <cargo target dir>/<triple>/<profile>
headers = <all generated .h files>
compilerOpts = -I<headers dir>
linkerOpts = <native-static-libs> [--allow-multiple-definition]
```

`linkerOpts` comes from asking rustc for `--print native-static-libs`, the system libraries the
static library needs. `--allow-multiple-definition` is added on Linux and Windows, because several
UniFFI static libraries in one binary can contain the same Rust symbols (see
[External and remote types](external-and-remote-types.md#multi-module-builds)).

The plugin registers a cinterop called `uniffi-cinterop` with package `cinterop` on each target's
`main` compilation. The name ends up in the KLIB's identity, so it must not change. The static
library is also added as an input of the cinterop task, so that cinterop reruns when the library
changes.

During an IDE sync, `GenerateDummyDefFileTask` writes a def file with only the headers, and no
native Cargo builds are registered. The IDE gets declarations without a cross-compile.

### JVM and Android

`MergeLibrariesTask` collects the dynamic libraries of several Rust targets into one tree:

```
<out>/darwin-aarch64/libfoo.dylib     JVM resources (JNA's resource prefix)
<out>/arm64-v8a/libfoo.so             Android jniLibs (ABI name)
```

It uses `sync`, so libraries of targets that were dropped don't linger. The JVM tree is added as a
resource directory of `jvmMain`. The Android trees are given to AGP through the variant API
(`addGeneratedSourceDirectory`) for `jniLibs` and for host test resources.

## Android Gradle Plugin isolation

AGP is a `compileOnly` dependency. A consumer without Android doesn't have it on the classpath.
All AGP types are therefore confined to `AndroidSupport`, which is only instantiated inside
`pluginManager.withPlugin("com.android.kotlin.multiplatform.library")` or for Android Rust targets.
Don't reference AGP classes from anywhere else, or non-Android builds fail with
`NoClassDefFoundError`.

## Dependencies added

`configureCommonMain`, `configureJvmTarget` and `configureAndroidTarget` add the runtime and the
libraries from [Dependencies](../guide/configuration/dependencies.md). Versions are in
`Constants.kt`. The runtime version is generated into `PluginVersions` at build time and always
equals the plugin version.
