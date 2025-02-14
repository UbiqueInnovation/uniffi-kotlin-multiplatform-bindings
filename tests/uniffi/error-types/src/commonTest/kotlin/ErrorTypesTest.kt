/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import error_types.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ErrorTypesTest {
    @Test
    fun sync() {
        shouldThrow<ErrorInterface> {
            oops()
        }.also { e ->
            e.toString() shouldBe "because uniffi told me so\n\nCaused by:\n    oops"
            e.chain().size shouldBe 2
            e.link(0U) shouldBe "because uniffi told me so"
        }
        shouldThrow<kotlin.Exception> {
            oops()
        }.also { e ->
            e.toString() shouldBe "because uniffi told me so\n\nCaused by:\n    oops"
        }

        shouldThrow<ErrorInterface> {
            oopsNowrap()
        }.also { e ->
            e.toString() shouldBe "because uniffi told me so\n\nCaused by:\n    oops"
            e.chain().size shouldBe 2
            e.link(0U) shouldBe "because uniffi told me so"
        }

        shouldThrow<ErrorTrait> {
            toops()
        }.also { e ->
            e.msg() shouldBe "trait-oops"
        }

        val error = getError("the error")
        error.toString() shouldBe "the error"
        error.link(0U) shouldBe "the error"

        shouldThrow<RichException> {
            throwRich("oh no")
        }.also { e ->
            e.toString() shouldBe "RichError: \"oh no\""
        }

        shouldThrow<Exception> {
            oopsEnum(0u)
        }.also { e ->
            e.toString() shouldBeIn setOf(
                "error_types.Exception.Oops: ",
                "error_types.Exception\$Oops: ",
            )
        }

        shouldThrow<Exception> {
            oopsEnum(1u)
        }.also { e ->
            e.toString() shouldBeIn setOf(
                "error_types.Exception.Value: value=value",
                "error_types.Exception\$Value: value=value",
            )
        }

        shouldThrow<Exception> {
            oopsEnum(2u)
        }.also { e ->
            e.toString() shouldBeIn setOf(
                "error_types.Exception.IntValue: value=2",
                "error_types.Exception\$IntValue: value=2",
            )
        }

        shouldThrow<Exception.FlatInnerException> {
            oopsEnum(3u)
        }.also { e ->
            e.toString() shouldBeIn setOf(
                "error_types.Exception.FlatInnerException: error=error_types.FlatInner.CaseA: inner",
                "error_types.Exception\$FlatInnerException: error=error_types.FlatInner\$CaseA: inner",
            )
        }

        shouldThrow<Exception.FlatInnerException> {
            oopsEnum(4u)
        }.also { e ->
            e.toString() shouldBeIn setOf(
                "error_types.Exception.FlatInnerException: error=error_types.FlatInner.CaseB: NonUniffiTypeValue: value",
                "error_types.Exception\$FlatInnerException: error=error_types.FlatInner\$CaseB: NonUniffiTypeValue: value",
            )
        }

        shouldThrow<Exception.InnerException> {
            oopsEnum(5u)
        }.also { e ->
            e.toString() shouldBeIn setOf(
                "error_types.Exception.InnerException: error=error_types.Inner.CaseA: v1=inner",
                "error_types.Exception\$InnerException: error=error_types.Inner\$CaseA: v1=inner",
            )
        }

        shouldThrow<TupleException> {
            oopsTuple(0u)
        }.also { e ->
            e.toString() shouldBeIn setOf(
                "error_types.TupleException.Oops: v1=oops",
                "error_types.TupleException\$Oops: v1=oops",
            )
        }

        shouldThrow<TupleException> {
            oopsTuple(1u)
        }.also { e ->
            e.toString() shouldBeIn setOf(
                "error_types.TupleException.Value: v1=1",
                "error_types.TupleException\$Value: v1=1",
            )
        }

    }

    @Test
    fun async() = runTest {
        shouldThrow<ErrorInterface> {
            aoops()
        }.also { e ->
            e.toString() shouldBe "async-oops"
        }
    }
}