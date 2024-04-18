/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import kotlin.time.Duration

expect object CargoOnlyLibrary {
    /**
     * Returns `"Hello, world!"`.
     */
    fun getHelloWorld(): String

    /**
     * Waits for the given duration.
     */
    suspend fun wait(duration: Duration)

    /**
     * Returns the opt-level of the Cargo profile used to build the Rust library. Note that the opt-level of `dev` and
     * `release` are 0 and 3 respectively.
     */
    val optLevel: Int

    /**
     * Returns the Cargo features used to build the Rust library.
     */
    val features: Array<String>
}
