import org.junit.Assert.assertEquals
import org.junit.Test
import uniffi.runtime.getPointerNativeValue
import uniffi.runtime.toPointer

class AndroidDeviceTest {
    @Test
    fun pointerRoundTrip() {
        assertEquals(0x1234L, getPointerNativeValue(0x1234L.toPointer()))
    }
}
