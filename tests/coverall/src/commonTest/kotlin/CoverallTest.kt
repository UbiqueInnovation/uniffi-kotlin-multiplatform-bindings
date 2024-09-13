/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import coverall.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.math.abs
import kotlin.test.BeforeTest
import kotlin.test.Test

class CoverallTest {
    private val GC_DELAY: Long = 10L

    private fun runGCWithDelay() {
        runGC()
        runBlocking { delay(GC_DELAY) }
    }

    @BeforeTest
    fun gcBetweenTests() {
        runGCWithDelay()
    }

    @Test
    fun dict() {
        createSomeDict().use { d ->
            d.text shouldBe "text"
            d.maybeText shouldBe "maybe_text"
            d.aBool shouldBe true
            d.maybeABool shouldBe false
            d.unsigned8 shouldBe 1.toUByte()
            d.maybeUnsigned8 shouldBe 2.toUByte()
            d.unsigned16 shouldBe 3.toUShort()
            d.maybeUnsigned16 shouldBe 4.toUShort()
            d.unsigned64 shouldBe 18446744073709551615UL
            d.maybeUnsigned64 shouldBe 0UL
            d.signed8 shouldBe 8.toByte()
            d.maybeSigned8 shouldBe 0.toByte()
            d.signed64 shouldBe 9223372036854775807L
            d.maybeSigned64 shouldBe 0L

            // floats should be "close enough".
            infix fun Float.almostEquals(other: Float) = (abs(this - other) < 0.000001) shouldBe true
            infix fun Double.almostEquals(other: Double) = (abs(this - other) < 0.000001) shouldBe true

            d.float32 almostEquals 1.2345F
            d.maybeFloat32!! almostEquals (22.0F / 7.0F)
            d.float64 almostEquals 0.0
            d.maybeFloat64!! almostEquals 1.0

            d.byteArray shouldBe byteArrayOf(5, 4, 3, 2, 1)
            d.maybeByteArray shouldBe byteArrayOf(10, 8, 6, 4, 2)

            d.coveralls!!.getName() shouldBe "some_dict"
        }
    }

    @Test
    fun arcs() {
        Coveralls("test_arcs").use { coveralls ->
            getNumAlive() shouldBe 1UL
            // One ref held by the foreign-language code, one created for this method call.
            coveralls.strongCount() shouldBe 2UL
            coveralls.getOther() shouldBe null
            coveralls.takeOther(coveralls)
            // Should now be a new strong ref, held by the object's reference to itself.
            coveralls.strongCount() shouldBe 3UL
            // But the same number of instances.
            getNumAlive() shouldBe 1UL
            // Careful, this makes a new Kotlin object which must be separately destroyed.
            coveralls.getOther()!!.use { other ->
                // It's the same Rust object.
                other.getName() shouldBe "test_arcs"
            }
            shouldThrow<CoverallException.TooManyHoles> {
                coveralls.takeOtherFallible()
            }
            shouldThrow<InternalException> {
                coveralls.takeOtherPanic("expected panic: with an arc!")
            }
            shouldThrow<InternalException> {
                coveralls.falliblePanic("Expected panic in a fallible function!")
            }
            coveralls.takeOther(null)

            runGCWithDelay()

            coveralls.strongCount() shouldBe 2UL
        }

        runGCWithDelay()

        getNumAlive() shouldBe 0UL
    }

    @Test
    fun returnObjects() {
        Coveralls("test_return_objects").use { coveralls ->
            getNumAlive() shouldBe 1UL
            coveralls.strongCount() shouldBe 2UL
            coveralls.cloneMe().use { c2 ->
                c2.getName() shouldBe coveralls.getName()
                getNumAlive() shouldBe 2UL
                c2.strongCount() shouldBe 2UL

                coveralls.takeOther(c2)
                // same number alive but `c2` has an additional ref count.
                getNumAlive() shouldBe 2UL
                coveralls.strongCount() shouldBe 2UL
                c2.strongCount() shouldBe 3UL
            }
            // Here we've dropped Kotlin's reference to `c2`, but the rust struct will not
            // be dropped as coveralls hold an `Arc<>` to it.
            getNumAlive() shouldBe 2UL
        }

        runGCWithDelay()

        // Destroying `coveralls` will kill both.
        getNumAlive() shouldBe 0UL

        Coveralls("test_simple_errors").use { coveralls ->
            shouldThrow<CoverallException.TooManyHoles> {
                coveralls.maybeThrow(true)
            }.also { e ->
                e.message shouldBe "The coverall has too many holes"
            }

            shouldThrow<CoverallException.TooManyHoles> {
                coveralls.maybeThrowInto(true)
            }

            shouldThrow<InternalException> {
                coveralls.panic("oops")
            }.also { e ->
                e.message shouldBe "oops"
            }
        }

        Coveralls("test_complex_errors").use { coveralls ->
            coveralls.maybeThrowComplex(0) shouldBe true

            shouldThrow<ComplexException.OsException> {
                coveralls.maybeThrowComplex(1)
            }.also { e ->
                e.code shouldBe 10.toShort()
                e.extendedCode shouldBe 20.toShort()
                e.toString() shouldBeIn setOf(
                    "coverall.ComplexException.OsException: code=10, extendedCode=20",
                    "coverall.ComplexException\$OsException: code=10, extendedCode=20"
                )
            }

            shouldThrow<ComplexException.PermissionDenied> {
                coveralls.maybeThrowComplex(2)
            }.also { e ->
                e.reason shouldBe "Forbidden"
                e.toString() shouldBeIn setOf(
                    "coverall.ComplexException.PermissionDenied: reason=Forbidden",
                    "coverall.ComplexException\$PermissionDenied: reason=Forbidden"
                )
            }

            shouldThrow<InternalException> {
                coveralls.maybeThrowComplex(3)
            }
        }

        Coveralls("test_interfaces_in_dicts").use { coveralls ->
            coveralls.addPatch(Patch(Color.RED))
            coveralls.addRepair(
                Repair(`when` = Clock.System.now(), patch = Patch(Color.BLUE))
            )
            coveralls.getRepairs().size shouldBe 2
        }

        Coveralls("test_regressions").use { coveralls ->
            coveralls.getStatus("success") shouldBe "status: success"
        }
    }

    // This tests that the UniFFI-generated scaffolding doesn't introduce any unexpected locking.
    // We have one thread busy-wait for a some period of time, while a second thread repeatedly
    // increments the counter and then checks if the object is still busy. The second thread should
    // not be blocked on the first, and should reliably observe the first thread being busy.
    // If it does not, that suggests UniFFI is accidentally serializing the two threads on access
    // to the shared counter object.
    @Test
    fun threadSafe() = runTest {
        val scope = CoroutineScope(Dispatchers.Default)
        try {
            ThreadsafeCounter().use { counter ->
                val busyWaiting = scope.launch {
                    // 300 ms should be long enough for the other thread to easily finish
                    // its loop, but not so long as to annoy the user with a slow test.
                    counter.busyWait(300)
                }
                val incrementing = scope.async {
                    var count = 0
                    for (n in 1..100) {
                        // We expect most iterations of this loop to run concurrently
                        // with the busy-waiting thread.
                        count = counter.incrementIfBusy()
                    }
                    count
                }

                busyWaiting.join()
                val count = incrementing.await()
                count shouldBeGreaterThan 0
            }
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun noRustCall() {
        // This does not call Rust code.
        var d = DictWithDefaults()
        d.name shouldBe "default-value"
        d.category shouldBe null
        d.integer shouldBe 31UL

        d = DictWithDefaults(name = "this", category = "that", integer = 42UL)
        d.name shouldBe "this"
        d.category shouldBe "that"
        d.integer shouldBe 42UL
    }
}