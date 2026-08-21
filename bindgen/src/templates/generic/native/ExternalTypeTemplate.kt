
{%- let fully_qualified_type_name = "{}.{}"|format(package_name, name|class_name(ci)) %}
{%- let fully_qualified_ffi_converter_name = "{}.FfiConverterType{}"|format(package_name, name) %}
{%- let fully_qualified_rustbuffer_name = "{}.RustBuffer"|format("uniffi.runtime") %}
{%- let local_rustbuffer_name = "RustBuffer{}"|format(name) %}
{%- let fully_qualified_rustbuffer_by_value_name = "{}.RustBufferByValue"|format("uniffi.runtime") %}
{%- let local_rustbuffer_by_value_name = "RustBuffer{}ByValue"|format(name) %}

{{- self.add_import(fully_qualified_type_name) }}
{{- self.add_import(fully_qualified_ffi_converter_name) }}
{#
{{ self.add_import_as(fully_qualified_rustbuffer_name, local_rustbuffer_name) }}
{{ self.add_import_as(fully_qualified_rustbuffer_by_value_name, local_rustbuffer_by_value_name) }}
#}

internal typealias {{ local_rustbuffer_name }} = {{ fully_qualified_rustbuffer_name }}
internal typealias {{ local_rustbuffer_by_value_name }} = {{ fully_qualified_rustbuffer_by_value_name }}
internal fun {{ local_rustbuffer_by_value_name }}(
	capacity: Long,
	len: Long,
	data: Pointer?,
): {{ local_rustbuffer_by_value_name }} = {{ fully_qualified_rustbuffer_by_value_name }}(capacity, len, data)
