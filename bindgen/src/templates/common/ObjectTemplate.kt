
{%- if self.include_once_check("interface-support") %}
    {%- include "ObjectCleanerHelper.kt" %}
{%- endif %}

{%- let obj = ci|get_object_definition(name) %}
{%- let (interface_name, impl_class_name) = obj|object_names(ci) %}
{%- let methods = obj.methods() %}
{%- let interface_docstring = obj.docstring() %}
{%- let is_error = ci.is_name_used_as_error(name) %}
{%- let ffi_converter_name = obj|ffi_converter_name %}

{%- include "Interface.kt" %}

{%- call kt::docstring(obj, 0) %}
{% if (is_error) %}
open class {{ impl_class_name }} : kotlin.Exception, Disposable, AutoCloseable, {{ interface_name }} {
{% else -%}
open class {{ impl_class_name }}: Disposable, AutoCloseable, {{ interface_name }} {
{%- endif %}

    constructor(pointer: Pointer) {
        this.pointer = pointer
        this.cleanable = UniffiLib.CLEANER.register(this, UniffiCleanAction(pointer))
    }

    /**
     * This constructor can be used to instantiate a fake object. Only used for tests. Any
     * attempt to actually use an object constructed this way will fail as there is no
     * connected Rust object.
     */
    @Suppress("UNUSED_PARAMETER")
    constructor(noPointer: NoPointer) {
        this.pointer = null
        this.cleanable = UniffiLib.CLEANER.register(this, UniffiCleanAction(pointer))
    }

    {%- match obj.primary_constructor() %}
    {%- when Some(cons) %}
    {%-     if cons.is_async() %}
    // Note no constructor generated for this object as it is async.
    {%-     else %}
    {%- call kt::docstring(cons, 4) %}
    constructor({% call kt::arg_list(cons, true) -%}) :
        this({% call kt::to_ffi_call(cons) %})
    {%-     endif %}
    {%- when None %}
    {%- endmatch %}

    protected val pointer: Pointer?
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

    override fun destroy() {
        // Only allow a single call to this method.
        // TODO: maybe we should log a warning if called more than once?
        if (this.wasDestroyed.compareAndSet(false, true)) {
            // This decrement always matches the initial count of 1 given at creation time.
            if (this.callCounter.decrementAndGet() == 0L) {
                cleanable.clean()
            }
        }
    }

    override fun close() {
        synchronized { this.destroy() }
    }

    internal inline fun <R> callWithPointer(block: (ptr: Pointer) -> R): R {
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
        // Now we can safely do the method call without the pointer being freed concurrently.
        try {
            return block(this.uniffiClonePointer())
        } finally {
            // This decrement always matches the increment we performed above.
            if (this.callCounter.decrementAndGet() == 0L) {
                cleanable.clean()
            }
        }
    }

    // Use a static inner class instead of a closure so as not to accidentally
    // capture `this` as part of the cleanable's action.
    private class UniffiCleanAction(private val pointer: Pointer?) : Runnable {
        override fun run() {
            pointer?.let { ptr ->
                uniffiRustCall { status ->
                    UniffiLib.INSTANCE.{{ obj.ffi_object_free().name() }}(ptr, status)!!
                }
            }
        }
    }

    fun uniffiClonePointer(): Pointer {
        return uniffiRustCall() { status ->
            UniffiLib.INSTANCE.{{ obj.ffi_object_clone().name() }}(pointer!!, status)!!
        }
    }

    {% for meth in obj.methods() -%}
    {%- call kt::func_decl("override", meth, 4) %}
    {% endfor %}

    {%- for tm in obj.uniffi_traits() %}
    {%-     match tm %}
    {%         when UniffiTrait::Display { fmt } %}
    override fun toString(): String {
        return {{ fmt.return_type().unwrap()|lift_fn }}({% call kt::to_ffi_call(fmt) %})
    }
    {%         when UniffiTrait::Eq { eq, ne } %}
    {# only equals used #}
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is {{ impl_class_name}}) return false
        return {{ eq.return_type().unwrap()|lift_fn }}({% call kt::to_ffi_call(eq) %})
    }
    {%         when UniffiTrait::Hash { hash } %}
    override fun hashCode(): Int {
        return {{ hash.return_type().unwrap()|lift_fn }}({%- call kt::to_ffi_call(hash) %}).toInt()
    }
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
    {% else if is_error %}
    companion object ErrorHandler : UniffiRustCallStatusErrorHandler<{{ impl_class_name }}> {
        override fun lift(error_buf: RustBufferByValue): {{ impl_class_name }} {
            // Due to some mismatches in the ffi converter mechanisms, errors are a RustBuffer.
            val bb = error_buf.asByteBuffer()
            if (bb == null) {
                throw InternalException("?")
            }
            return {{ ffi_converter_name }}.read(bb)
        }
    }
    {% else %}
    companion object
    {% endif %}
}
{%- if obj.has_callback_interface() %}
{%- let vtable = obj.vtable().expect("trait interface should have a vtable") %}
{%- let vtable_methods = obj.vtable_methods() %}
{%- let ffi_init_callback = obj.ffi_init_callback() %}

{% include "CallbackInterfaceImpl.kt" %}

{%- endif %}

{% macro converter_type(obj) %}
{% if obj.has_callback_interface() %}
{{ interface_name }}
{% else %}
{{ impl_class_name }}
{% endif %}
{% endmacro %}

public object {{ ffi_converter_name }}: FfiConverter<{%- call converter_type(obj) -%}, Pointer> {
    {%- if obj.has_callback_interface() %}
    internal val handleMap = UniffiHandleMap<{%- call converter_type(obj) -%}>()
    {%- endif %}

    override fun lower(value: {%- call converter_type(obj) -%}): Pointer {
        {%- if obj.has_callback_interface() %}
        return handleMap.insert(value).toPointer()
        {%- else %}
        val obj = value as {{ impl_class_name }}
        return obj.uniffiClonePointer()
        {%- endif %}
        }


    override fun lift(value: Pointer): {%- call converter_type(obj) -%} {
        return {{ impl_class_name }}(value)
    }

    override fun read(buf: ByteBuffer): {%- call converter_type(obj) -%} {
        // The Rust code always writes pointers as 8 bytes, and will
        // fail to compile if they don't fit.
        return lift(buf.getLong().toPointer())
    }

    override fun allocationSize(value: {%- call converter_type(obj) -%}) = 8UL

    override fun write(value: {%- call converter_type(obj) -%}, buf: ByteBuffer) {
        // The Rust code always expects pointers written as 8 bytes,
        // and will fail to compile if they don't fit.
        buf.putLong(getPointerNativeValue(lower(value)))
    }
}
