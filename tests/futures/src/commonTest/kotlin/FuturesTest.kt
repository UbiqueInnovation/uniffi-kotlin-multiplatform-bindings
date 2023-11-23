import futures.*
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFails

class FuturesTest {

    // FIXME Timing assertions have temporarily been removed, so that test pass on MacOS
    // See https://gitlab.com/trixnity/uniffi-kotlin-multiplatform-bindings/-/issues/23

    // TODO think about some real async/suspend tests
    // This could be done via sync callbacks to Kotlin
    // at key points in the async protocol.
    //
    // For example:
    //
    //    trait SleepCallback {
    //        fn before_sleep(&self);
    //        fn after_sleep(&self);
    //    }
    //
    //    async func sleep(ms: u16, sleep_callback: Box<dyn SleepCallback>) {
    //        // ...
    //    }

    @Test
    fun testAlwaysReady() = runTest {
        alwaysReady() shouldBe true
    }

    @Test
    fun testVoid() = runTest {
        void() shouldBe Unit
    }

    @Test
    fun testSleep() = runTest {
        sleep(200u) shouldBe true
    }

    // This test is to ensure that coroutines still work after multiple coroutine calls.
    // Coroutines may halt or crash after several coroutine invocations due to wrong
    // atomic operations in the generated binding. Please see #24 for more details.
    @Test
    fun testSleepWithRepeat() = runTest {
        repeat(65) {
            sleep(20u) shouldBe true
        }
    }

    @Test
    fun testSequentialFutures() = runTest {
        sayAfter(100u, "Alice") shouldBe "Hello, Alice!"
        sayAfter(200u, "Bob") shouldBe "Hello, Bob!"
    }

    @Test
    fun testConcurrentFutures() = runTest {
        val resultAlice = async { sayAfter(100u, "Alice") }
        val resultBob = async { sayAfter(200u, "Bob") }
        resultAlice.await() shouldBe "Hello, Alice!"
        resultBob.await() shouldBe "Hello, Bob!"
    }

    @Test
    fun testAsyncMethodsFromObject() = runTest {
        val megaphone = newMegaphone()
        megaphone.sayAfter(200u, "Alice") shouldBe "HELLO, ALICE!"
    }

    @Test
    fun testAsyncMethodsFromTopLevel() = runTest {
        val megaphone = newMegaphone()
        sayAfterWithMegaphone(megaphone, 200u, "Alice") shouldBe "HELLO, ALICE!"
    }

    @Test
    fun testAsyncMethodReturningOptional() = runTest {
        asyncMaybeNewMegaphone(true) shouldNotBe null
        asyncMaybeNewMegaphone(false) shouldBe null
    }

    @Test
    fun testWithTokioRuntime() = runTest {
        sayAfterWithTokio(200u, "Alice") shouldBe "Hello, Alice (with Tokio)!"
    }

    @Test
    fun testFallibleFunctions() = runTest {
        fallibleMe(false)
        assertFails {
            fallibleMe(true)
        }
    }

    @Test
    fun testFallibleMethods() = runTest {
        val megaphone = newMegaphone()
        megaphone.fallibleMe(false)
        assertFails {
            megaphone.fallibleMe(true)
        }
    }

    @Test
    fun testFallibleStruct() = runTest {
        fallibleStruct(false) shouldNotBe null
        assertFails {
            fallibleStruct(true)
        }
    }

    @Test
    fun testRecord() = runTest {
        newMyRecord("foo", 42u) shouldBe MyRecord("foo", 42u)
    }

    @Test
    fun testBrokenSleep() = runTest {
        brokenSleep(100U, 0U) // calls the waker twice immediately
        sleep(100U) // wait for possible failure

        brokenSleep(100U, 100U) // calls the waker a second time after 1s
        sleep(200U) // wait for possible failure
    }

    @Test
    fun testFutureWithLockAndCancelled() = runTest {
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
    fun testFutureWithLockButNotCancelled() = runTest {
        useSharedResource(SharedResourceOptions(releaseAfterMs = 100U, timeoutMs = 1000U))
        useSharedResource(SharedResourceOptions(releaseAfterMs = 0U, timeoutMs = 1000U))
    }
}
