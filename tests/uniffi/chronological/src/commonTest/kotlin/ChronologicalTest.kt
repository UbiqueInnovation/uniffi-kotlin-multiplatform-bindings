/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import chronological.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

class ChronologicalTest {
    // Test passing timestamp and duration while returning timestamp
    @Test
    fun testAdd() {
        add(
            Instant.fromEpochSeconds(100, 100),
            1.seconds + 1.nanoseconds,
        ) shouldBe Instant.fromEpochSeconds(101, 101)
    }

    // Test passing timestamp while returning duration
    @Test
    fun testDiff() {
        diff(
            Instant.fromEpochSeconds(101, 101),
            Instant.fromEpochSeconds(100, 100),
        ) shouldBe (1.seconds + 1.nanoseconds)
    }

    @Test
    fun testPreEpochTimestamps() {
        // Test pre-epoch timestamps
        add(
            Instant.parse("1955-11-05T00:06:00.283000001Z"),
            1.seconds + 1.nanoseconds,
        ) shouldBe Instant.parse("1955-11-05T00:06:01.283000002Z")
    }

    // Test exceptions are propagated
    @Test
    fun testChronologicalException() {
        shouldThrow<ChronologicalException> {
            diff(Instant.fromEpochSeconds(100), Instant.fromEpochSeconds(101))
        }
    }

    // Test max Instant upper bound
    @Test
    fun testInstantUpperBound() {
        add(Instant.MAX, Duration.ZERO) shouldBe Instant.MAX

        // Test Instant is clamped to the upper bound, and don't check for the exception as in upstream.
        // While Java's Instant.plus throws DateTimeException for overflow, kotlinx-datetime Instant just coerces the
        // value to the upper bound.
        add(Instant.MAX, 1.seconds) shouldBe Instant.MAX

        // Upstream checks for the exception
        // try {
        //     add(Instant.MAX, 1.seconds)
        //     throw RuntimeException("Should have thrown a DateTimeException exception!")
        // } catch (e: DateTimeException) {
        //     // It's okay!
        // }
    }

    // Test that rust timestamps behave like kotlin timestamps
    @Test
    fun test() {
        // The underlying implementation of `Clock.System` may be lower resolution than the Rust clock.
        // Sleep for 10ms between each call, which should ensure `Clock.System` ticks forward.
        runBlocking {
            val kotlinBefore = Clock.System.now()
            delay(10)
            val rustNow = now()
            delay(10)
            val kotlinAfter = Clock.System.now()
            kotlinBefore shouldBeLessThan rustNow
            kotlinAfter shouldBeGreaterThan rustNow
        }
    }

    // Test optional values work
    @Test
    fun testOptionalValues() {
        optional(Instant.MAX, Duration.ZERO) shouldBe true
        optional(null, Duration.ZERO) shouldBe false
        optional(Instant.MAX, null) shouldBe false
    }
}


// This is to mock java.time.Instant.MAX, which does not exist as a public API in kotlinx-datetime.
// Since `Instant.fromEpochSeconds` clamps the given value to the platform-specific boundaries, passing `Long.MAX_VALUE`
// is okay to get the maximum value.
private val Instant.Companion.MAX: Instant
    get() = fromEpochSeconds(Long.MAX_VALUE, 999_999_999)
