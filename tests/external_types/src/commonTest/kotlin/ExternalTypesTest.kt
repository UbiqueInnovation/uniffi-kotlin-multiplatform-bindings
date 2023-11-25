/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import external_types.CombinedType
import external_types.CrateOneType
import external_types.CrateTwoType
import external_types.getCombinedType
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ExternalTypesTest {
    @Test
    fun test() {
        val ct = getCombinedType(
            CombinedType(
                CrateOneType("test"),
                CrateTwoType(42),
            )
        );
        ct.cot.sval shouldBe "test"
        ct.ctt.ival shouldBe 42

        val ct2 = getCombinedType(null)
        ct2.cot.sval shouldBe "hello"
        ct2.ctt.ival shouldBe 1
    }
}