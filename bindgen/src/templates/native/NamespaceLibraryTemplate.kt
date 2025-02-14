
// Define FFI callback types

{%- for def in self.ffi_definitions_no_builtins() %}
{%- match def %}
{%- when FfiDefinition::CallbackFunction(callback) %}
internal typealias {{ callback.name()|ffi_callback_name }} = {{ ci.namespace() }}.cinterop.{{ callback.name()|ffi_callback_name }}
{%- when FfiDefinition::Struct(ffi_struct) %}
internal typealias {{ ffi_struct.name()|ffi_struct_name }} = CPointer<{{ ci.namespace() }}.cinterop.{{ ffi_struct.name()|ffi_struct_name }}>
{% for field in ffi_struct.fields() %}
internal var {{ ffi_struct.name()|ffi_struct_name }}.{{ field.name()|var_name }}: {{ field.type_().borrow()|ffi_type_name_for_ffi_struct }}
{% let type_name = field.type_().borrow()|ffi_type_name_for_ffi_struct -%}
    get() = pointed.{{ field.name()|var_name }}
        {%-  if type_name.contains("ByValue") -%}
        .readValue()
        {%- else if field.type_().borrow()|is_pointer_type -%}
        ?.let { Pointer(it) }
        {%- else -%}
        /* test  {{ type_name }} */
        {% endif %}
    set(value) {
        {%- match field.type_() %}
        {%- when FfiType::RustBuffer(_) %}
        value.write(pointed.{{ field.name()|var_name }}.rawPtr)
        {%- when FfiType::RustCallStatus %}
        value.write(pointed.{{ field.name()|var_name }}.rawPtr)
        {%- when _ %}
        {%- if field.type_().borrow()|is_pointer_type -%}
        pointed.{{ field.name()|var_name }} = value?.inner
        {%- else -%}
        pointed.{{ field.name()|var_name }} = value as {{ field.type_().borrow()|ffi_type_name_for_ffi_struct_inner }}
        {%- endif -%}
        {%- endmatch %}
    }
{% endfor %}

internal fun {{ ffi_struct.name()|ffi_struct_name }}.uniffiSetValue(other: {{ ffi_struct.name()|ffi_struct_name }}) {
    {%- for field in ffi_struct.fields() %}
    {{ field.name()|var_name }} = other.{{ field.name()|var_name }}
    {%- endfor %}
}
internal fun {{ ffi_struct.name()|ffi_struct_name }}.uniffiSetValue(other: {{ ffi_struct.name()|ffi_struct_name }}UniffiByValue) {
    {%- for field in ffi_struct.fields() %}
    {{ field.name()|var_name }} = other.{{ field.name()|var_name }}
    {%- endfor %}
}

internal typealias {{ ffi_struct.name()|ffi_struct_name }}UniffiByValue = CValue<{{ ci.namespace() }}.cinterop.{{ ffi_struct.name()|ffi_struct_name }}>
fun {{ ffi_struct.name()|ffi_struct_name }}UniffiByValue(
    {% for field in ffi_struct.fields() %}
    {{ field.name()|var_name }}: {{ field.type_().borrow()|ffi_type_name_for_ffi_struct }},
    {% endfor %}
): {{ ffi_struct.name()|ffi_struct_name }}UniffiByValue {
    return cValue<{{ ci.namespace() }}.cinterop.{{ ffi_struct.name()|ffi_struct_name }}> {
        {% for field in ffi_struct.fields() %}
        {%- match field.type_() %}
        {%- when FfiType::RustBuffer(_) %}
        {{ field.name()|var_name }}.write(this.{{ field.name()|var_name }}.rawPtr)
        {%- when FfiType::RustCallStatus %}
        {{ field.name()|var_name }}.write(this.{{ field.name()|var_name }}.rawPtr)
        {%- when _ %}
        {%- if field.type_().borrow()|is_pointer_type -%}
        this.{{ field.name()|var_name }} = {{ field.name()|var_name }}?.inner
        {%- else -%}
        this.{{ field.name()|var_name }} = {{ field.name()|var_name }} as {{ field.type_().borrow()|ffi_type_name_for_ffi_struct_inner }}
        {%- endif -%}
        {%- endmatch %}
        {% endfor %}
    }
}

{% for field in ffi_struct.fields() %}
internal val {{ ffi_struct.name()|ffi_struct_name }}UniffiByValue.{{ field.name()|var_name }}: {{ field.type_().borrow()|ffi_type_name_for_ffi_struct }}
{% let type_name = field.type_().borrow()|ffi_type_name_for_ffi_struct -%}
    get() = useContents { {{ field.name()|var_name }} 
    {%-  if type_name.contains("ByValue") -%}
        .readValue()
    {%- else if field.type_().borrow()|is_pointer_type -%}
        ?.let { Pointer(it) }
    {%- else -%}
        /* test  {{ type_name }} */
    {%- endif -%}
    }
{% endfor %}

{%- when FfiDefinition::Function(_) %}
{# functions are handled below #}
{%- endmatch %}
{%- endfor %}


internal interface UniffiLib {
    companion object {
        internal val INSTANCE: UniffiLib by lazy {
            UniffiLibInstance().also { lib ->
             {% for fn in self.initialization_fns() -%}
                {{ fn }}(lib)
             {% endfor -%}
             }
        }
        {% if ci.contains_object_types() %}
        // The Cleaner for the whole library
        internal val CLEANER: UniffiCleaner by lazy {
            UniffiCleaner.create()
        }
        {%- endif %}
    }

    {% for func in ci.iter_ffi_function_definitions() -%}
    fun {{ func.name() }}(
        {%- call kt::arg_list_ffi_decl_for_ffi_function(func) %}
    ): {% match func.return_type() %}{% when Some with (return_type) %}{{ return_type.borrow()|ffi_type_name_for_ffi_function }}{% when None %}Unit{% endmatch %}
    {% endfor %}
}

internal class UniffiLibInstance: UniffiLib {
    {% for func in ci.iter_ffi_function_definitions() -%}
    override fun {{ func.name() }}(
        {%- call kt::arg_list_ffi_decl_for_ffi_function(func) %}
    ): {% match func.return_type() %}{% when Some with (return_type) %}{{ return_type.borrow()|ffi_type_name_for_ffi_function }}{% when None %}Unit{% endmatch %}
        = {{ ci.namespace() }}.cinterop.{{ func.name() }}({%- call kt::arg_list_ffi_call(func) %})
          {%- match func.return_type() -%}
          {%- when Some with (return_type) -%}
          {%- if return_type.borrow()|is_pointer_type -%}
            ?.let { Pointer(it) }
          {%- endif -%}
          {%- when None -%}
          {%- endmatch -%}
          {%- match func.return_type() -%}
          {%- when Some with (return_type) -%}
          as {{ return_type.borrow()|ffi_type_name_for_ffi_function }}
          {%- when None -%}
          {%- endmatch %}
    
    {% endfor %}
}
