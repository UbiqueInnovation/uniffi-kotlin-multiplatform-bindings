/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import io.kotest.matchers.shouldBe
import kotlin.test.Test
import sub_lib.callUniffiOneTrait
import sub_lib.callUniffiOneUdlTrait
import uniffi_one.UniffiOneTrait
import uniffi_one.UniffiOneUdlTrait

// Same calls as sub-lib's ExternalCrateInitTest, but here sub-lib comes from sub-lib-jvm.jar.
class SubLibFromJarTest {
    object ProcMacroImpl : UniffiOneTrait {
        override fun hello() = "kotlin proc-macro impl says hello"
    }

    object UdlImpl : UniffiOneUdlTrait {
        override fun hello() = "kotlin UDL impl says hello"
    }

    @Test
    fun callsExternalProcMacroTrait() {
        callUniffiOneTrait(ProcMacroImpl) shouldBe "kotlin proc-macro impl says hello"
    }

    @Test
    fun callsExternalUdlTrait() {
        callUniffiOneUdlTrait(UdlImpl) shouldBe "kotlin UDL impl says hello"
    }
}
