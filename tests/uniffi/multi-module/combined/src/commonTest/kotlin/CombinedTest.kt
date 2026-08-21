import io.kotest.matchers.shouldBe
import module_a.ModACallback
import module_a.differentCrateCallCallback
import module_a.greet
import module_a.modACallOwnCallback
import module_a.testGetInt
import module_b.createTestObject
import module_b.createTestRecord
import module_b.modBCallCallback
import rust_common.TestCallback
import rust_common.sameCrateCallCallback
import kotlin.test.Test

/**
 * This module has no Rust and no uniffi plugin - it only consumes the three published
 * modules, which is the shape a real app has.
 */
class MultiModuleTest {
    @Test
    fun testRecord() {
        val r = createTestRecord(1337, "asdf", listOf(42))
        testGetInt(r) shouldBe 1337
    }

	@Test
	fun testObject() {
		val o = createTestObject("John")
		o.getName() shouldBe "John"
		greet(o) shouldBe "Hello John!"
	}

    /**
     * A single Kotlin implementation of a `rust_common` trait, handed to every module.
     *
     * `module_a` and `module_b` each statically link their own copy of `rust_common`, so
     * each library has its own `UniffiForeignPointerCell` for this trait; the `rust_common`
     * Kotlin package exists once and can only register with one of them directly.
     * `UniffiVtableRegistry` is what fills in the rest - without it this aborts the JVM
     * with SIGABRT rather than throwing.
     *
     * `module_b` is touched before anything else on purpose: `rust_common`, which declares
     * the trait, then initialises *after* a consumer's library is already registered, which
     * is the direction that only works if the registry replays vtables into known libraries.
     */
    @Test
    fun testCallbackAcrossModules() {
        createTestObject("warm-up").getName() shouldBe "warm-up"

        val callback = CountingCallback()

        modBCallCallback(callback) shouldBe "call 1"
        differentCrateCallCallback(callback) shouldBe "call 2"
        sameCrateCallCallback(callback) shouldBe "call 3"

        // Repeat in a different order. Lowering mints a fresh handle per call, so each
        // receiver owns its own and one module's free cannot invalidate another's.
        differentCrateCallCallback(callback) shouldBe "call 4"
        modBCallCallback(callback) shouldBe "call 5"
        sameCrateCallCallback(callback) shouldBe "call 6"
    }

    /**
     * `ModACallback` is declared by `module_a`, so its init symbol exists in no other
     * library here. Publishing it must skip `rust_common`'s and `module_b`'s images
     * instead of failing to resolve the symbol in them.
     */
    @Test
    fun testModuleLocalCallback() {
        modACallOwnCallback(object : ModACallback {
            override fun callback() = "own"
        }) shouldBe "own"
    }

    private class CountingCallback : TestCallback {
        private var calls = 0
        override fun callback(): String = "call ${++calls}"
    }
}
