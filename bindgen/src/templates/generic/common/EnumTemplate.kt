
{#
// Kotlin's `enum class` construct doesn't support variants with associated data,
// but is a little nicer for consumers than its `sealed class` enum pattern.
// So, we switch here, using `enum class` for enums with no associated data
// and `sealed class` for the general case.
#}

{%- let comparable = e.uniffi_trait_methods().ord_cmp.is_some() %}

{%- if e.is_flat() %}

{%- call kt::docstring(e, 0) %}{% endcall %}
{% match e.variant_discr_type() %}
{% when None %}
{%- if config.generate_serializable_records() && self.is_enum_serializable(e) %}
@kotlinx.serialization.Serializable
{% endif%}
enum class {{ type_name }} {
    {% for variant in e.variants() -%}
    {%- call kt::docstring(variant, 4) %}{% endcall %}
    {{ variant|variant_name }}{% if loop.last %};{% else %},{% endif %}
    {%- endfor %}

    {% for meth in e.methods() -%}
    {%- call kt::self_method_decl(meth, 4) %}{% endcall %}
    {% endfor %}
    {%- let uniffi_trait_methods = e.uniffi_trait_methods() %}
    {%- call kt::self_uniffi_trait_impls(uniffi_trait_methods, type_name, true) %}{% endcall %}

    companion object
}
{% when Some with (variant_discr_type) %}
{%- if config.generate_serializable_records() && self.is_enum_serializable(e) %}
@kotlinx.serialization.Serializable
{% endif%}
enum class {{ type_name }}(val value: {{ variant_discr_type|type_name(ci) }}) {
    {% for variant in e.variants() -%}
    {%- call kt::docstring(variant, 4) %}{% endcall %}
    {{ variant|variant_name }}({{ e|variant_discr_literal(loop.index0) }}){% if loop.last %};{% else %},{% endif %}
    {%- endfor %}

    {% for meth in e.methods() -%}
    {%- call kt::self_method_decl(meth, 4) %}{% endcall %}
    {% endfor %}
    {%- let uniffi_trait_methods = e.uniffi_trait_methods() %}
    {%- call kt::self_uniffi_trait_impls(uniffi_trait_methods, type_name, true) %}{% endcall %}

    companion object
}
{% endmatch %}
{% else %}

    {%- if !contains_object_references && config.generate_serializable_records() && self.is_enum_serializable(e) %}

{%  for variant in e.variants() %}
{% if variant.has_fields() && variant.fields().len() == 1 && variant.fields()[0].name()|var_name|unquote == "" %}
{%- let field = variant.fields()[0] %}
object {{ type_name }}{{ variant|variant_type_name(ci) }}Serializer : kotlinx.serialization.KSerializer<{{ type_name }}> {
    override val descriptor: kotlinx.serialization.descriptors.SerialDescriptor
    get() =  kotlinx.serialization.descriptors.buildClassSerialDescriptor("{{ type_name }}{{ variant|variant_type_name(ci) }}") {
        element("v1",  kotlinx.serialization.serializer<{{ field|type_name(ci) }}>().descriptor)
    }

    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: {{ type_name }}) {
        when(value) {
            is {{type_name}}.{{ variant|variant_type_name(ci) }} ->encoder.encodeSerializableValue(kotlinx.serialization.serializer(),value.v1)
            else -> throw RuntimeException("invalid enum value, something is very wrong!!")
        }
    }
    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): {{type_name}}.{{ variant|variant_type_name(ci) }} {
        val inner : {{ field|type_name(ci) }} = decoder.decodeSerializableValue(kotlinx.serialization.serializer())
        return {{type_name}}.{{ variant|variant_type_name(ci) }}(inner)
    }
}
{% endif %}
{%  endfor %}

// we can serialize this here, but it only works some times
object {{ type_name }}PolySerializer : kotlinx.serialization.json.JsonContentPolymorphicSerializer<{{ type_name }}>({{ type_name }}::class) {
    override fun selectDeserializer(element: kotlinx.serialization.json.JsonElement) : kotlinx.serialization.DeserializationStrategy<{{ type_name }}> {
        {{ self.add_import("kotlinx.serialization.json.jsonObject") }}
        {{ self.add_import("kotlinx.serialization.json.Json") }}
        {{ self.add_import("kotlinx.serialization.builtins.serializer") }}


        {%- for variant in e.variants() %}
        var is{{ variant|variant_type_name(ci) }} = true
        {%- endfor %}
        var fieldName = ""
        var alternativeFieldName = ""

        {%- for variant in e.variants() -%}
        {% if variant.has_fields() %}

        {%- for field in variant.fields() -%}

            {%- let fieldNameInternal = field.name()|var_name|unquote %}
            fieldName = "{% call kt::field_name_unquoted_unescaped(field, loop.index) %}{% endcall %}"
            alternativeFieldName = "{{ field.name() }}"

            // if it has fields, try all field names
            {%- if fieldNameInternal != ""  -%}
                {%- let is_optional = field|is_optional -%}
                {%- if !is_optional %}
                        if ((element is kotlinx.serialization.json.JsonObject && !element.containsKey(fieldName) && !element.containsKey(alternativeFieldName))
                                || (element !is kotlinx.serialization.json.JsonObject)) {
                            is{{ variant|variant_type_name(ci) }} = false
                        }
                {%- endif -%}
            {%- else %}
                try {
                    val objectSerializer : kotlinx.serialization.KSerializer<{{ field|type_name(ci) }}> = kotlinx.serialization.serializer()
                 val inner : {{ field|type_name(ci) }} = kotlinx.serialization.json.Json.decodeFromJsonElement(objectSerializer,element)
                 } catch(e: Exception) {
                  is{{ variant|variant_type_name(ci) }} = false
                 }
            {%- endif -%}

        {%- endfor -%}
        {%- endif -%}
        {%- endfor %}
        return when {
            {%- for variant in e.variants() %}
            {%- if variant.has_fields() && variant.fields().len() == 1 && variant.fields()[0].name()|var_name|unquote == "" -%}
            {%- let field = variant.fields()[0] %}
                    is{{ variant|variant_type_name(ci) }} -> {{ type_name }}{{ variant|variant_type_name(ci) }}Serializer
                    {% else %}
                is{{ variant|variant_type_name(ci) }} ->  {{type_name}}.{{ variant|variant_type_name(ci) }}.serializer()
            {%- endif -%}
            {%- endfor %}
            else -> throw IllegalArgumentException("Unsupported BaseType")
        }


    }
}
{%- call kt::docstring(e, 0) %}{% endcall %}
@kotlinx.serialization.Serializable({{ type_name }}PolySerializer::class)
{% endif %}
sealed class {{ type_name }}
{%- if contains_object_references %}: Disposable{% if comparable %}, Comparable<{{ type_name }}>{% endif %}
{%- else if comparable %}: Comparable<{{ type_name }}>
{%- endif %} {
    {% for variant in e.variants() -%}
    {%- call kt::docstring(variant, 4) %}{% endcall %}
    {% if !variant.has_fields() -%}
    {%- if !contains_object_references && config.generate_serializable_records() && self.is_variant_serializable(variant) %}
    @kotlinx.serialization.Serializable
    {% endif %}
    object {{ variant|variant_type_name(ci) }} : {{ type_name }}() {% if contains_object_references %} {
        override fun destroy() = Unit
    } {% endif %}
    {% else -%}
    {%- if !contains_object_references && config.generate_serializable_records() && self.is_variant_serializable(variant) %}
    @kotlinx.serialization.Serializable{% if variant.has_fields() && variant.fields().len() == 1 && variant.fields()[0].name()|var_name|unquote == "" %}({{ type_name }}{{ variant|variant_type_name(ci) }}Serializer::class) {% endif %}
    {% endif %}
    data class {{ variant|variant_type_name(ci) }}(
        {%- for field in variant.fields() -%}
        {%- call kt::docstring(field, 8) %}{% endcall %}
        {%- if config.generate_serializable_records() && self.is_variant_serializable(variant) %}
        @kotlinx.serialization.json.JsonNames("{% call kt::field_name_unquoted_unescaped(field, loop.index) %}{% endcall %}")
        {%-if !field.name().is_empty() %}
        @kotlinx.serialization.SerialName("{{ field.name() }}")
        {%- endif %}
        {% endif %}
        val {% call kt::field_name(field, loop.index) %}{% endcall %}: {{ field|type_name(ci) }}
        {%- match field.default_value() %}
        {%- when Some with(literal) %} = {{ literal|render_default(field, ci, config) }}
        {%- else %}
        {% if field|is_optional %} = null {% endif %}
        {%- endmatch %}{% if loop.last %}{% else %}, {% endif %}
        {%- endfor -%}
    ) : {{ type_name }}() {
        {#- A `data class` generates `equals`/`hashCode`/`toString` of its own, which would
            shadow the ones on the sealed base, so the trait impls are repeated per variant. #}
        {%- let uniffi_trait_methods = e.uniffi_trait_methods() %}
        {%- call kt::self_uniffi_trait_impls(uniffi_trait_methods, type_name, false) %}{% endcall %}
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
    {%- endif %}
    {% endfor %}

    {#- Methods also have to live on the base: a variant with no fields is an `object`,
        which has no body of its own to hang them on. #}
    {% for meth in e.methods() -%}
    {%- call kt::self_method_decl(meth, 4) %}{% endcall %}
    {% endfor %}
    {%- let uniffi_trait_methods = e.uniffi_trait_methods() %}
    {%- call kt::self_uniffi_trait_impls(uniffi_trait_methods, type_name, false) %}{% endcall %}
}

{% endif %}

{#- The bodies of everything declared above; see `self_shim_expect` in `macros.kt`. -#}
{%- let uniffi_trait_methods = e.uniffi_trait_methods() %}
{%- call kt::self_shims_expect(e.methods(), uniffi_trait_methods, type_name, e.is_flat()) %}{% endcall %}
