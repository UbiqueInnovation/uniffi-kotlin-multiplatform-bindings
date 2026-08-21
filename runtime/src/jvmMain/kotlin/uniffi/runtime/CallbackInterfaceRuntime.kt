package uniffi.runtime

import com.sun.jna.NativeLibrary
import com.sun.jna.Structure

const val IDX_CALLBACK_FREE = 0
// Callback return codes
const val UNIFFI_CALLBACK_SUCCESS = 0
const val UNIFFI_CALLBACK_ERROR = 1
const val UNIFFI_CALLBACK_UNEXPECTED_ERROR = 2

abstract class FfiConverterCallbackInterface<CallbackInterface: Any>:
    FfiConverter<CallbackInterface, Long> {
    val handleMap = UniffiHandleMap<CallbackInterface>()

    fun drop(handle: Long) {
        handleMap.remove(handle)
    }

    override fun lift(value: Long): CallbackInterface {
        return handleMap.get(value)
    }

    override fun read(buf: ByteBuffer) = lift(buf.getLong())

    override fun lower(value: CallbackInterface) = handleMap.insert(value)

    override fun allocationSize(value: CallbackInterface) = 8UL

    override fun write(value: CallbackInterface, buf: ByteBuffer) {
        buf.putLong(lower(value))
    }
}

/**
 * App-wide registry that keeps every foreign callback interface vtable installed into
 * every loaded uniffi library.
 *
 * Each Gradle module builds its own cdylib and statically links its Rust dependencies,
 * so the declaring crate's `UniffiForeignPointerCell` for a `with_foreign` trait exists
 * once *per linked image*. The Kotlin package for that crate is generated once and bound
 * to one library, so on its own it can only ever fill in one of those cells; passing a
 * Kotlin implementation into a function of a different module then dispatches through a
 * null vtable and aborts the process.
 *
 * Linking cannot be undone from Kotlin, but this registry is a true singleton - the
 * `uniffi.runtime` artifact resolves once for the whole app - so it can know every
 * vtable and every library and keep the cross product installed: a vtable registered
 * later is pushed into the libraries already loaded, and a library loaded later gets
 * every vtable seen so far.
 *
 * Generated bindings call this from `UniffiLib.INSTANCE`'s initialiser. It complements,
 * rather than replaces, the `uniffiEnsureInitialized()` chaining that forces a declaring
 * namespace to initialise in the first place (upstream #2343).
 */
public object UniffiVtableRegistry {
    private class VtableEntry(
        val crate: String,
        val symbol: String,
        val vtable: Structure,
        val contractVersion: Int,
    )

    private class LibraryEntry(
        val library: NativeLibrary,
        val linkedCrates: Set<String>,
    )

    /** Keyed by init symbol, which is unique per callback interface across all crates. */
    private val vtables = LinkedHashMap<String, VtableEntry>()

    /** Keyed by library name, so that the same cdylib loaded by two namespaces counts once. */
    private val libraries = LinkedHashMap<String, LibraryEntry>()

    /**
     * Register a loaded library, and install every vtable known so far into it.
     *
     * @param name the resolved library name, as passed to JNA.
     * @param library **must** come from `Native.getNativeLibrary(lib)` on the loaded
     *   proxy, never from `NativeLibrary.getInstance(name)`. Those are separate entries
     *   in JNA's cache, and for a library shipped inside a jar each entry extracts and
     *   opens its own copy - which would install the vtable into an image nothing calls.
     * @param linkedCrates every crate statically linked into this library. A vtable
     *   whose crate is absent has no cell here and no init symbol to call.
     */
    @Synchronized
    public fun addLibrary(name: String, library: NativeLibrary, linkedCrates: Set<String>) {
        if (libraries.containsKey(name)) return
        val entry = LibraryEntry(library, linkedCrates)
        libraries[name] = entry
        vtables.values.forEach { install(it, entry) }
    }

    /**
     * Publish a callback interface vtable, and install it into every library loaded so
     * far that links [crate].
     *
     * @param crate the crate that declares the callback interface.
     * @param symbol the `uniffi_..._fn_init_callback_vtable_...` init symbol.
     * @param vtable the JNA struct holding the trampolines; a single instance shared by
     *   every image, all of whose entries dispatch through the one handle map that lives
     *   in the declaring namespace's Kotlin package.
     * @param contractVersion the uniffi contract version these bindings were generated
     *   against, checked against each library before its cell is written.
     */
    @Synchronized
    public fun addVtable(crate: String, symbol: String, vtable: Structure, contractVersion: Int) {
        if (vtables.containsKey(symbol)) return
        val entry = VtableEntry(crate, symbol, vtable, contractVersion)
        vtables[symbol] = entry
        libraries.values.forEach { install(entry, it) }
    }

    private fun install(vtable: VtableEntry, library: LibraryEntry) {
        // The only silent path: this library does not contain the declaring crate, so it
        // has neither a cell to fill nor the symbol to fill it with.
        if (vtable.crate !in library.linkedCrates) return

        val actualContractVersion =
            library.library.getFunction("ffi_${vtable.crate}_uniffi_contract_version")
                .invokeInt(arrayOf<Any>())
        check(actualContractVersion == vtable.contractVersion) {
            "UniFFI contract version mismatch for ${vtable.crate} in ${library.library.name}: " +
                "bindings expect ${vtable.contractVersion}, that library has $actualContractVersion"
        }

        // Writing the same pointer into an already-set cell is an idempotent atomic store,
        // so re-installing over the namespace's own `register(lib)` is harmless.
        library.library.getFunction(vtable.symbol).invokeVoid(arrayOf<Any>(vtable.vtable))
    }
}
