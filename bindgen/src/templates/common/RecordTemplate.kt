
{%- let rec = ci|get_record_definition(name) %}
// Generated records, do not modify
{%- if rec.has_fields() %}
{%- call kt::docstring(rec, 0) %}
{%- if config.generate_serializable_records() && self.is_serializable(rec) %}
// it is serializable
@kotlinx.serialization.Serializable
{% endif%}
data class {{ type_name }} (
    {%- for field in rec.fields() %}
    {%- call kt::docstring(field, 4) %}
    @kotlinx.serialization.json.JsonNames("{% call kt::field_name_unquoted_unescaped(field, loop.index) %}")
    {%-if !field.name().is_empty() %}
    @kotlinx.serialization.SerialName("{{ field.name() }}")
    {%- endif %}
    {% if config.generate_immutable_records() %}val{% else %}var{% endif %} {{ field.name()|var_name }}: {{ field|type_name(ci) -}}
    {%- match field.default_value() %}
        {%- when Some with(literal) %} = {{ literal|render_literal(field, ci) }}
        {%- else %}
{% if field|is_optional %} = null {% endif %}
    {%- endmatch -%}
    {% if !loop.last %}, {% endif %}
    {%- endfor %}
) {% if contains_object_references %}: Disposable {% endif %}{
    {% if contains_object_references %}
    @Suppress("UNNECESSARY_SAFE_CALL") // codegen is much simpler if we unconditionally emit safe calls here
    override fun destroy() {
        {% call kt::destroy_fields(rec) %}
    }
    {% endif %}
    companion object
}
{%- else -%}
{%- call kt::docstring(rec, 0) %}
class {{ type_name }} {
    override fun equals(other: Any?): Boolean {
        return other is {{ type_name }}
    }

    override fun hashCode(): Int {
        return {{ type_name }}::class.hashCode()
    }

    companion object
}
{%- endif %}

public object {{ rec|ffi_converter_name }}: FfiConverterRustBuffer<{{ type_name }}> {
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
