import runtime_test.*
import io.kotest.matchers.shouldBe
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
}