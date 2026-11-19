import dev.oxikt.runtime.getPointerNativeValue
import dev.oxikt.runtime.toPointer
import org.junit.Assert.assertEquals
import org.junit.Test

class AndroidDeviceTest {
    @Test
    fun pointerRoundTrip() {
        assertEquals(0x1234L, getPointerNativeValue(0x1234L.toPointer()))
    }
}
