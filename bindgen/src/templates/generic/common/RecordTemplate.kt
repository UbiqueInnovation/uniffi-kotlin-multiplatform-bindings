
{%- let rec = ci|get_record_definition(name) %}
{%- let comparable = rec.uniffi_trait_methods().ord_cmp.is_some() %}

{%- if rec.has_fields() %}
{%- call kt::docstring(rec, 0) %}{% endcall %}
{%- if config.generate_serializable_records() && self.is_name_serializable(type_name) %}
@kotlinx.serialization.Serializable
{% endif%}
data class {{ type_name }} (
    {%- for field in rec.fields() %}
    {%- call kt::docstring(field, 4) %}{% endcall %}
    {%- if config.generate_serializable_records() && self.is_name_serializable(type_name)  %}
    @kotlinx.serialization.json.JsonNames("{% call kt::field_name_unquoted_unescaped(field, loop.index) %}{% endcall %}")
    {%- if !field.name().is_empty() && self.is_name_serializable(type_name)  %}
    @kotlinx.serialization.SerialName("{{ field.name() }}")
    {%- endif -%}
    {%- endif %}
    {% if config.is_record_immutable(name) %}val{% else %}var{% endif %} {{ field.name()|var_name }}: {{ field|type_name(ci) -}}
    {%- match field.default_value() %}
        {%- when Some with(literal) %} = {{ literal|render_default(field, ci, config) }}
        {%- else %}
        {% if field|is_optional %} = null {% endif %}
    {%- endmatch -%}
    {% if !loop.last %}, {% endif %}
    {%- endfor %}
)
{%- if contains_object_references %}: Disposable{% if comparable %}, Comparable<{{ type_name }}>{% endif %}
{%- else if comparable %}: Comparable<{{ type_name }}>
{%- endif %} {
    {% for meth in rec.methods() -%}
    {%- call kt::self_method_decl(meth, 4) %}{% endcall %}
    {% endfor %}
    {%- let uniffi_trait_methods = rec.uniffi_trait_methods() %}
    {%- call kt::self_uniffi_trait_impls(uniffi_trait_methods, type_name, false) %}{% endcall %}
    {% if contains_object_references %}
    @Suppress("UNNECESSARY_SAFE_CALL") // codegen is much simpler if we unconditionally emit safe calls here
    override fun destroy() {
        {% call kt::destroy_fields(rec) %}{% endcall %}
    }
    {% endif %}
    companion object
}
{%- else -%}
{%- call kt::docstring(rec, 0) %}{% endcall %}
class {{ type_name }}{% if comparable %}: Comparable<{{ type_name }}>{% endif %} {
    {% for meth in rec.methods() -%}
    {%- call kt::self_method_decl(meth, 4) %}{% endcall %}
    {% endfor %}
    {%- let uniffi_trait_methods = rec.uniffi_trait_methods() %}
    {%- call kt::self_uniffi_trait_impls(uniffi_trait_methods, type_name, false) %}{% endcall %}
    {#- A fieldless record has nothing to compare, so it gets structural equality by hand -
        unless Rust exported `Eq`/`Hash` for it, in which case the impls above are the
        authority and these would be duplicate declarations. #}
    {%- if uniffi_trait_methods.eq_eq.is_none() %}
    override fun equals(other: Any?): Boolean {
        return other is {{ type_name }}
    }
    {%- endif %}
    {%- if uniffi_trait_methods.hash_hash.is_none() %}

    override fun hashCode(): Int {
        return super.hashCode()
    }
    {%- endif %}

    companion object
}
{%- endif %}

{#- The bodies of everything declared above; see `self_shim_expect` in `macros.kt`. -#}
{%- let uniffi_trait_methods = rec.uniffi_trait_methods() %}
{%- call kt::self_shims_expect(rec.methods(), uniffi_trait_methods, type_name, false) %}{% endcall %}
