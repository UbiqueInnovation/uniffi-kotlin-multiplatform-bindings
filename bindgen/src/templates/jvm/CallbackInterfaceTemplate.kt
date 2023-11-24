{%- let cbi = ci|get_callback_interface_definition(name) %}
{%- let foreign_callback_name = format!("ForeignCallback{}", canonical_type_name) %}

{% if self.include_once_check("CallbackInterfaceRuntime.kt") %}{% include "CallbackInterfaceRuntime.kt" %}{% endif %}

internal actual fun {{ foreign_callback_name }}.toForeignCallback() : ForeignCallback =
    NativeCallback(this::callback)
