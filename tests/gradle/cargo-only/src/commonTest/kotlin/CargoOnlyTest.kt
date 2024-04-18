/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

class CargoOnlyTest {
    @Test
    fun getStringTest() {
        CargoOnlyLibrary.getHelloWorld() shouldBe "Hello, world!"
    }

    @Test
    fun waitTest() = runTest {
        measureTime {
            CargoOnlyLibrary.wait(1.seconds)
        } shouldBeGreaterThanOrEqualTo 1.seconds
    }

    @Test
    fun optLevelTest() {
        // If `my-opt-level-2-profile` is applied correctly, this should not be 0 or 3, which are the opt-levels of
        // `dev` and `release`. We check the opt-level since there is no reliable way to get the name of the profile
        // in Rust code.
        CargoOnlyLibrary.optLevel shouldBe 2
    }

    @Test
    fun featureTest() {
        CargoOnlyLibrary.features.toSet() shouldBe setOf("feature2", "feature3", "feature5", "feature7")
    }
}
