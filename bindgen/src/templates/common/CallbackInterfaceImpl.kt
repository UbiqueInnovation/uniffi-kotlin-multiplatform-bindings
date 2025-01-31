
{% if self.include_once_check("CallbackInterfaceRuntime.kt") %}{% include "CallbackInterfaceRuntime.kt" %}{% endif %}

{%- let trait_impl=format!("uniffiCallbackInterface{}", name) %}

// Put the implementation in an object so we don't pollute the top-level namespace
internal object {{ trait_impl }} {
    {%- for (ffi_callback, meth) in vtable_methods.iter() %}
     var {{ meth.name()|var_name }}: Any = create{{ ffi_callback.name()|ffi_callback_name }}Callback()
    {%- endfor %}

     val uniffiFree = create{{ "CallbackInterfaceFree"|ffi_callback_name }}{{name}}Callback()

     var vtable = Uniffi{{ vtable|ffi_type_name }}Factory.create(
        {%- for (ffi_callback, meth) in vtable_methods.iter() %}
        {{ meth.name()|var_name() }},
        {%- endfor %}
        uniffiFree,
    )

    // Registers the foreign callback with the Rust side.
    // This method is generated for each callback interface.
     fun register(lib: UniffiLib) {
        lib.{{ ffi_init_callback.name() }}(vtable)
    }
}
{%- for (ffi_callback, meth) in vtable_methods.iter() %}
    expect fun create{{ ffi_callback.name()|ffi_callback_name }}Callback() : Any
{%- endfor %}

internal expect fun create{{ "CallbackInterfaceFree"|ffi_callback_name }}{{name}}Callback() : Any
internal expect  open class Uniffi{{ vtable|ffi_type_name }}Factory {
        companion object {
            fun create(
            {%- for (ffi_callback, meth) in vtable_methods.iter() %}
                    {{ meth.name()|var_name() }} : Any,
                    {%- endfor %}
                    uniffiFree : Any,
            ) : {{ vtable|ffi_type_name }}
        }
}
