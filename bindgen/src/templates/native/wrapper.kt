{%- call kt::docstring_value(ci.namespace_docstring(), 0) %}

@file:Suppress("NAME_SHADOWING", "ACTUAL_WITHOUT_EXPECT", "INCOMPATIBLE_MATCHING")
@file:OptIn(ExperimentalForeignApi::class)


package {{ config.package_name() }}

// Common helper code.
//
// Ideally this would live in a separate .kt file where it can be unittested etc
// in isolation, and perhaps even published as a re-useable package.
//
// However, it's important that the details of how this helper code works (e.g. the
// way that different builtin types are passed across the FFI) exactly match what's
// expected by the Rust code on the other side of the interface. In practice right
// now that means coming from the exact some version of `uniffi` that was used to
// compile the Rust component. The easiest way to ensure this is to bundle the Kotlin
// helpers directly inline like we're doing here.

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.COpaquePointerVar
import kotlinx.cinterop.CPointed
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVarOf
import kotlinx.cinterop.interpretCPointer
import kotlinx.cinterop.nativeNullPtr
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
import kotlinx.cinterop.alloc
import kotlinx.cinterop.cValue
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readValue
import kotlinx.cinterop.toCPointer
import kotlin.experimental.ExperimentalNativeApi
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.value
import kotlin.coroutines.resume

{%- for req in self.imports() %}
{{ req.render() }}
{%- endfor %}

{% if  !config.has_import_helpers() %}

{% include "PointerHelper.kt" %}

{% include "RustBufferTemplate.kt" %}
{% include "Helpers.kt" %}
{% include "ReferenceHelper.kt" %}



// Async support
{%- if ci.has_async_fns() %}
{% include "Async.kt" %}
{%- endif %}

{% else %}
import {{ config.import_helper_namespace() }}.*
{% endif %}

// Public interface members begin here.
{{ type_helper_code }}

// Contains loading, initialization code,
// and the FFI Function declarations in a com.sun.jna.Library.
{% include "NamespaceLibraryTemplate.kt" %}

{% import "macros.kt" as kt %}