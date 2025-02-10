{% if self.include_once_check("ffi/CallbackInterfaceRuntime.kt") %}{% include "ffi/CallbackInterfaceRuntime.kt" %}{% endif %}
{{ self.add_import("kotlinx.cinterop.invoke") }}

{%- let trait_impl=format!("uniffiCallbackInterface{}", name) %}

// Put the implementation in an object so we don't pollute the top-level namespace
internal object {{ trait_impl }} {
    {%- for (ffi_callback, meth) in vtable_methods.iter() %}
    internal fun {{ meth.name()|var_name }}({%- call kt::arg_list_ffi_decl(ffi_callback) -%})
    {%- if let Some(return_type) = ffi_callback.return_type() %}
        : {{ return_type|ffi_type_name_by_value }},
    {%- endif %} {
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
            val uniffiResult = cValue<{{ ci.namespace() }}.cinterop.{{ meth.foreign_future_ffi_result_struct().name()|ffi_struct_name }}> {
                {%- if let Some(return_type) = meth.return_type() %}
                {%- match return_type.into() %}
                {%- when FfiType::RustBuffer(_) %}
                {{ return_type|lower_fn }}(returnValue).write(this.returnValue.rawPtr)
                {%- when FfiType::RustCallStatus %}
                {{ return_type|lower_fn }}(returnValue).write(this.returnValue.rawPtr)
                {%- when _ %}
                this.returnValue = {{ return_type|lower_fn }}(returnValue)
                {%- endmatch %}
                {%- endif %}
                UniffiRustCallStatusHelper.allocValue().write(this.callStatus.rawPtr)
            }
            uniffiFutureCallback.invoke(uniffiCallbackData, uniffiResult)
        }
        val uniffiHandleError = { callStatus: UniffiRustCallStatusByValue ->
            val uniffiResult = cValue<{{ ci.namespace() }}.cinterop.{{ meth.foreign_future_ffi_result_struct().name()|ffi_struct_name }}> {
                {%- if let Some(return_type) = meth.return_type() %}
                {%- match return_type.into() %}
                {%- when FfiType::RustBuffer(_) %}
                {{ return_type.into()|ffi_default_value }}.write(this.returnValue.rawPtr)
                {%- when FfiType::RustCallStatus %}
                {{ return_type.into()|ffi_default_value }}.write(this.returnValue.rawPtr)
                {%- when _ %}
                this.returnValue = {{ return_type.into()|ffi_default_value }}
                {%- endmatch %}
                {%- endif %}
                callStatus.write(this.callStatus.rawPtr)
            }
            uniffiFutureCallback.invoke(uniffiCallbackData, uniffiResult)
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
    {%- endfor %}
    internal fun uniffiFree(handle: Long) {
        {{ ffi_converter_name }}.handleMap.remove(handle)
    }

    internal val vtable = nativeHeap.alloc<{{ci.namespace()}}.cinterop.{{ vtable|ffi_type_name }}> {
        {%- for (ffi_callback, meth) in vtable_methods.iter() %}
        this.{{ meth.name()|var_name }} = staticCFunction {
            {%- for arg in ffi_callback.arguments() -%}
            {{ arg.name().borrow()|var_name }}: 
            {%- if arg.type_().borrow()|is_pointer_type -%}
                GenericPointer
            {%- else -%}
                {{ arg.type_().borrow()|ffi_type_name_by_value }}
            {%- endif -%},

            {%- endfor -%}
            {%- if ffi_callback.has_rust_call_status_arg() -%}
            uniffiCallStatus: UniffiRustCallStatus,
            {%- endif -%} ->
            {{ trait_impl }}.{{ meth.name()|var_name() }}(
                {%- for arg in ffi_callback.arguments() -%}
                {{ arg.name().borrow()|var_name }}
                {%- if arg.type_().borrow()|is_pointer_type -%}
                    .let{ Pointer(it) }
                {%- endif -%},
                {%- endfor -%}
                {%- if ffi_callback.has_rust_call_status_arg() -%}
                uniffiCallStatus,
                {%- endif -%}
            )
        } as {{ ci.namespace() }}.cinterop.{{ ffi_callback.name()|ffi_callback_name }}
        {%- endfor %}
        this.uniffiFree = staticCFunction { handle: Long ->
            {{ trait_impl }}.uniffiFree(handle)
        } as {{ ci.namespace() }}.cinterop.{{ "CallbackInterfaceFree"|ffi_callback_name }}
    }.ptr

    internal fun register(lib: UniffiLib) {
        lib.{{ ffi_init_callback.name() }}(vtable)
    }
}