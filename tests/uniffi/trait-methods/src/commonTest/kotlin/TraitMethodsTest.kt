/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.test.runTest
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

    @Test
    fun testOrd() {
        TraitMethods("a") shouldBeLessThan TraitMethods("b")
        TraitMethods("b") shouldBeGreaterThan TraitMethods("a")
        TraitMethods("a").compareTo(TraitMethods("a")) shouldBe 0
    }

    @Test
    fun testProcMacroOrd() {
        ProcTraitMethods("a") shouldBeLessThan ProcTraitMethods("b")
        ProcTraitMethods("b") shouldBeGreaterThan ProcTraitMethods("a")
        ProcTraitMethods("a").compareTo(ProcTraitMethods("a")) shouldBe 0
    }

    // Methods and uniffi trait exports on a record (uniffi 0.31). The receiver crosses
    // the FFI as a serialized value rather than a handle, so these exercise a different
    // call shape than the object cases above.

    @Test
    fun testRecordMethods() {
        TraitMethodsRecord("yo").shout() shouldBe "YO"
        TraitMethodsRecord("yo").repeated(3u) shouldBe "yoyoyo"
    }

    @Test
    fun testRecordAsyncMethod() = runTest {
        TraitMethodsRecord("yo").shoutLater() shouldBe "YO"
    }

    @Test
    fun testRecordDisplay() {
        TraitMethodsRecord("yo").toString() shouldBe "TraitMethodsRecord(yo)"
    }

    @Test
    fun testRecordEq() {
        TraitMethodsRecord("yo") shouldBe TraitMethodsRecord("yo")
        TraitMethodsRecord("yo") shouldNotBe TraitMethodsRecord("yoyo")
        TraitMethodsRecord("yo").equals("yo") shouldBe false
        (TraitMethodsRecord("yo") == TraitMethodsRecord("yo")) shouldBe true
    }

    @Test
    fun testRecordHash() {
        val m = TraitMethodsRecord("yo")
        val map = mapOf(m to 1, TraitMethodsRecord("yoyo") to 2)
        map[TraitMethodsRecord("yo")] shouldBe 1
        map[TraitMethodsRecord("yoyo")] shouldBe 2
    }

    @Test
    fun testRecordOrd() {
        TraitMethodsRecord("a") shouldBeLessThan TraitMethodsRecord("b")
        TraitMethodsRecord("b") shouldBeGreaterThan TraitMethodsRecord("a")
        listOf(TraitMethodsRecord("b"), TraitMethodsRecord("a")).sorted() shouldBe
            listOf(TraitMethodsRecord("a"), TraitMethodsRecord("b"))
    }

    // The same, for an enum with associated data - a Kotlin `sealed class`. Each
    // data-class variant generates its own `equals`/`hashCode`/`toString`, so the trait
    // impls have to be repeated per variant to actually reach Rust.

    @Test
    fun testEnumMethods() {
        TraitMethodsEnum.Plain.describe() shouldBe "plain"
        TraitMethodsEnum.WithData("yo").describe() shouldBe "data:yo"
    }

    @Test
    fun testEnumDisplay() {
        TraitMethodsEnum.Plain.toString() shouldBe "TraitMethodsEnum(plain)"
        TraitMethodsEnum.WithData("yo").toString() shouldBe "TraitMethodsEnum(data:yo)"
    }

    @Test
    fun testEnumEq() {
        TraitMethodsEnum.WithData("yo") shouldBe TraitMethodsEnum.WithData("yo")
        TraitMethodsEnum.WithData("yo") shouldNotBe TraitMethodsEnum.WithData("yoyo")
        TraitMethodsEnum.WithData("yo") shouldNotBe TraitMethodsEnum.Plain
        TraitMethodsEnum.Plain shouldBe TraitMethodsEnum.Plain
    }

    @Test
    fun testEnumHash() {
        val map = mapOf(
            TraitMethodsEnum.Plain to 1,
            TraitMethodsEnum.WithData("yo") to 2,
        )
        map[TraitMethodsEnum.Plain] shouldBe 1
        map[TraitMethodsEnum.WithData("yo")] shouldBe 2
    }

    @Test
    fun testEnumOrd() {
        // Rust's derived `Ord` orders by variant declaration order first.
        TraitMethodsEnum.Plain shouldBeLessThan TraitMethodsEnum.WithData("yo")
        TraitMethodsEnum.WithData("a") shouldBeLessThan TraitMethodsEnum.WithData("b")
        listOf(TraitMethodsEnum.WithData("a"), TraitMethodsEnum.Plain).sorted() shouldBe
            listOf(TraitMethodsEnum.Plain, TraitMethodsEnum.WithData("a"))
    }

    // A fieldless enum becomes a Kotlin `enum class`, where `equals`, `hashCode` and
    // `compareTo` are final on `kotlin.Enum` - only `Display`/`Debug` is rendered, and
    // the other three keep Kotlin's own behaviour.

    @Test
    fun testFlatEnumMethods() {
        TraitMethodsFlatEnum.ONE.doubled() shouldBe 2u
        TraitMethodsFlatEnum.TWO.doubled() shouldBe 4u
    }

    @Test
    fun testFlatEnumDisplay() {
        TraitMethodsFlatEnum.ONE.toString() shouldBe "TraitMethodsFlatEnum(2)"
        TraitMethodsFlatEnum.TWO.toString() shouldBe "TraitMethodsFlatEnum(4)"
    }
}
