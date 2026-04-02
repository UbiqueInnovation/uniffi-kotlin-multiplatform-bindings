import io.kotest.matchers.shouldBe
import simple.addOne
import simple.addOneToMyStruct
import uniffi.external_lib.MyStruct
import kotlin.test.Test

class SimpleTest {
	@Test
	fun `Test uniffi setup`() {
		val x = 41

		addOne(x) shouldBe 42

		val s = MyStruct(value = 41)
		addOneToMyStruct(s).value shouldBe 42
	}
}