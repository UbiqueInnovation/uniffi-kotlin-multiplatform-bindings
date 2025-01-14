
{#
// Kotlin's `enum class` construct doesn't support variants with associated data,
// but is a little nicer for consumers than its `sealed class` enum pattern.
// So, we switch here, using `enum class` for enums with no associated data
// and `sealed class` for the general case.
#}

{%- if e.is_flat() %}

{%- call kt::docstring(e, 0) %}
{% match e.variant_discr_type() %}
{% when None %}
{%- if config.generate_serializable_records() %}
@kotlinx.serialization.Serializable
{% endif%}
enum class {{ type_name }} {
    {% for variant in e.variants() -%}
    {%- call kt::docstring(variant, 4) %}
    {{ variant|variant_name }}{% if loop.last %};{% else %},{% endif %}
    {%- endfor %}
    companion object
}
{% when Some with (variant_discr_type) %}
{%- if config.generate_serializable_records() %}
@kotlinx.serialization.Serializable
{% endif%}
enum class {{ type_name }}(val value: {{ variant_discr_type|type_name(ci) }}) {
    {% for variant in e.variants() -%}
    {%- call kt::docstring(variant, 4) %}
    {{ variant|variant_name }}({{ e|variant_discr_literal(loop.index0) }}){% if loop.last %};{% else %},{% endif %}
    {%- endfor %}
    companion object
}
{% endmatch %}

public object {{ e|ffi_converter_name }}: FfiConverterRustBuffer<{{ type_name }}> {
    override fun read(buf: ByteBuffer) = try {
        {% if config.use_enum_entries() %}
        {{ type_name }}.entries[buf.getInt() - 1]
        {% else -%}
        {{ type_name }}.values()[buf.getInt() - 1]
        {%- endif %}
    } catch (e: IndexOutOfBoundsException) {
        throw RuntimeException("invalid enum value, something is very wrong!!", e)
    }

    override fun allocationSize(value: {{ type_name }}) = 4UL

    override fun write(value: {{ type_name }}, buf: ByteBuffer) {
        buf.putInt(value.ordinal + 1)
    }
}

{% else %}

{%- call kt::docstring(e, 0) %}
{% if contains_object_references %}
{% else %}
{%- if config.generate_serializable_records() %}
// we can serialize this here, but it only works some times
object {{ type_name }}PolySerializer : kotlinx.serialization.json.JsonContentPolymorphicSerializer<{{ type_name }}>({{ type_name }}::class) {
    override fun selectDeserializer(element: kotlinx.serialization.json.JsonElement) : kotlinx.serialization.DeserializationStrategy<{{ type_name }}> {
        {{ self.add_import("kotlinx.serialization.json.jsonObject") }}
        {{ self.add_import("kotlinx.serialization.json.Json") }}
        {{ self.add_import("kotlinx.serialization.builtins.serializer") }}

        val jsonObject = element.jsonObject
        {% for variant in e.variants() -%}
        var is{{ variant|variant_type_name(ci) }} = true
        {% endfor %}
        var fieldName = ""
        var alternativeFieldName = ""
        val jsonTester = Json {}

        {% for variant in e.variants() -%}
        {% if variant.has_fields() %}
        {%for field in variant.fields() %}

            {%- let fieldNameInternal = field.name()|var_name|unquote %}
            fieldName = "{% call kt::field_name_unquoted_unescaped(field, loop.index) %}"
            alternativeFieldName = "{{ field.name() }}"



            // if it has fields, try all field names
            {% if fieldNameInternal != ""  %}
                {% let is_optional = field|is_optional %}
                {% if !is_optional %}
                        if (!jsonObject.containsKey(fieldName) && !jsonObject.containsKey(alternativeFieldName)) {
                            is{{ variant|variant_type_name(ci) }} = false
                        }
                {% endif %}
            {%else %}

               var innerObject{{ variant|variant_type_name(ci) }} = jsonObject.getValue(fieldName)
                try {
                    val objectSerializer : kotlinx.serialization.KSerializer<{{ field|type_name(ci) }}> = kotlinx.serialization.serializer()
                 val inner : {{ field|type_name(ci) }} = jsonTester.decodeFromJsonElement(objectSerializer,innerObject{{ variant|variant_type_name(ci) }})
                 } catch(e: Exception) {
                  is{{ variant|variant_type_name(ci) }} = false
                 }
            {% endif %}

        {% endfor %}
        {% endif %}
        {% endfor %}

        return when {
            {% for variant in e.variants() -%}
                is{{ variant|variant_type_name(ci) }} -> {{type_name}}.{{ variant|variant_type_name(ci) }}.serializer()
            {% endfor %}
            else -> throw IllegalArgumentException("Unsupported BaseType")
        }


    }
}
// it is serializable
@kotlinx.serialization.Serializable({{ type_name }}PolySerializer::class)
{% endif%}
{% endif %}
sealed class {{ type_name }}{% if contains_object_references %}: Disposable {% endif %} {
    {% for variant in e.variants() -%}
    {%- call kt::docstring(variant, 4) %}
    {% if !variant.has_fields() -%}
    @kotlinx.serialization.Serializable
    object {{ variant|variant_type_name(ci) }} : {{ type_name }}()
    {% else -%}
    {% if contains_object_references %}
    {% else %}
    {%- if config.generate_serializable_records() && self.is_variant_serializable(variant) %}
    // it is serializable but it is not clear how to do it
     @kotlinx.serialization.Serializable
    {% endif%}
    {% endif %}
    data class {{ variant|variant_type_name(ci) }}(
        {%- for field in variant.fields() -%}
        {%- call kt::docstring(field, 8) %}
        {%- if config.generate_serializable_records() && self.is_variant_serializable(variant) %}
        // it is serializable but it is not clear how to do it
        @kotlinx.serialization.SerialName("{% call kt::field_name_unquoted_unescaped(field, loop.index) %}")
        {%-if !field.name().is_empty() %}
        @kotlinx.serialization.json.JsonNames("{{ field.name() }}")
        {%- endif %}
        {% endif%}
        val {% call kt::field_name(field, loop.index) %}: {{ field|type_name(ci) }} {% if field|is_optional %} = null {% endif %} {% if loop.last %}{% else %}, {% endif %}
        {%- endfor -%}
    ) : {{ type_name }}() {
        companion object
    }
    {%- endif %}
    {% endfor %}

    {% if contains_object_references %}
    @Suppress("UNNECESSARY_SAFE_CALL") // codegen is much simpler if we unconditionally emit safe calls here
    override fun destroy() {
        when(this) {
            {%- for variant in e.variants() %}
            is {{ type_name }}.{{ variant|variant_type_name(ci) }} -> {
                {%- if variant.has_fields() %}
                {% call kt::destroy_fields(variant) %}
                {% else -%}
                // Nothing to destroy
                {%- endif %}
            }
            {%- endfor %}
        }.let { /* this makes the `when` an expression, which ensures it is exhaustive */ }
    }
    {% endif %}
    companion object
}

public object {{ e|ffi_converter_name }} : FfiConverterRustBuffer<{{ type_name }}>{
    override fun read(buf: ByteBuffer): {{ type_name }} {
        return when(buf.getInt()) {
            {%- for variant in e.variants() %}
            {{ loop.index }} -> {{ type_name }}.{{ variant|variant_type_name(ci) }}{% if variant.has_fields() %}(
                {% for field in variant.fields() -%}
                {{ field|read_fn }}(buf),
                {% endfor -%}
            ){%- endif -%}
            {%- endfor %}
            else -> throw RuntimeException("invalid enum value, something is very wrong!!")
        }
    }

    override fun allocationSize(value: {{ type_name }}) = when(value) {
        {%- for variant in e.variants() %}
        is {{ type_name }}.{{ variant|variant_type_name(ci) }} -> {
            // Add the size for the Int that specifies the variant plus the size needed for all fields
            (
                4UL
                {%- for field in variant.fields() %}
                + {{ field|allocation_size_fn }}(value.{%- call kt::field_name(field, loop.index) -%})
                {%- endfor %}
            )
        }
        {%- endfor %}
    }

    override fun write(value: {{ type_name }}, buf: ByteBuffer) {
        when(value) {
            {%- for variant in e.variants() %}
            is {{ type_name }}.{{ variant|variant_type_name(ci) }} -> {
                buf.putInt({{ loop.index }})
                {%- for field in variant.fields() %}
                {{ field|write_fn }}(value.{%- call kt::field_name(field, loop.index) -%}, buf)
                {%- endfor %}
                Unit
            }
            {%- endfor %}
        }.let { /* this makes the `when` an expression, which ensures it is exhaustive */ }
    }
}

{% endif %}
