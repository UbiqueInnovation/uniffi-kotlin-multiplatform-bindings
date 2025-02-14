{%- let obj = ci|get_object_definition(name) %}
{%- let (interface_name, impl_class_name) = obj|object_names(ci) %}
{%- let methods = obj.methods() %}
{%- let interface_docstring = obj.docstring() %}
{%- let is_error = ci.is_name_used_as_error(name) %}
{%- let ffi_converter_name = obj|ffi_converter_name %}

{%- include "Interface.kt" %}

{%- call kt::docstring(obj, 0) %}
{% if (is_error) %}
expect open class {{ impl_class_name }} : kotlin.Exception, Disposable, {{ interface_name }} {
{% else -%}
expect open class {{ impl_class_name }}: Disposable, {{ interface_name }} {
{%- endif %}
    constructor(pointer: Pointer)

    /**
     * This constructor can be used to instantiate a fake object. Only used for tests. Any
     * attempt to actually use an object constructed this way will fail as there is no
     * connected Rust object.
     */
    constructor(noPointer: NoPointer)

    {%- match obj.primary_constructor() %}
    {%- when Some(cons) %}
    {%-     if cons.is_async() %}
    // Note no constructor generated for this object as it is async.
    {%-     else %}
    {%- call kt::docstring(cons, 4) %}
    constructor({% call kt::arg_list(cons, true) -%})
    {%-     endif %}
    {%- when None %}
    {%- endmatch %}

    override fun destroy()
    override fun close()

    internal inline fun <R> callWithPointer(block: (ptr: Pointer) -> R): R
    fun uniffiClonePointer(): Pointer

    {% for meth in obj.methods() -%}
    {%- call kt::func_decl("override", meth, 4) %}
    {% endfor %}

    {%- for tm in obj.uniffi_traits() %}
    {%-     match tm %}
    {%         when UniffiTrait::Display { fmt } %}
    override fun toString(): String
    {%         when UniffiTrait::Eq { eq, ne } %}
    {# only equals used #}
    override fun equals(other: Any?): Boolean
    {%         when UniffiTrait::Hash { hash } %}
    override fun hashCode(): Int
    {%-         else %}
    {%-     endmatch %}
    {%- endfor %}

    {# XXX - "companion object" confusion? How to have alternate constructors *and* be an error? #}
    {% if !obj.alternate_constructors().is_empty() -%}
    companion object {
        {% for cons in obj.alternate_constructors() -%}
        {% call kt::func_decl("", cons, 4) %}
        {% endfor %}
    }
    {% else %}
    companion object
    {% endif %}
}
