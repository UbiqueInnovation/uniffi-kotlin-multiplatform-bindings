import futures.*
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.*
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

class FuturesTest {

    @Test
    fun testAlwaysReady() = assertReturnsImmediately {
        alwaysReady() shouldBe true
    }

    @Test
    fun testVoid() = assertReturnsImmediately {
        void() shouldBe Unit
    }

    @Test
    fun testSleep() = assertApproximateTime(200) {
        sleep(200u) shouldBe true
    }

    @Test
    fun testSequentialFutures() = assertApproximateTime(300) {
        sayAfter(100u, "Alice") shouldBe "Hello, Alice!"
        sayAfter(200u, "Bob") shouldBe "Hello, Bob!"
    }

    @Test
    fun testConcurrentFutures() = assertApproximateTime(200) {
        val resultAlice = async { sayAfter(100u, "Alice") }
        val resultBob = async { sayAfter(200u, "Bob") }
        resultAlice.await() shouldBe "Hello, Alice!"
        resultBob.await() shouldBe "Hello, Bob!"
    }

    @Test
    fun testAsyncMethodsFromObject() = assertApproximateTime(200) {
        val megaphone = newMegaphone()
        megaphone.sayAfter(200u, "Alice") shouldBe "HELLO, ALICE!"
    }

    @Test
    fun testAsyncMethodsFromTopLevel() = assertApproximateTime(200) {
        val megaphone = newMegaphone()
        sayAfterWithMegaphone(megaphone, 200u, "Alice") shouldBe "HELLO, ALICE!"
    }

    @Test
    fun testAsyncMethodReturningOptional() = runBlocking {
        asyncMaybeNewMegaphone(true) shouldNotBe null
        asyncMaybeNewMegaphone(false) shouldBe null
    }

    @Test
    fun testWithTokioRuntime() = assertApproximateTime(200) {
        sayAfterWithTokio(200u, "Alice") shouldBe "Hello, Alice (with Tokio)!"
    }

    @Test
    fun testFallibleFunctions() = assertMaxTime(100) {
        fallibleMe(false)
        assertFails {
            fallibleMe(true)
        }
    }

    @Test
    fun testFallibleMethods() = assertMaxTime(100) {
        val megaphone = newMegaphone()
        megaphone.fallibleMe(false)
        assertFails {
            megaphone.fallibleMe(true)
        }
    }

    @Test
    fun testFallibleStruct() = assertMaxTime(100) {
        fallibleStruct(false) shouldNotBe null
        assertFails {
            fallibleStruct(true)
        }
    }

    @Test
    fun testRecord() = assertMaxTime(150) {
        newMyRecord("foo", 42u) shouldBe MyRecord("foo", 42u)
    }

    @Test
    fun testBrokenSleep() = assertApproximateTime(500) {
        brokenSleep(100U, 0U) // calls the waker twice immediately
        sleep(100U) // wait for possible failure

        brokenSleep(100U, 100U) // calls the waker a second time after 1s
        sleep(200U) // wait for possible failure
    }

    @Test
    fun testFutureWithLockAndCancelled() = assertMaxTime(60) {
        val job = launch {
            useSharedResource(SharedResourceOptions(releaseAfterMs = 5000U, timeoutMs = 100U))
        }

        // Wait some time to ensure the task has locked the shared resource
        delay(50)
        // Cancel the job before the shared resource has been released.
        job.cancel()

        // Try accessing the shared resource again.  The initial task should release the shared resource
        // before the timeout expires.
        useSharedResource(SharedResourceOptions(releaseAfterMs = 0U, timeoutMs = 1000U))
    }

    @Test
    fun testFutureWithLockButNotCancelled() = assertApproximateTime(100) {
        useSharedResource(SharedResourceOptions(releaseAfterMs = 100U, timeoutMs = 1000U))
        useSharedResource(SharedResourceOptions(releaseAfterMs = 0U, timeoutMs = 1000U))
    }
}

fun assertReturnsImmediately(block: suspend CoroutineScope.() -> Unit) = assertMaxTime(4, block)

@OptIn(ExperimentalTime::class)
fun assertApproximateTime(expectedTime: Int, block: suspend CoroutineScope.() -> Unit) = runBlocking {
    val actualTime = measureTime {
        block()
    }.inWholeMilliseconds

    assertTrue(
        actualTime >= expectedTime && actualTime <= expectedTime + 100,
        "unexpected time: ${actualTime}ms"
    )
}

@OptIn(ExperimentalTime::class)
fun assertMaxTime(maxTime: Int, block: suspend CoroutineScope.() -> Unit) = runBlocking {
    val actualTime = measureTime {
        block()
    }.inWholeMilliseconds

    assertTrue(
        actualTime <= maxTime,
        "unexpected time: ${actualTime}ms"
    )
}
