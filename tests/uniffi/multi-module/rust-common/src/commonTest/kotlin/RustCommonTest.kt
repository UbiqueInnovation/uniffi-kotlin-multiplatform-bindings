import io.kotest.matchers.shouldBe
import kotlin.test.Test
import rust_common.*

class RustCommonTest {
    @Test
    fun testRecord() {
        val r = TestRecord(1, "asdf", listOf(1,2,3))
    }

    @Test
    fun testObject() {
        val o = TestObject("Alex")
        o.getName() shouldBe "Alex"
    }

    @Test
    fun testCallback() {
        sameCrateCallCallback(CallbackImpl) shouldBe "Hello"
    }

    object CallbackImpl : TestCallback {
        override fun callback(): String {
            return "Hello"
        }
    }
}