import io.kotest.matchers.shouldBe
import module_a.testGetInt
import module_b.createTestRecord
import kotlin.test.Test

class MultiModuleTest {
    @Test
    fun testRecord() {
        val r = createTestRecord(1337, "asdf", listOf(42))
        testGetInt(r) shouldBe 1337
    }
}