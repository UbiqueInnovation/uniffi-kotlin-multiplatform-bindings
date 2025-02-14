import io.kotest.matchers.shouldBe
import module_a.*
import rust_common.TestCallback
import rust_common.TestObject
import rust_common.TestRecord
import rust_common.sameCrateCallCallback
import kotlin.test.Test

class ModATest {
    @Test
    fun testHello() {
        hello() shouldBe "Hello"
    }

    @Test
    fun testGetInt() {
        val r = TestRecord(1, "asdf", listOf(1, 2, 3))
        testGetInt(r) shouldBe 1
    }

    @Test
    fun testObj() {
        val o = TestObject("Alex")
        greet(o) shouldBe "Hello Alex!"
    }

//     @Test
//     fun testCallback() {
//         differentCrateCallCallback(CallbackImpl) shouldBe "Hello"
//     }
// 
//     object CallbackImpl : TestCallback {
//         override fun callback(): String {
//             return "Hello"
//         }
//     }

}