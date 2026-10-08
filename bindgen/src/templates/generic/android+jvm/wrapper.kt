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
)
@file:OptIn(kotlin.time.ExperimentalTime::class)

package {{ config.package_name() }}

// Helper code that doesn't depend on this crate comes from the `uniffi.runtime` package.
// It must match the version of the generator that produced this file.

import uniffi.runtime.*;

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Structure
import com.sun.jna.Callback
import com.sun.jna.ptr.*
import kotlin.coroutines.resume

{%- for req in self.imports() %}
{{ req.render() }}
{%- endfor %}

// Contains loading, initialization code,
// and the FFI Function declarations in a com.sun.jna.Library.
{% include "NamespaceLibraryTemplate.kt" %}

// Public interface members begin here.
{{ type_helper_code }}

{% import "macros.kt" as kt %}

{%- for func in ci.function_definitions() %}
{%- include "generic/ffi/TopLevelFunctionTemplate.kt" %}
{%- endfor %}
