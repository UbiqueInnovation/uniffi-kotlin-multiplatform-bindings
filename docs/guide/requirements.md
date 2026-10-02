# Requirements

| Requirement | Version |
| --- | --- |
| Rust | `>= 1.91` |
| UniFFI | `= 0.32.0` |
| Gradle | `>= 9.6.1` |
| Kotlin | `>= 2.4.0` |
| Android Gradle Plugin | `9.x`, only if you build for Android |
| JDK | 17 |

The UniFFI version must match exactly. The generated Kotlin depends on the FFI layout of a
specific UniFFI release. On JVM and Android the bindings check the version when the library is first loaded (see
[Troubleshooting](troubleshooting.md#runtime-errors)).

The project itself is built and tested with Rust `1.97.1`, Gradle `9.7.0`, Kotlin `2.4.0` and AGP
`9.3.1`.

## Don't use a custom global allocator

!!! warning
    Your crates must use Rust's default allocator. Don't set a `#[global_allocator]` (for example
    `mimalloc` or `jemalloc`) in any crate that is built with this plugin, or in its dependencies.

    Buffers that Kotlin sends to Rust are allocated by the UniFFI runtime's library, and freed by
    your crate. That only works when both use the same allocator, which is the system allocator
    by default. With a custom allocator, freeing such a buffer corrupts memory or crashes the
    process. See [Runtime](../internals/runtime.md#the-runtimes-own-rust-library) for details.

## Toolchain

- **Rust** via [rustup](https://rustup.rs). The plugin calls `cargo` and `rustup`, from `PATH` or
  from `~/.cargo/bin`. If a Rust target is missing, the plugin runs
  `rustup target add` for you.
- **Android NDK**, for Android targets. See [Targets](targets.md#android).
- **Xcode command line tools**, for Apple targets. Apple targets can only be built on macOS.
- **Cross toolchains** if you build release JVM artifacts for other desktop platforms. See
  [Targets](targets.md#jvm).
