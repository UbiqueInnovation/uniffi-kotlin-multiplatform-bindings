/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import trait_methods.*
import kotlin.test.Test

class TraitMethodsTest {
    @Test
    fun testDisplay() {
        val m = TraitMethods("yo")
        m.toString() shouldBe "TraitMethods(yo)"
    }

    @Test
    fun testEq() {
        val m = TraitMethods("yo")
        m shouldBe TraitMethods("yo")
        m shouldNotBe TraitMethods("yoyo")
    }

    @Test
    fun testHash() {
        val m = TraitMethods("yo")
        val map = mapOf(m to 1, TraitMethods("yoyo") to 2)
        map[m] shouldBe 1
        map[TraitMethods("yoyo")] shouldBe 2
    }

    @Test
    fun testProcMacroDisplay() {
        val m = ProcTraitMethods("yo")
        m.toString() shouldBe "ProcTraitMethods(yo)"
    }

    @Test
    fun testProcMacroEq() {
        val m = ProcTraitMethods("yo")
        m shouldBe ProcTraitMethods("yo")
        m shouldNotBe ProcTraitMethods("yoyo")
    }

    @Test
    fun testProcMacroHash() {
        val m = ProcTraitMethods("yo")
        val map = mapOf(m to 1, ProcTraitMethods("yoyo") to 2)
        map[m] shouldBe 1
        map[ProcTraitMethods("yoyo")] shouldBe 2
    }
}
