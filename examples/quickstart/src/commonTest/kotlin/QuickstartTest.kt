import com.example.quickstart.add
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class QuickstartTest {
	@Test
	fun itWorks() {
		add(2, 2) shouldBe 4
	}
}
