{%- call kt::docstring_value(ci.namespace_docstring(), 0) %}

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
package {{ config.package_name() }}

import uniffi.runtime.*
import com.sun.jna.Native
import com.sun.jna.Library

{%- for req in self.imports() %}
{{ req.render() }}
{%- endfor %}

// Contains loading, initialization code,
// and the FFI Function declarations in a com.sun.jna.Library.
{% include "NamespaceLibraryTemplate.kt" %}

{% import "macros.kt" as kt %}
