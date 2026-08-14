/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import interface_throws.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.test.assertFailsWith

class InterfaceThrowsTest {
    @Test
    fun throwsThroughTheInterface() {
        val greeter: Greeter = makeGreeterTrait("Hello")

        greeter.greet("world") shouldBe "Hello, world!"
        assertFailsWith<GreeterException.NotInTheMood> { greeter.greet("") }
    }

    @Test
    fun throwsThroughTheObject() {
        val greeter = RustGreeter("Hello")

        greeter.greet("world") shouldBe "Hello, world!"
        assertFailsWith<GreeterException.NotInTheMood> { greeter.greet("") }
    }
}
