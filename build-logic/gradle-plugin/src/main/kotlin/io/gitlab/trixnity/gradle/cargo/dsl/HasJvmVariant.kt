/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.cargo.dsl

import io.gitlab.trixnity.gradle.Variant
import org.gradle.api.provider.Property

interface HasJvmVariant {
    /**
     * The variant to use for Desktop JVM builds. Defaults to `Variant.Debug`. Setting this will override the
     * `jvmVariant` properties in outer blocks. For example, in the following DSL:
     * ```kotlin
     * cargo {
     *   jvmVariant = Variant.Debug
     *   builds.mingw {
     *     jvmVariant = Variant.Release
     *   }
     * }
     * ```
     * the resulting `.jar` will have a release `.dll` file and debug `.so` and `.dylib` files.
     */
    val jvmVariant: Property<Variant>
}
