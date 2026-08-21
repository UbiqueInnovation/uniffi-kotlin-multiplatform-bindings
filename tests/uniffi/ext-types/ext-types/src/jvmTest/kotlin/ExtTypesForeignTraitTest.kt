/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import ext_types.callUniffiOneTraitFromExtTypes
import ext_types.callUniffiOneUdlTraitFromExtTypes
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import uniffi_one.UniffiOneTrait
import uniffi_one.UniffiOneUdlTrait

/**
 * A Kotlin implementation of a trait declared by `uniffi_one`, passed into a function of
 * *this* module's library.
 *
 * `uniffi_one`'s Kotlin package is generated once, by the `sub-lib` module, and binds to
 * that module's library; this module links its own copy of `uniffi_one`'s Rust, so it has
 * a separate `UniffiForeignPointerCell` per trait. Keeping every copy filled is
 * `uniffi.runtime.UniffiVtableRegistry`'s job - without it Rust dispatches through a null
 * vtable and aborts the process (SIGABRT) instead of throwing.
 *
 * JVM-only on purpose. The registry is JNA-based, and Kotlin/Native has an analogous but
 * separate problem in this shape: both static archives carry a private copy of the cell
 * while only one copy of the `..._fn_init_callback_vtable_...` symbol survives linking, so
 * whether the write lands in the cell the read uses is decided by link order. The
 * `multi-module` fixture happens to link the other way round and passes on native.
 */
class ExtTypesForeignTraitTest {
    object ProcMacroImpl : UniffiOneTrait {
        override fun hello() = "kotlin proc-macro impl says hello"
    }

    object UdlImpl : UniffiOneUdlTrait {
        override fun hello() = "kotlin UDL impl says hello"
    }

    @Test
    fun callsForeignTraitDeclaredInAnotherModule() {
        callUniffiOneTraitFromExtTypes(ProcMacroImpl) shouldBe "kotlin proc-macro impl says hello"

        // Lowering mints a fresh handle each time, so nothing here is consumed.
        callUniffiOneTraitFromExtTypes(ProcMacroImpl) shouldBe "kotlin proc-macro impl says hello"
    }

    /**
     * As above for the UDL-declared trait. Our own view of it is
     * `typedef trait UniffiOneUDLTrait`, ie `Trait(RustOnly)` - only `uniffi_one`'s own
     * interface knows it has a foreign vtable at all, which is why the registry reads the
     * declaring crate's `ComponentInterface` rather than the consumer's.
     */
    @Test
    fun callsForeignUdlTraitDeclaredInAnotherModule() {
        callUniffiOneUdlTraitFromExtTypes(UdlImpl) shouldBe "kotlin UDL impl says hello"
        callUniffiOneUdlTraitFromExtTypes(UdlImpl) shouldBe "kotlin UDL impl says hello"
    }
}
