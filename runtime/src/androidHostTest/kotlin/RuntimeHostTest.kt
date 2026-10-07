import io.kotest.matchers.shouldBe
import org.junit.Test
import uniffi.runtime.getPointerNativeValue
import uniffi.runtime.toPointer

class RuntimeHostTest {
    @Test
    fun pointerRoundTrip() {
        getPointerNativeValue(0x1234L.toPointer()) shouldBe 0x1234L
    }
}
