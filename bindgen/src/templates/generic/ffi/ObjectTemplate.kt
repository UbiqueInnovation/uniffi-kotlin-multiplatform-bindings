
{%- let obj = ci|get_object_definition(name) %}
{%- let (interface_name, impl_class_name) = obj|object_names(ci) %}
{%- let methods = obj.methods() %}
{%- let interface_docstring = obj.docstring() %}
{%- let is_error = ci.is_name_used_as_error(name) %}
{%- let ffi_converter_name = obj|ffi_converter_name %}

{%- call kt::docstring(obj, 0) %}{% endcall %}
{% if (is_error) %}
actual open class {{ impl_class_name }} : kotlin.Exception, Disposable, {{ interface_name }} {
{% else -%}
actual open class {{ impl_class_name }}: Disposable, {{ interface_name }} {
{%- endif %}

    actual constructor(uniffiWithHandle: UniffiWithHandle, handle: Long) {
        this.handle = handle
        this.cleanable = UniffiLib.CLEANER.register(this, UniffiCleanAction(handle))
    }

    /**
     * This constructor can be used to instantiate a fake object. Only used for tests. Any
     * attempt to actually use an object constructed this way will fail as there is no
     * connected Rust object.
     */
    @Suppress("UNUSED_PARAMETER")
    actual constructor(noPointer: NoPointer) {
        this.handle = null
        this.cleanable = UniffiLib.CLEANER.register(this, UniffiCleanAction(handle))
    }

    {%- match obj.primary_constructor() %}
    {%- when Some(cons) %}
    {%-     if cons.is_async() %}
    // Note no constructor generated for this object as it is async.
    {%-     else %}
    {%- call kt::docstring(cons, 4) %}{% endcall %}
    actual constructor({% call kt::arg_list(cons, false) %}{% endcall -%}) :
        this(UniffiWithHandle, {% call kt::to_ffi_call(cons) %}{% endcall %})
    {%-     endif %}
    {%- when None %}
    {%- endmatch %}

    protected val handle: Long?
    protected val cleanable: UniffiCleaner.Cleanable

    private val wasDestroyed: kotlinx.atomicfu.AtomicBoolean = kotlinx.atomicfu.atomic(false)
    private val callCounter: kotlinx.atomicfu.AtomicLong = kotlinx.atomicfu.atomic(1L)

    private val lock = kotlinx.atomicfu.locks.ReentrantLock()

    private fun <T> synchronized(block: () -> T): T {
        lock.lock()
        try {
            return block()
        } finally {
            lock.unlock()
        }
    }

    actual override fun destroy() {
        // Only allow a single call to this method.
        // TODO: maybe we should log a warning if called more than once?
        if (this.wasDestroyed.compareAndSet(false, true)) {
            // This decrement always matches the initial count of 1 given at creation time.
            if (this.callCounter.decrementAndGet() == 0L) {
                cleanable.clean()
            }
        }
    }

    actual override fun close() {
        synchronized { this.destroy() }
    }

    internal actual inline fun <R> callWithHandle(block: (handle: Long) -> R): R {
        // Check and increment the call counter, to keep the object alive.
        // This needs a compare-and-set retry loop in case of concurrent updates.
        do {
            val c = this.callCounter.value
            if (c == 0L) {
                throw IllegalStateException("${this::class::simpleName} object has already been destroyed")
            }
            if (c == Long.MAX_VALUE) {
                throw IllegalStateException("${this::class::simpleName} call counter would overflow")
            }
        } while (! this.callCounter.compareAndSet(c, c + 1L))
        // Now we can safely do the method call without the handle being freed concurrently.
        try {
            return block(this.uniffiCloneHandle())
        } finally {
            // This decrement always matches the increment we performed above.
            if (this.callCounter.decrementAndGet() == 0L) {
                cleanable.clean()
            }
        }
    }

    // Use a static inner class instead of a closure so as not to accidentally
    // capture `this` as part of the cleanable's action.
    private class UniffiCleanAction(private val handle: Long?) : Runnable {
        override fun run() {
            handle?.let { h ->
                uniffiRustCall { status ->
                    UniffiLib.INSTANCE.{{ obj.ffi_object_free().name() }}(h, status)!!
                }
            }
        }
    }

    actual fun uniffiCloneHandle(): Long {
        return uniffiRustCall() { status ->
            UniffiLib.INSTANCE.{{ obj.ffi_object_clone().name() }}(handle!!, status)!!
        }
    }

    {% for meth in obj.methods() -%}
    {%- call kt::func_decl_with_body("actual override", meth, 4) %}{% endcall %}
    {% endfor %}

    {%- for tm in obj.uniffi_traits() %}
    {%-     match tm %}
    {%         when UniffiTrait::Display { fmt } %}
    actual override fun toString(): String {
        return {{ fmt.return_type().unwrap()|lift_fn }}({% call kt::to_ffi_call(fmt) %}{% endcall %})
    }
    {%         when UniffiTrait::Eq { eq, ne } %}
    {# only equals used #}
    actual override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is {{ impl_class_name}}) return false
        return {{ eq.return_type().unwrap()|lift_fn }}({% call kt::to_ffi_call(eq) %}{% endcall %})
    }
    {%         when UniffiTrait::Hash { hash } %}
    actual override fun hashCode(): Int {
        return {{ hash.return_type().unwrap()|lift_fn }}({%- call kt::to_ffi_call(hash) %}{% endcall %}).toInt()
    }
    {%-         else %}
    {%-     endmatch %}
    {%- endfor %}

    {# XXX - "companion object" confusion? How to have alternate constructors *and* be an error? #}
    {% if !obj.alternate_constructors().is_empty() -%}
    actual companion object {
        {% for cons in obj.alternate_constructors() -%}
        {% call kt::func_decl_with_body("actual", cons, 4) %}{% endcall %}
        {% endfor %}
    }
    {% else %}
    actual companion object
    {% endif %}
}

{% if is_error %}
object {{ impl_class_name }}ErrorHandler : UniffiRustCallStatusErrorHandler<{{ impl_class_name }}> {
    override fun lift(errorBuf: RustBufferByValue): {{ impl_class_name }} {
        // Due to some mismatches in the ffi converter mechanisms, errors are a RustBuffer.
        val bb = errorBuf.asByteBuffer()
        if (bb == null) {
            throw InternalException("?")
        }
        return {{ ffi_converter_name }}.read(bb)
    }
}
{% endif %}

{% macro converter_type(obj) %}
{% if obj.has_callback_interface() %}
{{ interface_name }}
{% else %}
{{ impl_class_name }}
{% endif %}
{% endmacro %}

public object {{ ffi_converter_name }}: FfiConverter<{%- call converter_type(obj) %}{% endcall -%}, Long> {
    {%- if obj.has_callback_interface() %}
    internal val handleMap = UniffiHandleMap<{%- call converter_type(obj) %}{% endcall -%}>()
    {%- endif %}

    override fun lower(value: {%- call converter_type(obj) %}{% endcall -%}): Long {
        {%- if obj.has_callback_interface() %}
        // Since uniffi 0.30 a trait interface handle can originate on either side of the
        // FFI, so which side this value came from decides how it is lowered.
        if (value is {{ impl_class_name }}) {
            // Rust-implemented object: clone its handle and hand that over.
            return value.uniffiCloneHandle()
        } else {
            // Kotlin implementation: register it and hand over a vtable handle.
            return handleMap.insert(value)
        }
        {%- else %}
        val obj = value as {{ impl_class_name }}
        return obj.uniffiCloneHandle()
        {%- endif %}
        }


    override fun lift(value: Long): {%- call converter_type(obj) %}{% endcall -%} {
        {%- if obj.has_callback_interface() %}
        // Foreign handles always have the lowest bit set; Rust handles never do.
        if ((value and 1L) == 0L) {
            return {{ impl_class_name }}(UniffiWithHandle, value)
        } else {
            // Our own object coming back to us. Lifting takes ownership of the handle,
            // so drop the handle map entry rather than leaking it.
            return handleMap.remove(value)
        }
        {%- else %}
        return {{ impl_class_name }}(UniffiWithHandle, value)
        {%- endif %}
    }

    override fun read(buf: ByteBuffer): {%- call converter_type(obj) %}{% endcall -%} {
        // The Rust code always writes handles as 8 bytes, and will
        // fail to compile if they don't fit.
        return lift(buf.getLong())
    }

    override fun allocationSize(value: {%- call converter_type(obj) %}{% endcall -%}) = 8UL

    override fun write(value: {%- call converter_type(obj) %}{% endcall -%}, buf: ByteBuffer) {
        // The Rust code always expects handles written as 8 bytes,
        // and will fail to compile if they don't fit.
        buf.putLong(lower(value))
    }
}
