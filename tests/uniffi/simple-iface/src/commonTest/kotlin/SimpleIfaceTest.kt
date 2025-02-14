/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import io.kotest.matchers.*
import simple_iface.*
import kotlin.test.*

class SimpleIfaceTest {
    @Test
    fun testInterface() {
        var obj = makeObject(9000)
        obj.getInner() shouldBe 9000
        obj.someMethod()
    }
}
