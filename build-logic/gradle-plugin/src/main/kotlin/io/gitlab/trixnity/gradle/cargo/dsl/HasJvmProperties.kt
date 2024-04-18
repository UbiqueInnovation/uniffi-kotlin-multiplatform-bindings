/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.cargo.dsl

import org.gradle.api.provider.Property

interface HasJvmProperties : HasJvmVariant {
    /**
     * The resource prefix to use. In the `.jar` file, the Rust dynamic library will be located in
     * `<resource prefix>/<dynamic library>`. This defaults to `RustJvmTarget.jnaResourcePrefix`. The library then can
     * be loaded using `com.sun.jna.Native.register`. Users not using JNA can customize this property and implement
     * their own loading logic using `java.lang.ClassLoader`. If this property is empty, then the dynamic library
     * will be copied to the root of the `.jar` file.
     */
    val resourcePrefix: Property<String>

    /**
     * Determines whether to include the Rust dynamic library in the resulting `.jar` file. Defaults to `true`. When
     * the host does not support building for this target, this property is ignored and considered `false`.
     */
    val jvm: Property<Boolean>
}
