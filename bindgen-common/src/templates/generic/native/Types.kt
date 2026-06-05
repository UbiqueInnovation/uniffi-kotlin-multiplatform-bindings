
{%- import "generic/macros.kt" as kt %}

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

{%- when Type::Boolean %}
{%- include "generic/ffi/BooleanHelper.kt" %}

{%- when Type::Int8 %}
{%- include "generic/ffi/Int8Helper.kt" %}

{%- when Type::Int16 %}
{%- include "generic/ffi/Int16Helper.kt" %}

{%- when Type::Int32 %}
{%- include "generic/ffi/Int32Helper.kt" %}

{%- when Type::Int64 %}
{%- include "generic/ffi/Int64Helper.kt" %}

{%- when Type::UInt8 %}
{%- include "generic/ffi/UInt8Helper.kt" %}

{%- when Type::UInt16 %}
{%- include "generic/ffi/UInt16Helper.kt" %}

{%- when Type::UInt32 %}
{%- include "generic/ffi/UInt32Helper.kt" %}

{%- when Type::UInt64 %}
{%- include "generic/ffi/UInt64Helper.kt" %}

{%- when Type::Float32 %}
{%- include "generic/ffi/Float32Helper.kt" %}

{%- when Type::Float64 %}
{%- include "generic/ffi/Float64Helper.kt" %}

{%- when Type::String %}
{%- include "generic/ffi/StringHelper.kt" %}

{%- when Type::Bytes %}
{%- include "generic/ffi/ByteArrayHelper.kt" %}

{%- when Type::Enum { name, module_path } %}
{%- let e = ci.get_enum_definition(name).unwrap() %}
{%- if !ci.is_name_used_as_error(name) %}
{% include "generic/ffi/EnumTemplate.kt" %}
{%- else %}
{% include "generic/ffi/ErrorTemplate.kt" %}
{%- endif -%}

{%- when Type::Object { module_path, name, imp } %}
{% include "generic/ffi/ObjectTemplate.kt" %}
{%- let obj = ci|get_object_definition(name) %}
{%- if obj.has_callback_interface() %}
{%- let vtable = obj.vtable().expect("trait interface should have a vtable") %}
{%- let vtable_methods = obj.vtable_methods() %}
{%- let ffi_init_callback = obj.ffi_init_callback() %}
{% include "CallbackInterfaceImpl.kt" %}
{%- endif %}
{%- if self.include_once_check("interface-support") %}
    {% include "ObjectCleanerHelper.kt" %}
{%- endif %}

{%- when Type::Record { name, module_path } %}
{% include "generic/ffi/RecordTemplate.kt" %}

{%- when Type::Optional { inner_type } %}
{% include "generic/ffi/OptionalTemplate.kt" %}

{%- when Type::Sequence { inner_type } %}
{% include "generic/ffi/SequenceTemplate.kt" %}

{%- when Type::Map { key_type, value_type } %}
{% include "generic/ffi/MapTemplate.kt" %}

{%- when Type::CallbackInterface { module_path, name } %}
{% include "CallbackInterfaceTemplate.kt" %}

{%- when Type::Timestamp %}
{% include "generic/ffi/TimestampHelper.kt" %}

{%- when Type::Duration %}
{% include "generic/ffi/DurationHelper.kt" %}

{%- when Type::Custom { module_path, name, builtin } %}
{% include "generic/ffi/CustomTypeTemplate.kt" %}

{%- when Type::External { module_path, name, namespace, kind, tagged } %}
{% include "ExternalTypeTemplate.kt" %}

{%- else %}
{%- endmatch %}
{%- endfor %}

{%- if ci.has_async_fns() %}
{# Import types needed for async support #}
{{ self.add_import("kotlin.coroutines.resume") }}
{{ self.add_import("kotlinx.coroutines.launch") }}
{{ self.add_import("kotlinx.coroutines.suspendCancellableCoroutine") }}
{{ self.add_import("kotlinx.coroutines.CancellableContinuation") }}
{{ self.add_import("kotlinx.coroutines.DelicateCoroutinesApi") }}
{{ self.add_import("kotlinx.coroutines.Job") }}
{{ self.add_import("kotlinx.coroutines.GlobalScope") }}
{{ self.add_import("kotlinx.coroutines.withContext") }}
{{ self.add_import("kotlinx.coroutines.IO") }}
{{ self.add_import("kotlinx.coroutines.Dispatchers") }}
{%- endif %}
