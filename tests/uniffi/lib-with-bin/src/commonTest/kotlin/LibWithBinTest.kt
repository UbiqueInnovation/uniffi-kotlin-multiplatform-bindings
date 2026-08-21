import lib_with_bin.ping
import kotlin.test.Test
import kotlin.test.assertEquals

class LibWithBinTest {
    @Test
    fun callsIntoALibraryThatAlsoHasABinaryTarget() {
        assertEquals("pong", ping())
    }
}
