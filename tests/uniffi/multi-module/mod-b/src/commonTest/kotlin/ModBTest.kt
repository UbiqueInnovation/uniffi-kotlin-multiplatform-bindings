import io.kotest.matchers.shouldBe
import module_b.createTestObject
import module_b.modBCallCallback
import rust_common.TestCallback
import kotlin.test.Test

class ModBTest {
    @Test
    fun testHello() {
        createTestObject("Test").getName() shouldBe "Test"
    }

    /** As `ModATest.testCallback`, from the second consumer of `rust_common`. */
    @Test
    fun testCallback() {
        modBCallCallback(CallbackImpl) shouldBe "Hello"
    }

    object CallbackImpl : TestCallback {
        override fun callback(): String {
            return "Hello"
        }
    }
}
