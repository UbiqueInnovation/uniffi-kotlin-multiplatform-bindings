import runtime_test.hello
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class RuntimeTest {
    @Test
    fun testCallFunction() {
        hello() shouldBe "Hello, World!"
    }
}