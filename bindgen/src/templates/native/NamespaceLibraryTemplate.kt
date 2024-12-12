
// Define FFI callback types

{%- for type_ in ci.iter_types() %}
{%- let ffi_converter_name = type_|ffi_converter_name %}
   // {{ ffi_converter_name }} TEST
{% endfor %}

{%- for def in ci.ffi_definitions() %}
{%- match def %}
{%- when FfiDefinition::CallbackFunction(callback) %}
{%- if callback.name().contains("ForeignFuture") %}
internal actual fun create{{ callback.name()|ffi_callback_name }}Callback(

): Any {
    TODO("upsi")
}
{%- endif -%}
{%- when FfiDefinition::Struct(ffi_struct) %}
internal actual class {{ ffi_struct.name()|ffi_struct_name }} (val inner: CPointer<{{ ci.namespace() }}.cinterop.{{ ffi_struct.name()|ffi_struct_name }}>) {
    actual constructor(
        {%- for field in ffi_struct.fields() -%}
            {{ field.name()|var_name }}: {{ field.type_().borrow()|ffi_type_name_for_ffi_struct }},
        {%- endfor -%}
    ) : this(nativeHeap.alloc<{{ci.namespace()}}.cinterop.{{ ffi_struct.name()|ffi_struct_name  }}>().ptr)
}
{% for field in ffi_struct.fields() %}
internal actual var {{ ffi_struct.name()|ffi_struct_name }}.{{ field.name()|var_name }}: {{ field.type_().borrow()|ffi_type_name_for_ffi_struct }}
{% let type_name = field.type_().borrow()|ffi_type_name_for_ffi_struct -%}

    get() = {%- if field.type_().borrow()|is_pointer_type -%}
                inner.pointed.{{ field.name()|var_name }} {%  if type_name.contains("ByValue") %}.readValue() {%- else -%}?.let { Pointer(it) } {% endif %} 
            {%- else -%}
                {%- if type_name.contains("ByValue") -%}
                    {{ type_name }}(inner.pointed.{{ field.name()|var_name }}.readValue())
                {%- else -%}
                    inner.pointed.{{ field.name()|var_name }}
                {%- endif -%}
            {%- endif %} 
    
    set(value) {
    TODO("implement setter")
    // pointed.{{ field.name()|var_name }} = value as  {{ field.type_().borrow()|ffi_type_name_for_ffi_struct }}
     }
{% endfor %}

internal actual fun {{ ffi_struct.name()|ffi_struct_name }}.uniffiSetValue(other: {{ ffi_struct.name()|ffi_struct_name }}) {
    {%- for field in ffi_struct.fields() %}
    {{ field.name()|var_name }} = other.{{ field.name()|var_name }}
    {%- endfor %}
}
internal actual fun {{ ffi_struct.name()|ffi_struct_name }}.uniffiSetValue(other: {{ ffi_struct.name()|ffi_struct_name }}UniffiByValue) {
    {%- for field in ffi_struct.fields() %}
    {{ field.name()|var_name }} = other.{{ field.name()|var_name }}
    {%- endfor %}
}

internal actual class {{ ffi_struct.name()|ffi_struct_name }}UniffiByValue (val inner: CValue<{{ ci.namespace() }}.cinterop.{{ ffi_struct.name()|ffi_struct_name }}>) {
    actual constructor(
        {%- for field in ffi_struct.fields() -%}
            {{ field.name()|var_name }}: {{ field.type_().borrow()|ffi_type_name_for_ffi_struct }},
        {%- endfor -%}
    ) : this(nativeHeap.alloc<{{ ci.namespace() }}.cinterop.{{ ffi_struct.name()|ffi_struct_name }}>().readValue()) 
}
{% for field in ffi_struct.fields() %}
internal actual var {{ ffi_struct.name()|ffi_struct_name }}UniffiByValue.{{ field.name()|var_name }}: {{ field.type_().borrow()|ffi_type_name_for_ffi_struct }}
{% let type_name = field.type_().borrow()|ffi_type_name_for_ffi_struct -%}
    get() = {% if field.type_().borrow()|is_pointer_type %}
        inner.useContents { {{ field.name()|var_name }} {%  if type_name.contains("ByValue") %}.readValue() {%- else -%}?.let { Pointer(it) }{% endif %} }
    {% else %}
        {%- if type_name.contains("ByValue") -%}
            inner.useContents { {{ type_name }} ({{ field.name()|var_name }}.readValue()) }
        {%- else -%}
            inner.useContents { {{ field.name()|var_name }} }
        {%- endif -%}
    {% endif %} 
    
    set(value) { TODO("Not implemented") }
{% endfor %}

internal actual fun {{ ffi_struct.name()|ffi_struct_name }}UniffiByValue.uniffiSetValue(other: {{ ffi_struct.name()|ffi_struct_name }}) {
    {%- for field in ffi_struct.fields() %}
    {{ field.name()|var_name }} = other.{{ field.name()|var_name }}
    {%- endfor %}
}
internal actual fun {{ ffi_struct.name()|ffi_struct_name }}UniffiByValue.uniffiSetValue(other: {{ ffi_struct.name()|ffi_struct_name }}UniffiByValue) {
    {%- for field in ffi_struct.fields() %}
    {{ field.name()|var_name }} = other.{{ field.name()|var_name }}
    {%- endfor %}
}
{%- when FfiDefinition::Function(_) %}
{# functions are handled below #}
{%- endmatch %}
{%- endfor %}


internal actual interface UniffiLib {
    actual companion object {
        internal actual val INSTANCE: UniffiLib by lazy {
            UniffiLibInstance().also { lib ->
             {% for fn in self.initialization_fns() -%}
                {{ fn }}(lib)
             {% endfor -%}
             }
        }
        {% if ci.contains_object_types() %}
        // The Cleaner for the whole library
        internal actual val CLEANER: UniffiCleaner by lazy {
            UniffiCleaner.create()
        }
        {%- endif %}
    }

    {% for func in ci.iter_ffi_function_definitions() -%}
    actual fun {{ func.name() }}(
        {%- call kt::arg_list_ffi_decl(func) %}
    ): {% match func.return_type() %}{% when Some with (return_type) %}{{ return_type.borrow()|ffi_type_name_by_value }}{% when None %}Unit{% endmatch %}
    {% endfor %}
}

internal class UniffiLibInstance: UniffiLib {
    {% for func in ci.iter_ffi_function_definitions() -%}
    override fun {{ func.name() }}(
        {%- call kt::arg_list_ffi_decl(func) %}
    ): {% match func.return_type() %}{% when Some with (return_type) %}{{ return_type.borrow()|ffi_type_name_by_value }}{% when None %}Unit{% endmatch %}
        =   {%- match func.return_type() -%}
                {%- when Some with (return_type) -%}
                    {%- if return_type.borrow()|ffi_type_name_by_value == "RustBufferByValue" -%}
                        RustBufferByValue({{ ci.namespace() }}.cinterop.{{ func.name() }}({%- call kt::ptrwrap_arg_list_ffi_call(func) %}){% match func.return_type() %}{% when Some with (return_type) %}{% if return_type.borrow()|is_pointer_type %}?.let { Pointer(it) } {% endif %} {% when None %}{% endmatch %} )
                    {%- else -%}
                        {{ ci.namespace() }}.cinterop.{{ func.name() }}({%- call kt::ptrwrap_arg_list_ffi_call(func) %}){% match func.return_type() %}{% when Some with (return_type) %}{% if return_type.borrow()|is_pointer_type %}?.let { Pointer(it) } {% endif %} {% when None %}{% endmatch %} 
                    {%- endif -%}
                {%- when None -%}
                    {{ ci.namespace() }}.cinterop.{{ func.name() }}({%- call kt::ptrwrap_arg_list_ffi_call(func) %}){% match func.return_type() %}{% when Some with (return_type) %}{% if return_type.borrow()|is_pointer_type %}?.let { Pointer(it) } {% endif %} {% when None %}{% endmatch %} 
            {%- endmatch %}
    
    {% endfor %}
}
