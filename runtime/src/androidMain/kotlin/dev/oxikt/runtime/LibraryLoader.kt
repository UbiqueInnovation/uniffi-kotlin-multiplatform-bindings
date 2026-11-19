package dev.oxikt.runtime

import com.sun.jna.Library
import com.sun.jna.Native

// Loads the library `cdylibName` for the bindings of the namespace `componentName`. It is looked up by
// name, unless the system property `uniffi.component.<componentName>.libraryOverride` names another
// library.
fun <Lib : Library> uniffiLoadLibrary(componentName: String, cdylibName: String, lib: Class<Lib>): Lib {
    val libraryOverride = System.getProperty("uniffi.component.$componentName.libraryOverride")
    return Native.load(libraryOverride ?: cdylibName, lib)
}
