{%- if func.is_async() %}
{%- match func.throws_type() -%}
{%- when Some with (throwable) %}
@Throws({{ throwable|type_name }}::class, CancellationException::class)
{%- else -%}
{%- endmatch %}

@Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
suspend fun {{ func.name()|fn_name }}({%- call kt::arg_list_decl(func) -%}){% match func.return_type() %}{% when Some with (return_type) %} : {{ return_type|type_name }}{% when None %}{%- endmatch %} {
    // Create a new `CoroutineScope` for this operation, suspend the coroutine, and call the
    // scaffolding function, passing it one of the callback handlers from `AsyncTypes.kt`.
    //
    // Make sure to retain a reference to the callback handler to ensure that it's not GCed before
    // it's invoked
    var callbackDataHolder: {{ func.result_type().borrow()|future_callback_handler }}Data? = null
    try {
        return coroutineScope {
            val scope = this
            return@coroutineScope suspendCoroutine { continuation ->
                try {
                    val callbackData = create{{ func.result_type().borrow()|future_callback_handler }}Data(continuation)
                    callbackDataHolder = callbackData
                    rustCall { status ->
                        UniFFILib.{{ func.ffi_func().name() }}(
                        {% call kt::_arg_list_ffi_call(func) %}{% if func.arguments().len() > 0 %},{% endif %}
                        FfiConverterForeignExecutor.lower(scope),
                        callbackData.resultHandler,
                        callbackData.continuationRef,
                        status,
                        )
                    }
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }
        }
    } finally {
        callbackDataHolder?.dropHandle?.dropIt()
    }
}

{%- else %} {# if func.is_async() #}

{%- match func.throws_type() -%}
{%- when Some with (throwable) %}
@Throws({{ throwable|error_type_name }}::class)
{%- else -%}
{%- endmatch %}
{%- match func.return_type() -%}
{%- when Some with (return_type) %}

fun {{ func.name()|fn_name }}({%- call kt::arg_list_decl(func) -%}): {{ return_type|type_name }} {
    return {{ return_type|lift_fn }}({% call kt::to_ffi_call(func) %})
}

{% when None %}

fun {{ func.name()|fn_name }}({% call kt::arg_list_decl(func) %}) =
    {% call kt::to_ffi_call(func) %}
{% endmatch %}
{%- endif %}
