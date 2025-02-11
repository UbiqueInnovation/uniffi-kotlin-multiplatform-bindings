import com.example.moda.testModA
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ModATest {
    @Test
    fun testHello() {
        testModA() shouldBe "Hello"
    }
}