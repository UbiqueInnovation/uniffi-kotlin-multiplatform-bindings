import defaults.CustomType
import defaults.MyEnum
import defaults.MyStruct
import defaults.TextSplitter
import defaults.custom.MyCustomType
import defaults.doSomethingWithCustomType
import defaults.isOptionalCustomTypeNoneDefaultNone
import defaults.isOptionalCustomTypeNoneDefaultSome
import defaults.split
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */


class DefaultsTest {
    @Test
    fun `Default function arguments`() {
        val splits = split("apple,banana", maxSplits = 1U)
        splits shouldBe listOf("apple", "banana")
    }

    @Test
    fun `Object constructor arguments and method arguments`() {
        val splitter = TextSplitter()

        // default maxSplits is 0
        splitter.split("apple,banana") shouldBe listOf("apple,banana")

        splitter.split("apple,banana", maxSplits = 1U) shouldBe listOf("apple", "banana")
    }

    @Test
    fun `Enum and record default values`() {
        val myEnum = MyEnum.MyVariant()
        myEnum.d shouldBe 0u
        myEnum.e shouldBe 1u

        val myStruct = MyStruct()
        myStruct.a shouldBe 0u
        myStruct.b shouldBe 1u
    }

    @Test
    fun `Custom type conversion should be applied to defaults`() {
        val value: CustomType = MyCustomType(123)
        doSomethingWithCustomType(value) shouldBe MyCustomType(124)
        doSomethingWithCustomType() shouldBe MyCustomType(43)

        isOptionalCustomTypeNoneDefaultNone() shouldBe true
        isOptionalCustomTypeNoneDefaultSome() shouldBe false
    }
}