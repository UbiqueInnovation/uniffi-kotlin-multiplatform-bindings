import com.example.modb.testModB
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ModBTest {
    @Test
    fun testHello() {
        testModB() shouldBe "Hello"
    }
}