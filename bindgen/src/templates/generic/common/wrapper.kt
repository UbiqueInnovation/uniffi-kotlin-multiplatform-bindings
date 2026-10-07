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
@file:OptIn(
    ExperimentalStdlibApi::class,
    kotlin.time.ExperimentalTime::class,
    {%- if config.generate_serializable_records() %}
    kotlinx.serialization.ExperimentalSerializationApi::class
    {%- endif %}
)

package {{ config.package_name() }}

// Helper code that doesn't depend on this crate comes from the `uniffi.runtime` package.
// It must match the version of the generator that produced this file.

import uniffi.runtime.*

import kotlin.jvm.JvmField

{%- for req in self.imports() %}
{{ req.render() }}
{%- endfor %}

{% include "Helpers.kt" %}

// Public interface members begin here.
{{ type_helper_code }}

{%- for func in ci.function_definitions() %}
{% include "TopLevelFunctionTemplate.kt" %}
{%- endfor %}

{% import "macros.kt" as kt %}
