/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.cargo.rust.targets

/**
 * Represents a Rust target that can be statically linked to a Kotlin/Native library or an application.
 */
sealed interface RustNativeTarget : RustTarget {
    /**
     * The name of the platform (without the CPU architecture) used in cinterop.
     */
    val cinteropName: String
}
