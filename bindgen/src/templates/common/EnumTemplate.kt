{#
// Kotlin's `enum class` construct doesn't support variants with associated data,
// but is a little nicer for consumers than its `sealed class` enum pattern.
// So, we switch here, using `enum class` for enums with no associated data
// and `sealed class` for the general case.
#}

{%- if e.is_flat() %}

enum class {{ type_name }} {
    {% for variant in e.variants() -%}
    {{ variant|variant_name }}{% if loop.last %};{% else %},{% endif %}
    {%- endfor %}
    companion object
}

internal object {{ e|ffi_converter_name }}: FfiConverterRustBuffer<{{ type_name }}> {
    override fun read(buf: NoCopySource) = try {
        {{ type_name }}.values()[buf.readInt() - 1]
    } catch (e: IndexOutOfBoundsException) {
        throw RuntimeException("invalid enum value, something is very wrong!!", e)
    }

    override fun allocationSize(value: {{ type_name }}) = 4

    override fun write(value: {{ type_name }}, buf: Buffer) {
        buf.writeInt(value.ordinal + 1)
    }
}

{% else %}

sealed class {{ type_name }}{% if contains_object_references %}: Disposable {% endif %} {
    {% for variant in e.variants() -%}
    {% if !variant.has_fields() -%}
    object {{ variant|variant_type_name(ci) }} : {{ type_name }}()
    {% else -%}
    data class {{ variant|variant_type_name(ci) }}(
        {% for field in variant.fields() -%}
        val {{ field.name()|var_name }}: {{ field|type_name(ci) }}{% if loop.last %}{% else %}, {% endif %}
        {% endfor -%}
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
        }
    }
    {% endif %}
    companion object
}

internal object {{ e|ffi_converter_name }} : FfiConverterRustBuffer<{{ type_name }}>{
    override fun read(buf: NoCopySource): {{ type_name }} {
        return when(buf.readInt()) {
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
            (
                4
                {%- for field in variant.fields() %}
                + {{ field|allocation_size_fn }}(value.{{ field.name()|var_name }})
                {%- endfor %}
            )
        }
        {%- endfor %}
    }

    override fun write(value: {{ type_name }}, buf: Buffer) {
        when(value) {
            {%- for variant in e.variants() %}
            is {{ type_name }}.{{ variant|variant_type_name(ci) }} -> {
                buf.writeInt({{ loop.index }})
                {%- for field in variant.fields() %}
                {{ field|write_fn }}(value.{{ field.name()|var_name }}, buf)
                {%- endfor %}
                Unit
            }
            {%- endfor %}
        }
    }
}

{% endif %}
