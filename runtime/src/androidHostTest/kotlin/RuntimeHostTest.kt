import dev.oxikt.runtime.getPointerNativeValue
import dev.oxikt.runtime.toPointer
import io.kotest.matchers.shouldBe
import org.junit.Test

class RuntimeHostTest {
    @Test
    fun pointerRoundTrip() {
        getPointerNativeValue(0x1234L.toPointer()) shouldBe 0x1234L
    }
}
