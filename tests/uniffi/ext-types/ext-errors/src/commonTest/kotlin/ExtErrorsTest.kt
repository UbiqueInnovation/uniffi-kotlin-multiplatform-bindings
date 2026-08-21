import error_lib.ExternalException
import ext_errors.throwExternalError
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ExtErrorsTest {
    @Test
    fun throwsAnErrorDeclaredInAnotherCrate() {
        val e = assertFailsWith<ExternalException.Failed> { throwExternalError() }
        kotlin.test.assertEquals("oops", e.reason)
    }
}
