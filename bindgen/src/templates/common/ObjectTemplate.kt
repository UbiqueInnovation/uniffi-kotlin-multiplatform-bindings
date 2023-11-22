{%- let obj = ci|get_object_definition(name) %}
{%- if self.include_once_check("ObjectRuntime.kt") %}{% include "ObjectRuntime.kt" %}{% endif %}

interface {{ type_name }}Interface {
    {% for meth in obj.methods() -%}
    {%- match meth.throws_type() -%}
    {%- when Some with (throwable) %}
    @Throws({{ throwable|error_type_name }}::class{%- if meth.is_async() -%}, CancellationException::class{%- endif -%})
    {%- else -%}
    {%- endmatch %}
    {% if meth.is_async() -%}
    suspend fun {{ meth.name()|fn_name }}({% call kt::arg_list_decl(meth) %})
    {%- else -%}
    fun {{ meth.name()|fn_name }}({% call kt::arg_list_decl(meth) %})
    {%- endif %}
    {%- match meth.return_type() -%}
    {%- when Some with (return_type) %}: {{ return_type|type_name -}}
    {%- else -%}
    {%- endmatch %}
    {% endfor %}
}

class {{ type_name }}(
    pointer: Pointer
) : FFIObject(pointer), {{ type_name }}Interface {

    {%- match obj.primary_constructor() %}
    {%- when Some with (cons) %}
    constructor({% call kt::arg_list_decl(cons) -%}) :
        this({% call kt::to_ffi_call(cons) %})
    {%- when None %}
    {%- endmatch %}

    override protected fun freeRustArcPtr() {
        rustCall { status: RustCallStatus ->
            UniFFILib.{{ obj.ffi_object_free().name() }}(this.pointer, status)
        }
    }

    {% for meth in obj.methods() -%}
    {%- match meth.throws_type() -%}
    {%- when Some with (throwable) %}
    @Throws({{ throwable|error_type_name }}::class{%- if meth.is_async() -%}, CancellationException::class{%- endif -%})
    {%- else -%}
    {%- endmatch %}
    {%- if meth.is_async() %}
    @Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
    override suspend fun {{ meth.name()|fn_name }}({%- call kt::arg_list_decl(meth) -%}){% match meth.return_type() %}{% when Some with (return_type) %} : {{ return_type|type_name }}{% when None %}{%- endmatch %} {
        return uniffiRustCallAsync(
            callWithPointer { thisPtr ->
                UniFFILib.{{ meth.ffi_func().name() }}(
                    thisPtr,
                    {% call kt::arg_list_lowered(meth) %}
                )
            },
            {{ meth|async_poll(ci) }},
            {{ meth|async_complete(ci) }},
            {{ meth|async_free(ci) }},
            // lift function
            {%- match meth.return_type() %}
            {%- when Some(return_type) %}
            { {{ return_type|lift_fn }}(it) },
            {%- when None %}
            { Unit },
            {% endmatch %}
            // Error FFI converter
            {%- match meth.throws_type() %}
            {%- when Some(e) %}
            {{ e|error_type_name }}.ErrorHandler,
            {%- when None %}
                    NullCallStatusErrorHandler,
            {%- endmatch %}
        )
    }
    {%- else -%}
    {%- match meth.return_type() -%}
    {%- when Some with (return_type) -%}
    override fun {{ meth.name()|fn_name }}({% call kt::arg_list_protocol(meth) %}): {{ return_type|type_name }} =
        callWithPointer {
            {%- call kt::to_ffi_call_with_prefix("it", meth) %}
        }.let {
            {{ return_type|lift_fn }}(it)
        }
    {%- when None -%}
    override fun {{ meth.name()|fn_name }}({% call kt::arg_list_protocol(meth) %}) =
        callWithPointer {
            {%- call kt::to_ffi_call_with_prefix("it", meth) %}
        }
    {% endmatch %}
    {% endif %}
    {% endfor %}

    {% if !obj.alternate_constructors().is_empty() -%}
    companion object {
        {% for cons in obj.alternate_constructors() -%}
        fun {{ cons.name()|fn_name }}({% call kt::arg_list_decl(cons) %}): {{ type_name }} =
            {{ type_name }}({% call kt::to_ffi_call(cons) %})
        {% endfor %}
    }
    {% endif %}
}

object {{ obj|ffi_converter_name }}: FfiConverter<{{ type_name }}, Pointer> {
    override fun lower(value: {{ type_name }}): Pointer = value.callWithPointer { it }

    override fun lift(value: Pointer): {{ type_name }} {
        return {{ type_name }}(value)
    }

    override fun read(source: NoCopySource): {{ type_name }} {
        return lift(source.readLong().toPointer())
    }

    override fun allocationSize(value: {{ type_name }}) = 8

    override fun write(value: {{ type_name }}, buf: Buffer) {
        buf.writeLong(lower(value).toLong())
    }
}