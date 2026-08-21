
{%- let rec = ci|get_record_definition(name) %}

object {{ rec|ffi_converter_name }}: FfiConverterRustBuffer<{{ type_name }}> {
    override fun read(buf: ByteBuffer): {{ type_name }} {
        {%- if rec.has_fields() %}
        return {{ type_name }}(
        {%- for field in rec.fields() %}
            {{ field|read_fn }}(buf),
        {%- endfor %}
        )
        {%- else %}
        return {{ type_name }}()
        {%- endif %}
    }

    override fun allocationSize(value: {{ type_name }}) = {%- if rec.has_fields() %} (
        {%- for field in rec.fields() %}
            {{ field|allocation_size_fn }}(value.{{ field.name()|var_name }}){% if !loop.last %} +{% endif %}
        {%- endfor %}
    ) {%- else %} 0UL {%- endif %}

    override fun write(value: {{ type_name }}, buf: ByteBuffer) {
        {%- for field in rec.fields() %}
            {{ field|write_fn }}(value.{{ field.name()|var_name }}, buf)
        {%- endfor %}
    }
}

{#- The `actual` side of the shims `generic/common/RecordTemplate.kt` declared for this
    record's methods and uniffi trait exports; see `self_shim_expect` in `macros.kt`. -#}
{%- let uniffi_trait_methods = rec.uniffi_trait_methods() %}
{%- call kt::self_shims_actual(rec.methods(), uniffi_trait_methods, type_name, false) %}{% endcall %}
