import io.kotest.matchers.shouldBe
import module_a.*
import rust_common.TestCallback
import rust_common.TestObject
import rust_common.TestRecord
import kotlin.test.Test

class ModATest {
    @Test
    fun testHello() {
        hello() shouldBe "Hello"
    }

    @Test
    fun testGetInt() {
        val r = TestRecord(1, "asdf", listOf(1, 2, 3))
        testGetInt(r) shouldBe 1
    }

    @Test
    fun testObj() {
        val o = TestObject("Alex")
        greet(o) shouldBe "Hello Alex!"
    }

    /**
     * `TestCallback` is declared by `rust_common`, which is a different Gradle module and
     * therefore a different library: `module_a` links its own copy of `rust_common`'s Rust,
     * with its own vtable cell. Before `UniffiVtableRegistry` this aborted the process
     * (SIGABRT) rather than throwing, because Rust called through a null vtable.
     */
    @Test
    fun testCallback() {
        differentCrateCallCallback(CallbackImpl) shouldBe "Hello"
    }

    /** The same trait, but declared here - so the vtable cell is in our own library. */
    @Test
    fun testOwnCallback() {
        modACallOwnCallback(OwnCallbackImpl) shouldBe "Hello from module_a"
    }

    object CallbackImpl : TestCallback {
        override fun callback(): String {
            return "Hello"
        }
    }

    object OwnCallbackImpl : ModACallback {
        override fun callback(): String {
            return "Hello from module_a"
        }
    }
}
