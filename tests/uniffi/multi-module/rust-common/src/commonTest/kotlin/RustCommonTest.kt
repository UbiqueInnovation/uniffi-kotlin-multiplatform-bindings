import io.kotest.matchers.shouldBe
import kotlin.test.Test
import rust_common.*

class RustCommonTest {
    @Test
    fun testHello() {
        hello() shouldBe "Hello"
    }
}