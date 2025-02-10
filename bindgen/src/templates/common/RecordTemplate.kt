
{%- let rec = ci|get_record_definition(name) %}

{%- if rec.has_fields() %}
{%- call kt::docstring(rec, 0) %}
{%- if config.generate_serializable_records() && self.is_serializable(rec) %}
@kotlinx.serialization.Serializable
{% endif%}
data class {{ type_name }} (
    {%- for field in rec.fields() %}
    {%- call kt::docstring(field, 4) %}
    {%- if config.generate_serializable_records() && self.is_serializable(rec) %}
    @kotlinx.serialization.json.JsonNames("{% call kt::field_name_unquoted_unescaped(field, loop.index) %}")
    {%- if !field.name().is_empty() %}
    @kotlinx.serialization.SerialName("{{ field.name() }}")
    {%- endif -%}
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
        return super.hashCode()
    }

    companion object
}
{%- endif %}
