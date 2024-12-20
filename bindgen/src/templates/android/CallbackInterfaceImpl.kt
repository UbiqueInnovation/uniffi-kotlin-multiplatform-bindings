
{%- let trait_impl=format!("uniffiCallbackInterface{}", name) %}

// Put the implementation in an object so we don't pollute the top-level namespace
{%- for (ffi_callback, meth) in vtable_methods.iter() %}
// Method for createing native callbacks
{%- if !ffi_callback.name().contains("ForeignFuture") %}
actual fun create{{ ffi_callback.name()|ffi_callback_name }}Callback() : Any {
    return object: com.sun.jna.Callback {
         fun callback(
            {%- for arg in ffi_callback.arguments() -%}
            {{ arg.name().borrow()|var_name }}: {{ arg.type_().borrow()|ffi_type_name_by_value }},
            {%- endfor -%}
            {%- if ffi_callback.has_rust_call_status_arg() -%}
            uniffiCallStatus: UniffiRustCallStatus,
            {%- endif -%}
            {%- match ffi_callback.return_type() %}
            {%- when Some(return_type) %}: {{ return_type|ffi_type_name_by_value }},
            {%- when None %}
            {%- endmatch %}
            ) {
            val uniffiObj = {{ ffi_converter_name }}.handleMap.get(uniffiHandle)
            val makeCall = {% if meth.is_async() %}suspend {% endif %}{ ->
                uniffiObj.{{ meth.name()|fn_name() }}(
                    {%- for arg in meth.arguments() %}
                    {{ arg|lift_fn }}({{ arg.name()|var_name }}!!),
                    {%- endfor %}
                )
            }
            {%- if !meth.is_async() %}

            {%- match meth.return_type() %}
            {%- when Some(return_type) %}
            val writeReturn = { value: {{ return_type|type_name(ci) }} -> uniffiOutReturn.setValue({{ return_type|lower_fn }}(value)) }
            {%- when None %}
            val writeReturn = { _: Unit -> Unit }
            {%- endmatch %}

            {%- match meth.throws_type() %}
            {%- when None %}
            uniffiTraitInterfaceCall(uniffiCallStatus, makeCall, writeReturn)
            {%- when Some(error_type) %}
            uniffiTraitInterfaceCallWithError(
                uniffiCallStatus,
                makeCall,
                writeReturn,
                { e: {{error_type|type_name(ci) }} -> {{ error_type|lower_fn }}(e) }
            )
            {%- endmatch %}

            {%- else %}
            val uniffiHandleSuccess = { {% if meth.return_type().is_some() %}returnValue{% else %}_{% endif %}: {% match meth.return_type() %}{%- when Some(return_type) %}{{ return_type|type_name(ci) }}{%- when None %}Unit{% endmatch %} ->
                val uniffiResult = {{ meth.foreign_future_ffi_result_struct().name()|ffi_struct_name }}UniffiByValue(
                    {%- match meth.return_type() %}
                    {%- when Some(return_type) %}
                    {{ return_type|lower_fn }}(returnValue),
                    {%- when None %}
                    {%- endmatch %}
                    UniffiRustCallStatusHelper.allocValue()
                )
                uniffiResult.write()
                uniffiFutureCallback.callback(uniffiCallbackData, uniffiResult)
            }
            val uniffiHandleError = { callStatus: UniffiRustCallStatusByValue ->
                uniffiFutureCallback.callback(
                    uniffiCallbackData,
                    {{ meth.foreign_future_ffi_result_struct().name()|ffi_struct_name }}UniffiByValue(
                        {%- match meth.return_type() %}
                        {%- when Some(return_type) %}
                        {{ return_type.into()|ffi_default_value }},
                        {%- when None %}
                        {%- endmatch %}
                        callStatus,
                    ),
                )
            }

            uniffiOutReturn.uniffiSetValue(
                {%- match meth.throws_type() %}
                {%- when None %}
                uniffiTraitInterfaceCallAsync(
                    makeCall,
                    uniffiHandleSuccess,
                    uniffiHandleError
                )
                {%- when Some(error_type) %}
                uniffiTraitInterfaceCallAsyncWithError(
                    makeCall,
                    uniffiHandleSuccess,
                    uniffiHandleError,
                    { e: {{error_type|type_name(ci) }} -> {{ error_type|lower_fn }}(e) }
                )
                {%- endmatch %}
            )
        {%- endif %}
        }
    }
}
{%- endif %}
{%- endfor %}

actual fun create{{ "CallbackInterfaceFree"|ffi_callback_name }}{{name}}Callback() : Any {
    return object: Callback {
        fun callback(handle: Long) {
            {{ ffi_converter_name }}.handleMap.remove(handle)
        }
    }
}

actual internal open class Uniffi{{ vtable|ffi_type_name }}Factory {
        actual companion object {
            actual fun create(
            {%- for (ffi_callback, meth) in vtable_methods.iter() %}
                    {{ meth.name()|var_name() }} : Any,
                    {%- endfor %}
                    uniffiFree : Any,
            ) : {{ vtable|ffi_type_name }} {

                return {{ vtable|ffi_type_name }}(
                    {%- for (ffi_callback, meth) in vtable_methods.iter() %}
                      {{ meth.name()|var_name() }},
                      {%- endfor %}
                      uniffiFree,
                    )
            }
        }
}
