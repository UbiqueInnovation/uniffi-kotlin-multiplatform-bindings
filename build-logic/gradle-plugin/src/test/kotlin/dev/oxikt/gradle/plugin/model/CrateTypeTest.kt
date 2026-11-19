package ch.ubique.uniffi.plugin.model

import kotlin.test.Test
import kotlin.test.assertEquals

class CrateTypeTest {
    @Test
    fun `system crate types use the platform library names`() {
        val crateName = "sample_crate"

        assertEquals("staticlib", CrateType.SystemStaticLibrary.toString())
        assertEquals("cdylib", CrateType.SystemDynamicLibrary.toString())
        assertEquals("libsample_crate.a", CrateType.SystemStaticLibrary.outputFileNameForLinux(crateName))
        assertEquals("libsample_crate.so", CrateType.SystemDynamicLibrary.outputFileNameForLinux(crateName))
        assertEquals("sample_crate.lib", CrateType.SystemStaticLibrary.outputFileNameForMsvc(crateName))
        assertEquals("sample_crate.dll", CrateType.SystemDynamicLibrary.outputFileNameForMsvc(crateName))
        assertEquals("libsample_crate.a", CrateType.SystemStaticLibrary.outputFileNameForMacOS(crateName))
        assertEquals("libsample_crate.dylib", CrateType.SystemDynamicLibrary.outputFileNameForMacOS(crateName))
    }
}
