{%- let cbi = ci|get_callback_interface_definition(name) %}
{%- let foreign_callback_name = format!("ForeignCallback{}", name) %}

{% include "CallbackInterfaceImpl.kt" %}

actual fun {{ foreign_callback_name }}.toForeignCallback() : ForeignCallback =
    NativeCallback(this::invoke)
