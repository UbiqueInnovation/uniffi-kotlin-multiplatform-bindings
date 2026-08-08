import com.example.swiftinterop.greetFromRust
import io.kotest.matchers.shouldBe
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.test.Test
import swiftGreeter.Greeter

@OptIn(ExperimentalForeignApi::class)
class SwiftInteropTest {
    @Test
    fun callsIntoRust() {
        greetFromRust("Kotlin") shouldBe "Hello Kotlin, from Rust!"
    }

    @Test
    fun callsIntoSwift() {
        Greeter.greetWithName("Kotlin") shouldBe "Hello Kotlin, from Swift!"
    }
}
