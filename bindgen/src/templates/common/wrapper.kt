{%- call kt::docstring_value(ci.namespace_docstring(), 0) %}

@file:Suppress("NAME_SHADOWING", "ACTUAL_WITHOUT_EXPECT", "INCOMPATIBLE_MATCHING")
@file:OptIn(ExperimentalStdlibApi::class)

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

import kotlin.jvm.JvmField

{%- for req in self.imports() %}
{{ req.render() }}
{%- endfor %}

{% if  !config.has_import_helpers() %}

{% include "PointerHelper.kt" %}

{% include "ByteBuffer.kt" %}
{% include "RustBufferTemplate.kt" %}
{% include "FfiConverterTemplate.kt" %}
{% include "Helpers.kt" %}
{% include "HandleMap.kt" %}
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

{%- for func in ci.function_definitions() %}
{%- include "TopLevelFunctionTemplate.kt" %}
{%- endfor %}

{% import "macros.kt" as kt %}

// Contains loading, initialization code,
// and the FFI Function declarations in a com.sun.jna.Library.
{% include "NamespaceLibraryTemplate.kt" %}