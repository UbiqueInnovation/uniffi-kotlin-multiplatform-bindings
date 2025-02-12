{%- call kt::docstring_value(ci.namespace_docstring(), 0) %}

@file:Suppress("NAME_SHADOWING","ACTUAL_WITHOUT_EXPECT", "ACTUAL_TYPE_ALIAS_WITH_USE_SITE_VARIANCE", "ACTUAL_TYPE_ALIAS_WITH_COMPLEX_SUBSTITUTION", "ACTUAL_TYPE_ALIAS_TO_CLASS_WITH_DECLARATION_SITE_VARIANCE", "INCOMPATIBLE_MATCHING")

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
