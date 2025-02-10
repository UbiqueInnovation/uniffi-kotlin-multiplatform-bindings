/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import io.kotest.assertions.throwables.*
import io.kotest.matchers.*
import io.kotest.matchers.collections.beEmpty
import io.kotest.matchers.types.shouldBeTypeOf
import proc_macro.*
import kotlin.test.*

class ProcMacroTest {

    @Test
    fun testMakeOne() {
        val one = makeOne(123)
        one.inner shouldBe 123
        oneInnerByRef(one) shouldBe 123
    }

    @Test
    fun testTwo() {
        val two = Two("a")
        takeTwo(two) shouldBe "a"
    }

    @Test
    fun testRecordWithBytes() {
        val rwb = RecordWithBytes(byteArrayOf(1, 2, 3))
        takeRecordWithBytes(rwb) shouldBe byteArrayOf(1, 2, 3)
    }

    @Test
    @Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
    fun testObject() {
        var obj = Object()
        obj = Object.namedCtor(1u)
        obj.isHeavy() shouldBe MaybeBool.UNCERTAIN

        val obj2 = Object()
        obj.isOtherHeavy(obj2) shouldBe MaybeBool.UNCERTAIN
    }

    @Test
    fun testEnumIdentity() {
        enumIdentity(MaybeBool.TRUE) shouldBe MaybeBool.TRUE
    }

    @Test
    fun testThree() {
        // just make sure this works / doesn't crash
        Three(Object())
    }

    @Test
    fun testMakeZero() {
        makeZero().inner shouldBe "ZERO"
    }

    @Test
    fun testMakeRecordWithBytes() {
        makeRecordWithBytes().someBytes shouldBe byteArrayOf(0, 1, 2, 3, 4)
    }

    @Test
    fun testJoin() {
        join(listOf("a", "b", "c"), ":") shouldBe "a:b:c"
    }

    @Test
    fun testAlwaysFails() {
        shouldThrow<BasicException> {
            alwaysFails()
        }
    }

    @Test
    fun testDoStuffWith5() {
        val obj = Object.namedCtor(1u)
        obj.doStuff(5u)
    }

    @Test
    fun testDoStuffWith0() {
        shouldThrow<FlatException.InvalidInput> {
            val obj = Object.namedCtor(1u)
            obj.doStuff(0u)
        }
    }

    @Test
    fun testRecordWithDefaults() {
        val recordWithDefaults = RecordWithDefaults("Test")
        recordWithDefaults.noDefaultString shouldBe "Test"
        recordWithDefaults.boolean shouldBe true
        recordWithDefaults.integer shouldBe 42
        recordWithDefaults.floatVar shouldBe 4.2
        recordWithDefaults.vec shouldBe beEmpty<Boolean>()
        recordWithDefaults.optVec shouldBe null
        recordWithDefaults.optInteger shouldBe 42

        doubleWithDefault() shouldBe 42
    }
    
    @Test
    fun testObjectWithDefaults() {
        val objWithDefaults = ObjectWithDefaults()
        objWithDefaults.addToNum() shouldBe 42
    }

    @Test
    fun testTraitImpl() {
        val obj = Object.namedCtor(1u)
        val traitImpl = obj.getTrait(null)
        traitImpl.concatStrings("foo", "bar") shouldBe "foobar"
        obj.getTrait(traitImpl).concatStrings("foo", "bar") shouldBe "foobar"
        concatStringsByRef(traitImpl, "foo", "bar") shouldBe "foobar"

        val traitImpl2 = obj.getTraitWithForeign(null)
        traitImpl2.name() shouldBe "RustTraitImpl"
        obj.getTraitWithForeign(traitImpl2).name() shouldBe "RustTraitImpl"
    }

    class KtTestCallbackInterface : TestCallbackInterface {
        override fun doNothing() {}

        override fun add(a: UInt, b: UInt) = a + b

        override fun optional(a: UInt?) = a ?: 0u

        override fun withBytes(rwb: RecordWithBytes) = rwb.someBytes

        override fun tryParseInt(value: String): UInt {
            if (value == "force-unexpected-error") {
                // raise an error that's not expected
                throw RuntimeException(value)
            }
            try {
                return value.toUInt()
            } catch (e: NumberFormatException) {
                throw BasicException.InvalidInput()
            }
        }

        override fun callbackHandler(h: Object): UInt {
            val v = h.takeError(BasicException.InvalidInput())
            return v
        }

        override fun getOtherCallbackInterface() = KtTestCallbackInterface2()
    }

    class KtTestCallbackInterface2 : OtherCallbackInterface {
        override fun multiply(a: UInt, b: UInt) = a * b
    }

    @Test
    fun testCallbackInterface() {
        callCallbackInterface(KtTestCallbackInterface())
    }

    @Test
    fun testMixedEnum() {
        getMixedEnum(null) shouldBe MixedEnum.Int(1)
        getMixedEnum(MixedEnum.None) shouldBe MixedEnum.None
        getMixedEnum(MixedEnum.String("hello")) shouldBe MixedEnum.String("hello")

        val e = getMixedEnum(null)
        e.shouldBeTypeOf<MixedEnum.Int>()
        run {
            // you can destruct the enum into its bits.
            val (i) = e
            i shouldBe 1L
        }
        val eb = MixedEnum.Both("hi", 2)
        val (s, i) = eb
        s shouldBe "hi"
        i shouldBe 2L
    }
}
