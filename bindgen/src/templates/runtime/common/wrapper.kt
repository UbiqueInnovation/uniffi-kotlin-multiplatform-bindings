{%- call kt::docstring_value(ci.namespace_docstring(), 0) %}{% endcall %}

package {{ config.package_name() }}

{% import "macros.kt" as kt %}
