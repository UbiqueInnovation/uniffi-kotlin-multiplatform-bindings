import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test

import uniffi.runtime.*

class RuntimeTest {
    @Test
    fun handleMapHandsOutOddHandles() {
        val map = UniffiHandleMap<String>()
        val handles = List(3) { map.insert("value $it") }
        handles.forEach { (it and 1L) shouldBe 1L }
        handles.toSet().size shouldBe 3
    }

    @Test
    fun handleMapCloneAndRemove() {
        val map = UniffiHandleMap<String>()
        val handle = map.insert("value")
        val clone = map.clone(handle)
        clone shouldNotBe handle
        map.remove(handle) shouldBe "value"
        map.get(clone) shouldBe "value"
        shouldThrow<InternalException> { map.get(handle) }
    }
}
