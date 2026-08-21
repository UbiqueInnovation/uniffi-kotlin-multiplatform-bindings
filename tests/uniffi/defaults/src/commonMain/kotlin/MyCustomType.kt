/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package defaults.custom

/**
 * The Kotlin counterpart of the Rust `CustomType`, which wraps an `i64` but
 * crosses the FFI as a `String`.
 *
 * The `lift`/`lower` expressions in `uniffi.toml` convert between this and the
 * builtin `kotlin.String`, so the conversion is deliberately not the identity.
 */
data class MyCustomType(val value: Long)
