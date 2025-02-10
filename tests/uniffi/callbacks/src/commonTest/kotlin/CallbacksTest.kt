/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import callbacks.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class CallbacksTest {

    class KotlinGetters : ForeignGetters {
        override fun getBool(v: Boolean, argumentTwo: Boolean): Boolean = v xor argumentTwo
        override fun getString(v: String, arg2: Boolean): String {
            if (v == "bad-argument") {
                throw SimpleException.BadArgument("bad argument")
            }
            if (v == "unexpected-error") {
                throw RuntimeException("something failed")
            }
            return if (arg2) "1234567890123" else v
        }

        override fun getOption(v: String?, arg2: Boolean): String? {
            if (v == "bad-argument") {
                throw ComplexException.ReallyBadArgument(20)
            }
            if (v == "unexpected-error") {
                throw RuntimeException("something failed")
            }
            return if (arg2) v?.uppercase() else v
        }

        override fun getList(v: List<Int>, arg2: Boolean): List<Int> = if (arg2) v else listOf()
        override fun getNothing(v: String): Unit {
            if (v == "bad-argument") {
                throw SimpleException.BadArgument("bad argument")
            }
            if (v == "unexpected-error") {
                throw RuntimeException("something failed")
            }
        }
    }

    // A bit more systematic in testing, but this time in English.
    //
    // 1. Pass in the callback as arguments.
    // Make the callback methods use multiple arguments, with a variety of types, and
    // with a variety of return types.
    @Test
    fun callbackAsArgument() {
        val rustGetters = RustGetters()

        val callback = KotlinGetters()
        listOf(true, false).forEach { v ->
            val flag = true
            val expected = callback.getBool(v, flag)
            val observed = rustGetters.getBool(callback, v, flag)
            expected shouldBe observed
        }

        listOf(listOf(1, 2), listOf(0, 1)).forEach { v ->
            val flag = true
            val expected = callback.getList(v, flag)
            val observed = rustGetters.getList(callback, v, flag)
            expected shouldBe observed
        }

        listOf("Hello", "world").forEach { v ->
            val flag = true
            val expected = callback.getString(v, flag)
            val observed = rustGetters.getString(callback, v, flag)
            expected shouldBe observed
        }

        listOf("Some", null).forEach { v ->
            val flag = false
            val expected = callback.getOption(v, flag)
            val observed = rustGetters.getOption(callback, v, flag)
            expected shouldBe observed
        }

        rustGetters.getStringOptionalCallback(callback, "TestString", false) shouldBe "TestString"
        rustGetters.getStringOptionalCallback(null, "TestString", false) shouldBe null

        // Should not throw
        rustGetters.getNothing(callback, "TestString")

        shouldThrow<SimpleException.BadArgument> {
            rustGetters.getString(callback, "bad-argument", true)
        }
        shouldThrow<SimpleException.UnexpectedException> {
            rustGetters.getString(callback, "unexpected-error", true)
        }

        shouldThrow<ComplexException.ReallyBadArgument> {
            rustGetters.getOption(callback, "bad-argument", true)
        }.also {
            it.code shouldBe 20
        }

        shouldThrow<ComplexException.UnexpectedErrorWithReason> {
            rustGetters.getOption(callback, "unexpected-error", true)
        }.also {
            it.reason shouldBe RuntimeException("something failed").toString()
        }

        shouldThrow<SimpleException.BadArgument> {
            rustGetters.getNothing(callback, "bad-argument")
        }
        shouldThrow<SimpleException.UnexpectedException> {
            rustGetters.getNothing(callback, "unexpected-error")
        }

        rustGetters.destroy()
    }

    class StoredKotlinStringifier : StoredForeignStringifier {
        override fun fromSimpleType(value: Int): String = "kotlin: $value"

        // We don't test this, but we're checking that the arg type is included in the minimal list of types used
        // in the UDL.
        // If this doesn't compile, then look at TypeResolver.
        override fun fromComplexType(values: List<Double?>?): String = "kotlin: $values"
    }

    @Test
    fun callbackAsConstructorArgument() {
        val kotlinStringifier = StoredKotlinStringifier()
        val rustStringifier = RustStringifier(kotlinStringifier)
        listOf(1, 2).forEach { v ->
            val expected = kotlinStringifier.fromSimpleType(v)
            val observed = rustStringifier.fromSimpleType(v)
            expected shouldBe observed
        }
        rustStringifier.destroy()

        // `stringifier` must remain valid after `rustStringifier2` drops the reference
        val stringifier = StoredKotlinStringifier()
        val rustStringifier1 = RustStringifier(stringifier)
        val rustStringifier2 = RustStringifier(stringifier)
        rustStringifier2.fromSimpleType(123) shouldBe "kotlin: 123"
        rustStringifier2.destroy()
        rustStringifier1.fromSimpleType(123) shouldBe "kotlin: 123"
    }
}
