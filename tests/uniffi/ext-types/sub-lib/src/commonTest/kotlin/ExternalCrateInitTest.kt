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

/**
 * Upstream #2343: a crate whose types we use registers its callback interface vtables
 * from its own lazy `UniffiLib`, so `sub_lib`'s initialiser has to chain into
 * `uniffi_one.uniffiEnsureInitialized()`. Without that, Rust calls through a vtable
 * that was never set, which aborts the process instead of throwing.
 *
 * Nothing here may touch the `uniffi_one` namespace before the call under test - doing
 * so would register the vtables as a side effect and mask a regression.
 */
class ExternalCrateInitTest {
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
