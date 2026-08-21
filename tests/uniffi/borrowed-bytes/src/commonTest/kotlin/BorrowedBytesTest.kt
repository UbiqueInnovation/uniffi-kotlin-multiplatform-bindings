/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import borrowed_bytes.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * `&[u8]` / `[ByRef] bytes` arguments cross the FFI as a borrowed `ForeignBytes` rather
 * than an owned `RustBuffer`, so the generated Kotlin holds the caller's array still for
 * the duration of the call - pinned on Kotlin/Native, copied into native memory on JNA.
 *
 * The assertions are all about the bytes Rust actually read: a pointer that is stale,
 * unpinned, or short by one shows up as a wrong sum rather than as a crash.
 */
class BorrowedBytesTest {
    /** Rust sums as `u8`; Kotlin's `Byte` is signed, so widen through `UByte`. */
    private fun ByteArray.unsignedSum(): ULong =
        fold(0uL) { acc, byte -> acc + byte.toUByte().toULong() }

    @Test
    fun testSumBytes() {
        val data = byteArrayOf(1, 2, 3, 4, 5)
        sumBytes(data) shouldBe 15uL
    }

    @Test
    fun testSumBytesReadsHighBytes() {
        // Every byte Kotlin considers negative is a value above 127 to Rust. Getting this
        // wrong would mean the length or the pointer is right but the memory is not.
        val data = byteArrayOf(-1, -128, 127, 0)
        sumBytes(data) shouldBe 255uL + 128uL + 127uL
    }

    @Test
    fun testSumEmptyBytes() {
        // An empty array has no address to pin and no memory to copy - it goes over as
        // `(null, 0)`, which Rust lifts as an empty slice.
        sumBytes(byteArrayOf()) shouldBe 0uL
    }

    @Test
    fun testSumLargeBytes() {
        val data = ByteArray(1 shl 20) { (it % 251).toByte() }
        sumBytes(data) shouldBe data.unsignedSum()
    }

    @Test
    fun testEchoBytes() {
        val data = ByteArray(1000) { (it % 256).toByte() }
        echoBytes(data) shouldBe data
    }

    @Test
    fun testEchoEmptyBytes() {
        echoBytes(byteArrayOf()) shouldBe byteArrayOf()
    }

    @Test
    fun testConcatBytes() {
        // Two borrowed arguments in one call: the generated code nests one borrow scope
        // inside the other, and both have to still be valid when the call happens.
        concatBytes(byteArrayOf(1, 2), byteArrayOf(3, 4, 5)) shouldBe byteArrayOf(1, 2, 3, 4, 5)
    }

    @Test
    fun testConcatBytesWithEmpty() {
        concatBytes(byteArrayOf(), byteArrayOf(3, 4)) shouldBe byteArrayOf(3, 4)
        concatBytes(byteArrayOf(1, 2), byteArrayOf()) shouldBe byteArrayOf(1, 2)
        concatBytes(byteArrayOf(), byteArrayOf()) shouldBe byteArrayOf()
    }

    @Test
    fun testWrapBytes() {
        // A borrowed slice between two ordinary owned arguments: the borrow scope wraps
        // the call, the other two still lower normally inside it.
        wrapBytes(9u, byteArrayOf(1, 2), "!") shouldBe byteArrayOf(9, 1, 2, '!'.code.toByte())
    }

    @Test
    fun testThrowingCallInsideBorrowScope() {
        sumNonEmptyBytes(byteArrayOf(1, 2, 3)) shouldBe 6uL
        shouldThrow<ByteException.Empty> {
            sumNonEmptyBytes(byteArrayOf())
        }
    }

    @Test
    fun testBorrowIsNotRetained() {
        // Rust must read the bytes during the call and keep nothing pointing at them. If
        // the borrow outlived the call, the sink's copy of the first array would change
        // underneath it when Kotlin mutates the array afterwards.
        val data = byteArrayOf(1, 2, 3)
        val sink = ByteSink(data)
        data[0] = 100
        sink.absorb(data) shouldBe 6uL
        sink.contents() shouldBe byteArrayOf(1, 2, 3, 100, 2, 3)
        sink.destroy()
    }

    @Test
    fun testConstructorAndMethodBorrows() {
        val sink = ByteSink(byteArrayOf(1, 2))
        sink.absorb(byteArrayOf(3)) shouldBe 3uL
        sink.absorb(byteArrayOf()) shouldBe 3uL
        sink.absorb(byteArrayOf(4, 5)) shouldBe 5uL
        sink.contents() shouldBe byteArrayOf(1, 2, 3, 4, 5)
        sink.destroy()
    }

    // The proc-macro path reaches `&[u8]` by a different route than UDL's `[ByRef]`, so it
    // gets the same coverage.

    @Test
    fun testProcSumBytes() {
        procSumBytes(byteArrayOf(1, 2, 3)) shouldBe 6uL
        procSumBytes(byteArrayOf()) shouldBe 0uL
    }

    @Test
    fun testProcConcatBytes() {
        procConcatBytes(byteArrayOf(1), byteArrayOf(2, 3)) shouldBe byteArrayOf(1, 2, 3)
    }

    @Test
    fun testProcKeywordNamedArgument() {
        // The parameter is named `object`, a Kotlin hard keyword, so the generated
        // parameter is backticked. The name the borrow binds to must not be.
        procSumKeywordNamedBytes(byteArrayOf(10, 20)) shouldBe 30uL
    }

    @Test
    fun testProcObjectBorrows() {
        val sink = ProcByteSink(byteArrayOf(1, 2))
        sink.absorb(byteArrayOf(3, 4)) shouldBe 4uL
        sink.contents() shouldBe byteArrayOf(1, 2, 3, 4)
        sink.destroy()
    }
}
