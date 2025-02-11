import com.example.moda.testModA
import com.example.modb.testModB
import io.kotest.matchers.*
import module_a.createVec
import module_a.testGetInt
import module_b.vecLength
import rust_common.TestRecord
import kotlin.test.*

class MultiModuleTest {
    @Test
    fun testCallFunctions() {
        testModA() shouldBe testModB()
    }

    @Test
    fun testPassVec() {
        val vec = createVec()
        vecLength(vec) shouldBe 4UL
    }

    @Test
    fun test() {
        val rec = TestRecord(1337, "abc", listOf(1, 2, 3))
        testGetInt(rec) shouldBe 1337
    }
}