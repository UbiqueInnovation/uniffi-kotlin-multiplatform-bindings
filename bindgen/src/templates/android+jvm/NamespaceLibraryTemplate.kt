
// Define FFI callback types
{% let ffi_definitions = self.ffi_definitions_no_builtins() %}

{%- for def in ffi_definitions %}
{%- match def %}
{%- when FfiDefinition::CallbackFunction(callback) %}
internal interface {{ callback.name()|ffi_callback_name }}: com.sun.jna.Callback {
    fun callback(
        {%- for arg in callback.arguments() -%}
        {{ arg.name().borrow()|var_name }}: {{ arg.type_().borrow()|ffi_type_name_by_value }},
        {%- endfor -%}
        {%- if callback.has_rust_call_status_arg() -%}
        uniffiCallStatus: UniffiRustCallStatus,
        {%- endif -%}
    )
    {%- match callback.return_type() %}
    {%- when Some(return_type) %}: {{ return_type|ffi_type_name_by_value }}
    {%- when None %}
    {%- endmatch %}
}
{%- when FfiDefinition::Struct(ffi_struct) %}
@Structure.FieldOrder({% for field in ffi_struct.fields() %}"{{ field.name()|var_name_raw }}"{% if !loop.last %}, {% endif %}{% endfor %})
internal open class {{ ffi_struct.name()|ffi_struct_name }}Struct(
    {%- for field in ffi_struct.fields() %}
    {{ field.name()|var_name }}: {{ field.type_().borrow()|ffi_type_name_for_ffi_struct }},
    {%- endfor %}
) : com.sun.jna.Structure() {
    {% for field in ffi_struct.fields() %}
    @JvmField internal var {{ field.name()|var_name }}: {{ (field.type_().borrow()|ffi_type_name_for_ffi_struct_inner) }} = {{ field.name()|var_name }} as {{ field.type_().borrow()|ffi_type_name_for_ffi_struct_inner }}
    {% endfor %}

    constructor(): this(
        {% for field in ffi_struct.fields() %}
        {{ field.name()|var_name }} = {{ field.type_()|ffi_default_value }},
        {% endfor %}
    )

    internal class UniffiByValue(
        {%- for field in ffi_struct.fields() %}
        {{ field.name()|var_name }}: {{ field.type_().borrow()|ffi_type_name_for_ffi_struct }},
        {%- endfor %}
    ): {{ ffi_struct.name()|ffi_struct_name }}({%- for field in ffi_struct.fields() %}{{ field.name()|var_name }}, {%- endfor %}), Structure.ByValue
}

internal typealias {{ ffi_struct.name()|ffi_struct_name }} = {{ ffi_struct.name()|ffi_struct_name }}Struct
{% for field in ffi_struct.fields() %}
internal var {{ ffi_struct.name()|ffi_struct_name }}.{{ field.name()|var_name }}: {{ field.type_().borrow()|ffi_type_name_for_ffi_struct }}
    get() = this.{{ field.name()|var_name }}
    set(value) { this.{{ field.name()|var_name }} = value as {{ field.type_().borrow()|ffi_type_name_for_ffi_struct_inner }} }
{% endfor %}

internal fun {{ ffi_struct.name()|ffi_struct_name }}.uniffiSetValue(other: {{ ffi_struct.name()|ffi_struct_name }}) {
    {%- for field in ffi_struct.fields() %}
    {{ field.name()|var_name }} = other.{{ field.name()|var_name }}
    {%- endfor %}
}
internal fun {{ ffi_struct.name()|ffi_struct_name }}.uniffiSetValue(other: {{ ffi_struct.name()|ffi_struct_name }}UniffiByValue) {
    {%- for field in ffi_struct.fields() %}
    {{ field.name()|var_name }} = other.{{ field.name()|var_name }}
    {%- endfor %}
}

internal typealias {{ ffi_struct.name()|ffi_struct_name }}UniffiByValue = {{ ffi_struct.name()|ffi_struct_name }}Struct.UniffiByValue
{% for field in ffi_struct.fields() %}
internal val {{ ffi_struct.name()|ffi_struct_name }}UniffiByValue.{{ field.name()|var_name }}: {{ field.type_().borrow()|ffi_type_name_for_ffi_struct }}
    get() = this.{{ field.name()|var_name }}
{% endfor %}

{%- when FfiDefinition::Function(_) %}
{# functions are handled below #}
{%- endmatch %}
{%- endfor %}

@Synchronized
private fun findLibraryName(componentName: String): String {
    val libOverride = System.getProperty("uniffi.component.$componentName.libraryOverride")
    if (libOverride != null) {
        return libOverride
    }
    return "{{ config.cdylib_name() }}"
}

private inline fun <reified Lib : Library> loadIndirect(
    componentName: String
): Lib {
    return Native.load<Lib>(findLibraryName(componentName), Lib::class.java)
}

// A JNA Library to expose the extern-C FFI definitions.
// This is an implementation detail which will be called internally by the public API.

internal interface UniffiLib : Library {
    companion object {
        internal val INSTANCE: UniffiLib by lazy {
            loadIndirect<UniffiLib>(componentName = "{{ ci.namespace() }}")
            .also { lib: UniffiLib ->
                uniffiCheckContractApiVersion(lib)
                uniffiCheckApiChecksums(lib)
                {% for fn in self.initialization_fns() -%}
                {{ fn }}(lib)
                {% endfor -%}
            }
        }
        {% if ci.contains_object_types() %}
        // The Cleaner for the whole library
        internal val CLEANER: UniffiCleaner by lazy {
            UniffiCleaner.create()
        }
        {%- endif %}
    }

    {% for func in ci.iter_ffi_function_definitions() -%}
    fun {{ func.name() }}(
        {%- call kt::arg_list_ffi_decl_for_ffi_function(func) %}
    ): {% match func.return_type() %}{% when Some with (return_type) %}{{ return_type.borrow()|ffi_type_name_for_ffi_function }}{% when None %}Unit{% endmatch %}
    {% endfor %}
}

private fun uniffiCheckContractApiVersion(lib: UniffiLib) {
    // Get the bindings contract version from our ComponentInterface
    val bindings_contract_version = {{ ci.uniffi_contract_version() }}
    // Get the scaffolding contract version by calling the into the dylib
    val scaffolding_contract_version = lib.{{ ci.ffi_uniffi_contract_version().name() }}()
    if (bindings_contract_version != scaffolding_contract_version) {
        throw RuntimeException("UniFFI contract version mismatch: try cleaning and rebuilding your project")
    }
}

@Suppress("UNUSED_PARAMETER")
private fun uniffiCheckApiChecksums(lib: UniffiLib) {
    {%- for (name, expected_checksum) in ci.iter_checksums() %}
    if (lib.{{ name }}() != {{ expected_checksum }}.toShort()) {
        throw RuntimeException("UniFFI API checksum mismatch: try cleaning and rebuilding your project")
    }
    {%- endfor %}
}
