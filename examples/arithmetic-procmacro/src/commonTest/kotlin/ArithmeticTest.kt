/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import io.gitlab.trixnity.uniffi.examples.arithmeticpm.*
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.serialization.json.*

class ArithmeticTest {
    @Test
    fun testIntegerOverflow() {
        shouldThrowExactly<ArithmeticException.IntegerOverflow> {
            add(18446744073709551615uL, 1uL)
        }
    }

    @Test
    fun testBasicOperations() {
        val test = "{\"ty\": \"ADD\", \"a\": 2, \"b\": 3}"
        val operation = Json {}.decodeFromString<Operation>(test)
        applyOperation(operation) shouldBe 5uL
        add(2uL, 4uL) shouldBe 6uL
        sub(4uL, 2uL) shouldBe 2uL
        div(8uL, 4uL) shouldBe 2uL
        equal(2uL, 2uL) shouldBe true
        equal(4uL, 8uL) shouldBe false
    }
}