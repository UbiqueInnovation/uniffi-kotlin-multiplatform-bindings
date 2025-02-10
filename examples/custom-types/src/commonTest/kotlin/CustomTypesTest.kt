/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import io.gitlab.trixnity.uniffi.examples.customtypes.*
import io.kotest.matchers.shouldBe
import io.ktor.http.Url
import kotlin.test.Test

class CustomTypesTest {
    @Test
    fun testCustomBinding() {
        // Get the custom types and check their data
        val demo = getCustomTypesDemo(null)
        // URL is customized on the bindings side
        demo.url shouldBe Url("http://example.com/")
        // Handle isn't so it appears as a plain Long
        demo.handle shouldBe 123L

        // Change some data and ensure that the round-trip works
        demo.url = Url("http://new.example.com/")
        demo.handle = 456
        demo shouldBe getCustomTypesDemo(demo)
    }
}