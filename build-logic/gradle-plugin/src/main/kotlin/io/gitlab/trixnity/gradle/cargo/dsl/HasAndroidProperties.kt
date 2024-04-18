/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.cargo.dsl

import org.gradle.api.provider.SetProperty

interface HasAndroidProperties {
    /**
     * The names of dynamic NDK libraries required in runtime without the prefix and the file extension. For example,
     * the following DSL will copy `libc++_shared.so` and `<API Level>/libaaudio.so` from the NDK directory to the app.
     * ```kotlin
     * cargo {
     *   builds.android {
     *     ndkLibraries = arrayOf("aaudio", "c++_shared")
     *   }
     * }
     * ```
     */
    val ndkLibraries: SetProperty<String>
}
