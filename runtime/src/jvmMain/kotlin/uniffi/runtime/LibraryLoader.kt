package uniffi.runtime

import com.sun.jna.Library
import com.sun.jna.Native
import java.io.File
import java.io.IOException

// What each library was loaded as, by its name: the absolute path of the library bundled on the
// classpath, or the name itself without a bundled library.
private val loadedLibraries = HashMap<String, String>()

// Loads the library `cdylibName` for the bindings of the namespace `componentName`.
//
// JNA looks a library up by name in the system library paths before the classpath, and on macOS and
// Windows these include the working directory, so a library with the same name placed there would be
// loaded instead of the bundled one. The library bundled on the classpath is loaded by its absolute
// path instead, which skips that lookup. Without a bundled library for the platform, the library is
// looked up by name. The system property `uniffi.component.<componentName>.libraryOverride` takes
// precedence over both.
//
// A library bundled in a JAR file is extracted once for all the namespaces generated from it. Each
// extracted copy would be loaded as a library of its own, and the callback interfaces one namespace
// registers would be missing from the copy of another.
@Synchronized
fun <Lib : Library> uniffiLoadLibrary(componentName: String, cdylibName: String, lib: Class<Lib>): Lib {
    val libraryOverride = System.getProperty("uniffi.component.$componentName.libraryOverride")
    if (libraryOverride != null) {
        return Native.load(libraryOverride, lib)
    }
    // JNA still has the library open under this name, so it needs no file to load it again.
    val loadedAs = loadedLibraries[cdylibName]
    if (loadedAs != null) {
        return Native.load(loadedAs, lib)
    }
    val bundledLibrary = try {
        Native.extractFromResourcePath(cdylibName, lib.classLoader)
    } catch (e: IOException) {
        null
    }
    val name = bundledLibrary?.absolutePath ?: cdylibName
    try {
        return Native.load(name, lib).also { loadedLibraries[cdylibName] = name }
    } finally {
        bundledLibrary?.let(::deleteExtractedLibrary)
    }
}

// Deletes the temporary file JNA extracted a bundled library to, as JNA does for the libraries it
// extracts itself. A library on a `file:` classpath is used in place and kept. Windows can't delete a
// loaded library: the marker file makes JNA delete it the next time it starts.
private fun deleteExtractedLibrary(library: File) {
    if (!Regex("jna-?\\d+\\.(tmp|dll)").matches(library.name) || library.delete()) return
    try {
        File(library.parentFile, library.name + ".x").createNewFile()
    } catch (e: IOException) {
    }
}
