{#
  Types shared by every UniFFI crate. The runtime's cinterop klib ships this header, and each
  generated crate includes a byte-identical copy. cinterop matches headers by content hash, so
  the crate's klib reuses the runtime's declarations instead of declaring them again. Keep the
  output independent of the crate: the builtins below come from UniFFI's fixed builtin list.
-#}
#pragma once

#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>

typedef struct RustBuffer
{
    int64_t capacity;
    int64_t len;
    uint8_t *_Nullable data;
} RustBuffer;

typedef struct RustBufferByReference
{
    int64_t capacity;
    int64_t len;
    uint8_t *_Nullable data;
} RustBufferByReference;

typedef struct ForeignBytes
{
    int32_t len;
    const uint8_t *_Nullable data;
} ForeignBytes;

typedef struct UniffiRustCallStatus {
  int8_t code;
  RustBuffer errorBuf;
} UniffiRustCallStatus;

{%- for def in self.ffi_definitions_builtins() %}

{%- match def %}
{% when FfiDefinition::CallbackFunction(callback) %}
typedef
    {%- match callback.return_type() %}{% when Some(return_type) %} {{ return_type|header_ffi_type_name(ci) }} {% when None %} void {% endmatch -%}
    (*{{ callback.name()|ffi_callback_name }})(
        {%- for arg in callback.arguments() -%}
        {{ arg.type_().borrow()|header_ffi_type_name(ci) }}
        {%- if !loop.last || callback.has_rust_call_status_arg() %}, {% endif %}
        {%- endfor -%}
        {%- if callback.has_rust_call_status_arg() %}
        UniffiRustCallStatus *_Nonnull uniffiCallStatus
        {%- endif %}
    );
{% when FfiDefinition::Struct(ffi_struct) %}
typedef struct {{ ffi_struct.name()|ffi_struct_name }} {
    {%- for field in ffi_struct.fields() %}
    {{ field.type_().borrow()|header_ffi_type_name(ci) }} {{ field.name()|var_name_raw }};
    {%- endfor %}
} {{ ffi_struct.name()|ffi_struct_name }};
{% when FfiDefinition::Function(func) %}
{% match func.return_type() -%}{%- when Some with (type_) %}{{ type_|header_ffi_type_name(ci) }}{% when None %}void{% endmatch %} {{ func.name() }}(
    {%- if func.arguments().len() > 0 %}
        {%- for arg in func.arguments() %}
            {{- arg.type_().borrow()|header_ffi_type_name(ci) }} {{ arg.name()|var_name|unquote -}}{% if !loop.last || func.has_rust_call_status_arg() %}, {% endif %}
        {%- endfor %}
        {%- if func.has_rust_call_status_arg() %}UniffiRustCallStatus *_Nonnull out_status{% endif %}
    {%- else %}
        {%- if func.has_rust_call_status_arg() %}UniffiRustCallStatus *_Nonnull out_status{%- else %}void{% endif %}
    {% endif %}
);
{%- endmatch %}
{%- endfor %}
