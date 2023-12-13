/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import io.kotest.assertions.throwables.*
import io.kotest.matchers.*
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
    fun testObject() {
        val obj = Object.namedCtor(1u)
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
    fun testAlwaysFails() {
        shouldThrow<BasicException> {
            alwaysFails()
        }
    }

    @Test
    fun testDoStuffWith5() {
        val obj = Object()
        obj.doStuff(5u)
    }

    @Test
    fun testDoStuffWith0() {
        shouldThrow<FlatException.InvalidInput> {
            val obj = Object()
            obj.doStuff(0u)
        }
    }

    @Test
    fun testTraitImpl() {
        val obj = Object()
        val traitImpl = obj.getTrait(null)
        obj.getTrait(traitImpl).name() shouldBe "TraitImpl"
        getTraitNameByRef(traitImpl) shouldBe "TraitImpl"
    }

    @Test
    fun testCallbackInterface() {
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
        }

        callCallbackInterface(KtTestCallbackInterface())
    }
}
