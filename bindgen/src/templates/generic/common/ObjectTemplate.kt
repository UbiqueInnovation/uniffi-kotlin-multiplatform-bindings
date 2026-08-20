{%- let obj = ci|get_object_definition(name) %}
{%- let (interface_name, impl_class_name) = obj|object_names(ci) %}
{%- let methods = obj.methods() %}
{%- let interface_docstring = obj.docstring() %}
{%- let is_error = ci.is_name_used_as_error(name) %}
{%- let ffi_converter_name = obj|ffi_converter_name %}
{%- let uniffi_trait_methods = obj.uniffi_trait_methods() %}
{%- let comparable = uniffi_trait_methods.ord_cmp.is_some() %}

{%- include "Interface.kt" %}

{%- call kt::docstring(obj, 0) %}{% endcall %}
{% if (is_error) %}
expect open class {{ impl_class_name }} : kotlin.Exception, Disposable, {{ interface_name }}{% if comparable %}, Comparable<{{ impl_class_name }}>{% endif %} {
{% else -%}
expect open class {{ impl_class_name }}: Disposable, {{ interface_name }}{% if comparable %}, Comparable<{{ impl_class_name }}>{% endif %} {
{%- endif %}
    constructor(uniffiWithHandle: UniffiWithHandle, handle: Long)

    /**
     * This constructor can be used to instantiate a fake object. Only used for tests. Any
     * attempt to actually use an object constructed this way will fail as there is no
     * connected Rust object.
     */
    constructor(noHandle: NoHandle)

    {%- match obj.primary_constructor() %}
    {%- when Some(cons) %}
    {%-     if cons.is_async() %}
    // Note no constructor generated for this object as it is async.
    {%-     else %}
    {%- call kt::docstring(cons, 4) %}{% endcall %}
    constructor({% call kt::arg_list(cons, true) %}{% endcall -%})
    {%-     endif %}
    {%- when None %}
    {%- endmatch %}

    /**
     * Whether this object has been destroyed and its reference on the Rust side is gone.
     *
     * Once this is `true` every method call on the object throws, and `destroy()` is a no-op.
     */
    val uniffiIsDestroyed: Boolean

    override fun destroy()
    override fun close()

    internal inline fun <R> callWithHandle(block: (handle: Long) -> R): R
    fun uniffiCloneHandle(): Long

    {% for meth in obj.methods() -%}
    {%- call kt::func_decl("override", meth, 4) %}{% endcall %}
    {% endfor %}

    {#- We have 2 display traits, kotlin has 1. Prefer `Display` but use `Debug` otherwise. #}
    {%- if uniffi_trait_methods.display_fmt.is_some() || uniffi_trait_methods.debug_fmt.is_some() %}
    override fun toString(): String
    {%- endif %}
    {%- if uniffi_trait_methods.eq_eq.is_some() %}
    {#- only equals used #}
    override fun equals(other: Any?): Boolean
    {%- endif %}
    {%- if uniffi_trait_methods.hash_hash.is_some() %}
    override fun hashCode(): Int
    {%- endif %}
    {%- if uniffi_trait_methods.ord_cmp.is_some() %}
    override fun compareTo(other: {{ impl_class_name }}): Int
    {%- endif %}

    {# XXX - "companion object" confusion? How to have alternate constructors *and* be an error? #}
    {% if !obj.alternate_constructors().is_empty() -%}
    companion object {
        {% for cons in obj.alternate_constructors() -%}
        {% call kt::func_decl("", cons, 4) %}{% endcall %}
        {% endfor %}
    }
    {% else %}
    companion object
    {% endif %}
}
