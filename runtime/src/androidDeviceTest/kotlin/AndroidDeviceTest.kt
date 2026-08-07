import org.junit.Assert.assertEquals
import org.junit.Test
import uniffi.runtime.foo

class AndroidDeviceTest {
    @Test
    fun sampleTest() {
        assertEquals(foo(), "Android")
    }
}
