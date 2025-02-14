/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import io.kotest.matchers.*
import simple_fns.*
import kotlin.test.*

class SimpleFnsTest {
    @Test
    fun testFunctions() {
        getString() shouldBe "String created by Rust"
        getInt() shouldBe 1289
        stringIdentity("String created by Kotlin") shouldBe "String created by Kotlin"
        byteToU32(255U) shouldBe 255U
    }

    @Test
    fun testSetFunctions() {
        val aSet = newSet()
        addToSet(aSet, "foo")
        addToSet(aSet, "bar")
        setContains(aSet, "foo") shouldBe true
        setContains(aSet, "bar") shouldBe true
        setContains(aSet, "baz") shouldBe false
    }
}
