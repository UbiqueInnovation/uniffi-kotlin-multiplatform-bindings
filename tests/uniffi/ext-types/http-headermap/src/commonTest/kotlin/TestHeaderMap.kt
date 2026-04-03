import http_headermap.getHeadermap
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class TestHeaderMap {
	@Test
	fun testCreateHeaderMap() {
		val v = "Test"
		val headerMap = getHeadermap(v)

		headerMap[0].key shouldBe "test-header"
		headerMap[0].`val` shouldBe "First value"

		headerMap[1].key shouldBe "test-header"
		headerMap[1].`val` shouldBe v
	}
}