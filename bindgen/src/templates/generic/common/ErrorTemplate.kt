
{%- let type_name = type_|type_name(ci) %}
{%- let ffi_converter_name = type_|ffi_converter_name %}
{%- let canonical_type_name = type_|canonical_name %}
{#- An error enum is a Kotlin `sealed class`, not an `enum class`, whichever shape the
    Rust enum has - so all four uniffi traits can be rendered, and `flat` is never set
    when calling into the macros below. -#}
{%- let comparable = e.uniffi_trait_methods().ord_cmp.is_some() %}

{% if e.is_flat() %}
{%- call kt::docstring(e, 0) %}{% endcall %}
sealed class {{ type_name }}(message: String): kotlin.Exception(message)
{%- if contains_object_references %}, Disposable{% endif %}
{%- if comparable %}, Comparable<{{ type_name }}>{% endif %} {
    {% for variant in e.variants() -%}
    {%- call kt::docstring(variant, 4) %}{% endcall %}
    class {{ variant|error_variant_name }}(message: String) : {{ type_name }}(message)
    {% endfor %}

    {% for meth in e.methods() -%}
    {%- call kt::self_method_decl(meth, 4) %}{% endcall %}
    {% endfor %}
    {%- let uniffi_trait_methods = e.uniffi_trait_methods() %}
    {%- call kt::self_uniffi_trait_impls(uniffi_trait_methods, type_name, false) %}{% endcall %}
}
{%- else %}
{%- call kt::docstring(e, 0) %}{% endcall %}
sealed class {{ type_name }}: kotlin.Exception()
{%- if contains_object_references %}, Disposable{% endif %}
{%- if comparable %}, Comparable<{{ type_name }}>{% endif %} {
    {% for variant in e.variants() -%}
    {%- call kt::docstring(variant, 4) %}{% endcall %}
    {%- let variant_name = variant|error_variant_name %}
    class {{ variant_name }}(
        {% for field in variant.fields() -%}
        {%- call kt::docstring(field, 8) %}{% endcall %}
        val {% call kt::field_name(field, loop.index) %}{% endcall %}: {{ field|type_name(ci) }}{% if loop.last %}{% else %}, {% endif %}
        {% endfor -%}
    ) : {{ type_name }}() {
        override val message
            get() = "{%- for field in variant.fields() %}{% call kt::field_name_unquoted(field, loop.index) %}{% endcall %}=${ {% call kt::field_name(field, loop.index) %}{% endcall %} }{% if !loop.last %}, {% endif %}{% endfor %}"

        {% if contains_object_references %}
        @Suppress("UNNECESSARY_SAFE_CALL") // codegen is much simpler if we unconditionally emit safe calls here
        override fun destroy() {
            {%- if variant.has_fields() %}
            {% call kt::destroy_fields(variant) %}{% endcall %}
            {% else -%}
            // Nothing to destroy
            {%- endif %}
        }
        {% endif %}
    }
    {% endfor %}

    {% for meth in e.methods() -%}
    {%- call kt::self_method_decl(meth, 4) %}{% endcall %}
    {% endfor %}
    {%- let uniffi_trait_methods = e.uniffi_trait_methods() %}
    {%- call kt::self_uniffi_trait_impls(uniffi_trait_methods, type_name, false) %}{% endcall %}
}
{%- endif %}

{#- The bodies of everything declared above; see `self_shim_expect` in `macros.kt`. -#}
{%- let uniffi_trait_methods = e.uniffi_trait_methods() %}
{%- call kt::self_shims_expect(e.methods(), uniffi_trait_methods, type_name, false) %}{% endcall %}
