import io.kotest.matchers.shouldBe
import org.junit.Test
import uniffi.runtime.foo

class RuntimeHostTest {
    @Test
    fun sampleTest() {
        foo() shouldBe "Android"
    }
}