import io.kotest.matchers.shouldBe
import kotlin.test.Test
import com.example.jvmonly.add

class QuickstartTest {
	@Test
	fun itWorks() {
		add(2, 2) shouldBe 4
	}
}
