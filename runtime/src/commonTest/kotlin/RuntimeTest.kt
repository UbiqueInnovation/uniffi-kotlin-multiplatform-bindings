import io.kotest.matchers.string.shouldMatch
import kotlin.test.Test

import uniffi.runtime.*

class RuntimeTest {
    @Test
    fun sampleTest() {
        foo() shouldMatch "JVM|Native|Android"
    }
}