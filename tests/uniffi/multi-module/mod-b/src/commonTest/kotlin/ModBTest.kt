import io.kotest.matchers.shouldBe
import module_b.createTestObject
import kotlin.test.Test

class ModBTest {
    @Test
    fun testHello() {
        createTestObject("Test").getName() shouldBe "Test"
    }
}