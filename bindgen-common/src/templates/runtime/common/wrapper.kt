{%- call kt::docstring_value(ci.namespace_docstring(), 0) %}

package {{ config.package_name() }}

{% import "runtime/macros.kt" as kt %}
