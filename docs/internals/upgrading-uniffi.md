# Upgrading UniFFI

The generated Kotlin has to match the FFI conventions of one exact UniFFI version. Upgrading is
mostly a matter of porting what changed in the upstream Kotlin generator to the templates here.

## Checklist

1. **Bump the versions.** `uniffi`, `uniffi_bindgen`, `uniffi_macros` and `uniffi_meta` in the
   workspace `Cargo.toml`, then `cargo update -p uniffi`. The bindgen is installed with
   `cargo install --locked`, so `Cargo.lock` must be committed in a working state.
2. **Diff the upstream Kotlin generator** between the old and the new version, in a checkout of
   [uniffi-rs](https://github.com/mozilla/uniffi-rs):

    ```bash
    git diff v0.32.0..v0.33.0 -- uniffi_bindgen/src/bindings/kotlin
    ```

    Most upstream templates have a counterpart in `bindgen/src/templates/generic/` or in the
    runtime. Port each change, and remember that the runtime has three copies
    (see [Runtime](runtime.md)).
3. **Check the FFI types.** New or changed `FfiType` variants need a mapping in
   `KotlinCodeOracle` for Kotlin (`ffi_type_label`) **and** for the C headers. Run a native build
   to make sure both agree.
4. **Check `FFI_BUILTINS`** in `mod.rs` against the renamed or new FFI definitions that are identical
   for every crate (see [Bindgen](bindgen.md#headers)).
5. **Check struct layouts** that Kotlin declares by hand: the callback vtable field order, the
   `ForeignFuture*` structs, `RustBuffer`, `RustCallStatus`. JNA declares these in Kotlin, so a
   layout change upstream doesn't cause a compile error. It only shows up as a crash.
6. **Port new upstream fixtures** from `fixtures/` in uniffi-rs to `tests/uniffi/`, and update the
   existing ones.
7. **Build everything**: `cargo test`, `./gradlew build`, and at least one Apple target locally.
8. **Document it**: requirements in the docs, the README, and consumer-facing migration notes in
   `CHANGELOG.md`.

## Things that changed in past upgrades

These are the kinds of changes to look out for. All of them happened between 0.28 and 0.32:

- Objects changed from raw pointers to opaque `u64` handles (0.30).
- `Type::External` was removed. External types became a query on the `ComponentInterface` (0.29).
- `module_path` became a full module path instead of the crate name (0.31).
- External metadata is attached to every `RustBuffer`, not only to external ones (0.32).
- Vtables gained a `clone` entry, and `free` moved to the front (0.30).
- Foreign-future types were renamed (`ForeignFutureFree` → `ForeignFutureDroppedCallback`, …).
- The custom type keys in `uniffi.toml` were renamed from `into_custom` / `from_custom` to `lift` /
  `lower` (0.29.1). Both are still accepted.
- Methods and trait exports on records and enums were added (0.31).
