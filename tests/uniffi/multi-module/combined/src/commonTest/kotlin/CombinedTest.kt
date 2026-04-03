import io.kotest.matchers.shouldBe
import module_a.greet
import module_a.testGetInt
import module_b.createTestObject
import module_b.createTestRecord
import kotlin.test.Test

class MultiModuleTest {
    @Test
    fun testRecord() {
        val r = createTestRecord(1337, "asdf", listOf(42))
        testGetInt(r) shouldBe 1337
    }

	@Test
	fun testObject() {
		val o = createTestObject("John")
		o.getName() shouldBe "John"
		greet(o) shouldBe "Hello John!"
	}
}