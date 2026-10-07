{%- call kt::docstring_value(ci.namespace_docstring(), 0) %}{% endcall %}

@file:Suppress(
    "NAME_SHADOWING",
    "INCOMPATIBLE_MATCHING",
    "RemoveRedundantBackticks",
    "KotlinRedundantDiagnosticSuppress",
    "UnusedImport",
    "unused",
    "RemoveRedundantQualifierName",
    "UnnecessaryOptInAnnotation"
)@file:OptIn(ExperimentalForeignApi::class)


package {{ config.package_name() }}

// Helper code that doesn't depend on this crate comes from the `uniffi.runtime` package.
// It must match the version of the generator that produced this file.

import uniffi.runtime.*

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.COpaquePointerVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CValue
import kotlinx.cinterop.DoubleVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.FloatVar
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.LongVar
import kotlinx.cinterop.ShortVar
import kotlinx.cinterop.get
import kotlinx.cinterop.pointed
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.set
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.useContents
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.cValue
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.plus
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readValue
import kotlinx.cinterop.toCPointer
import kotlinx.cinterop.usePinned
import kotlin.experimental.ExperimentalNativeApi
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.value
import kotlinx.cinterop.CFunction
import kotlinx.cinterop.write
import kotlin.coroutines.resume
import platform.posix.memcpy

// Contains loading, initialization code,
// and the FFI Function declarations.
{% include "NamespaceLibraryTemplate.kt" %}

{% import "macros.kt" as kt %}
