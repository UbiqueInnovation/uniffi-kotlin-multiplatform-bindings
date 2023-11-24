@Synchronized
private fun findLibraryName(): kotlin.String {
    val componentName = "{{ ci.namespace() }}"
    val libOverride = System.getProperty("uniffi.component.$componentName.libraryOverride")
    if (libOverride != null) {
        return libOverride
    }
    return "{{ config.cdylib_name() }}"
}

actual internal object UniFFILib : com.sun.jna.Library {
    init {
        com.sun.jna.Native.register(UniFFILib::class.java, findLibraryName())
        {% let initialization_fns = self.initialization_fns() %}
        {%- if !initialization_fns.is_empty() -%}
        {% for fn in initialization_fns -%}
        {{ fn }}(this)
        {% endfor -%}
        {% endif %}
    }

    {% for func in ci|iter_ffi_function_definitions -%}
    @JvmName("{{ func.name() }}")
    actual external fun {{ func.name() }}(
    {%- call kt::arg_list_ffi_decl(func) %}
    ){%- match func.return_type() -%}{%- when Some with (type_) %}: {{ type_.borrow()|ffi_type_name }}{% when None %}: Unit{% endmatch %}

    {% endfor %}
}
