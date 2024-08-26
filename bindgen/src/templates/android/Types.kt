
{%- for type_ in ci.iter_types() %}
{%- let type_name = type_|type_name(ci) %}
{%- let ffi_converter_name = type_|ffi_converter_name %}
{%- let canonical_type_name = type_|canonical_name %}
{%- let contains_object_references = ci.item_contains_object_references(type_) %}

{#
 # Map `Type` instances to an include statement for that type.
 #
 # There is a companion match in `KotlinCodeOracle::create_code_type()` which performs a similar function for the
 # Rust code.
 #
 #   - When adding additional types here, make sure to also add a match arm to that function.
 #   - To keep things manageable, let's try to limit ourselves to these 2 mega-matches
 #}
{%- match type_ %}
{%- when Type::CallbackInterface { module_path, name } %}
{% include "CallbackInterfaceTemplate.kt" %}
{%- when Type::Object { module_path, name, imp } %}
{%- let obj = ci|get_object_definition(name) %}
{%- if obj.has_callback_interface() %}
{%- let vtable = obj.vtable().expect("trait interface should have a vtable") %}
{%- let vtable_methods = obj.vtable_methods() %}
{%- let ffi_init_callback = obj.ffi_init_callback() %}
{% include "CallbackInterfaceImpl.kt" %}
{%- endif %}
{%- if self.include_once_check("interface-support") %}
    {%- include "ObjectCleanerHelper.kt" %}
{%- endif %}

{%- else %}
{%- endmatch %}
{%- endfor %}