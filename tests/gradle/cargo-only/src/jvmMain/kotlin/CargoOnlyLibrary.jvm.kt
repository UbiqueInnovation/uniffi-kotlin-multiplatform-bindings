/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration

actual object CargoOnlyLibrary {
    init {
        // This value can be configured in the Gradle script.
        val resourcePrefix = "jvm"
        val mappedLibraryName = System.mapLibraryName("uniffi_kmm_fixture_gradle_cargo_only")

        // Extract the library file to a temporary location as in JNA so this works even when packaged as a .jar file.
        val isWindows = System.getProperty("os.name").startsWith("Windows")
        val librarySuffix = ".dll".takeIf { isWindows }
        val libraryFile = File.createTempFile("uniffi_kmm_fixture_gradle_cargo_only", librarySuffix)
        CargoOnlyLibrary::class.java.classLoader!!.getResourceAsStream("$resourcePrefix/$mappedLibraryName")!!.use {
            it.copyTo(libraryFile.outputStream())
        }

        @Suppress("UnsafeDynamicallyLoadedCode")
        Runtime.getRuntime().load(libraryFile.absolutePath)
    }

    @JvmStatic
    actual external fun getHelloWorld(): String

    @JvmStatic
    external fun wait(seconds: Long, nanoseconds: Int, callback: () -> Unit)

    actual suspend fun wait(duration: Duration) {
        if (duration.isNegative()) return
        return suspendCoroutine { continuation ->
            duration.toComponents { seconds, nanoseconds ->
                wait(seconds, nanoseconds) {
                    continuation.resume(Unit)
                }
            }
        }
    }

    actual val optLevel: Int
        @JvmStatic
        external get

    actual val features: Array<String>
        @JvmStatic
        external get
}
