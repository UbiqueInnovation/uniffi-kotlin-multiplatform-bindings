{%- for type_ in ci.iter_types() %}
{%- let type_name = type_|type_name(ci) %}
{%- let ffi_converter_name = type_|ffi_converter_name %}
{%- let canonical_type_name = type_|canonical_name %}
{%- let contains_object_references = ci.item_contains_object_references(type_) %}

{%- match type_ %}

{%- when Type::External { module_path, name, namespace, kind, tagged } %}
{%- let local_rustbuffer_name = "RustBuffer{}"|format(name) %}

typedef RustBuffer {{local_rustbuffer_name}};

{%- else %}
{%- endmatch %}
{%- endfor %}