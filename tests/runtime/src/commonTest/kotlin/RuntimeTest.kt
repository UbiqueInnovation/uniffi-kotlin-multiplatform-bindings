import runtime_test.*
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class RuntimeTest {
    @Test
    fun testCallFunction() {
        hello() shouldBe "Hello, World!"
    }

    @Test
    fun testRecord() {
        val person = Person("John", 42)
        greet(person) shouldBe "Hello John, you're 42 years old!"
    }

    @Test
    fun testObject() {
        val car = Car()
        car.drive() shouldBe "vroom"
    }

    @Test
    fun testCallback() {
        callCallback(CallbackImpl) shouldBe "Hello"
    }

    object CallbackImpl : MyCallback {
        override fun callback(): String = "Hello"
    }

    @Test
    fun testAsyncFunction() {
        runBlocking {
            asyncHello() shouldBe "Hello"
        }
    }


    @Test
    fun testAsyncCallback() {
        runBlocking {
            callAsyncCallback(AsyncCallbackImpl) shouldBe "Hello"
        }
    }

    object AsyncCallbackImpl : MyAsyncCallback {
        override suspend fun callback(): String = "Hello"
    }
}