
{%- let fully_qualified_type_name = "{}.{}"|format(package_name, name|class_name(ci)) %}
{%- let fully_qualified_ffi_converter_name = "{}.{}"|format(package_name, ffi_converter_name) %}
{%- let fully_qualified_rustbuffer_name = "{}.RustBuffer"|format("uniffi.runtime") %}
{%- let local_rustbuffer_name = "RustBuffer{}"|format(name) %}
{%- let fully_qualified_rustbuffer_by_value_name = "{}.RustBufferByValue"|format("uniffi.runtime") %}
{%- let local_rustbuffer_by_value_name = "RustBuffer{}ByValue"|format(name) %}

{{- self.add_import(fully_qualified_type_name) }}
{{- self.add_import(fully_qualified_ffi_converter_name) }}
{%- if ci.is_name_used_as_error(type_.name().unwrap_or_default()) %}
{%- let fully_qualified_error_handler_name = "{}.{}ErrorHandler"|format(package_name, name) %}
{{- self.add_import(fully_qualified_error_handler_name) }}
{%- endif %}
{#
{{ self.add_import_as(fully_qualified_rustbuffer_name, local_rustbuffer_name) }}
{{ self.add_import_as(fully_qualified_rustbuffer_by_value_name, local_rustbuffer_by_value_name) }}
#}

internal typealias {{ local_rustbuffer_name }} = {{ fully_qualified_rustbuffer_name }}
internal typealias {{ local_rustbuffer_by_value_name }} = {{ fully_qualified_rustbuffer_by_value_name }}
