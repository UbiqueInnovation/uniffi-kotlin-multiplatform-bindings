@file:Suppress("CanBePrimaryConstructorProperty", "unused")

package uniffi.runtime

import com.sun.jna.Callback
import com.sun.jna.Structure

@Structure.FieldOrder("handle", "free")
open class UniffiForeignFutureStruct(
    handle: Long,
    free: Any?,
) : Structure() {
    @JvmField internal var handle: Long = handle
    @JvmField internal var free: UniffiForeignFutureFree? = free as UniffiForeignFutureFree?

    constructor() : this(
        handle = 0.toLong(),
        free = null,
    )

    class UniffiByValue(
        handle: Long,
        free: Any?,
    ) : UniffiForeignFuture(handle, free), ByValue
}

typealias UniffiForeignFuture = UniffiForeignFutureStruct

var UniffiForeignFuture.handle: Long
    get() = this.handle
    set(value) {
        this.handle = value
    }

var UniffiForeignFuture.free: Any?
    get() = this.free
    set(value) {
        this.free = value as UniffiForeignFutureFree?
    }

fun UniffiForeignFuture.uniffiSetValue(other: UniffiForeignFuture) {
    handle = other.handle
    free = other.free
}

fun UniffiForeignFuture.uniffiSetValue(other: UniffiForeignFutureUniffiByValue) {
    handle = other.handle
    free = other.free
}

typealias UniffiForeignFutureUniffiByValue = UniffiForeignFutureStruct.UniffiByValue

val UniffiForeignFutureUniffiByValue.handle: Long
    get() = this.handle

val UniffiForeignFutureUniffiByValue.free: Any?
    get() = this.free

@Structure.FieldOrder("returnValue", "callStatus")
open class UniffiForeignFutureStructU8Struct(
    returnValue: Byte,
    callStatus: UniffiRustCallStatusByValue,
) : Structure() {
    @JvmField var returnValue: Byte = returnValue
    @JvmField var callStatus: UniffiRustCallStatusByValue = callStatus

    constructor() : this(
        returnValue = 0.toByte(),
        callStatus = UniffiRustCallStatusHelper.allocValue(),
    )

    class UniffiByValue(
        returnValue: Byte,
        callStatus: UniffiRustCallStatusByValue,
    ) : UniffiForeignFutureStructU8(returnValue, callStatus), ByValue
}

typealias UniffiForeignFutureStructU8 = UniffiForeignFutureStructU8Struct

fun UniffiForeignFutureStructU8.uniffiSetValue(other: UniffiForeignFutureStructU8) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

fun UniffiForeignFutureStructU8.uniffiSetValue(other: UniffiForeignFutureStructU8UniffiByValue) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

typealias UniffiForeignFutureStructU8UniffiByValue = UniffiForeignFutureStructU8Struct.UniffiByValue

interface UniffiForeignFutureCompleteU8 : Callback {
    fun callback(
        callbackData: Long,
        result: UniffiForeignFutureStructU8UniffiByValue,
    )
}

@Structure.FieldOrder("returnValue", "callStatus")
open class UniffiForeignFutureStructI8Struct(
    returnValue: Byte,
    callStatus: UniffiRustCallStatusByValue,
) : Structure() {
    @JvmField var returnValue: Byte = returnValue
    @JvmField var callStatus: UniffiRustCallStatusByValue = callStatus

    constructor() : this(
        returnValue = 0.toByte(),
        callStatus = UniffiRustCallStatusHelper.allocValue(),
    )

    class UniffiByValue(
        returnValue: Byte,
        callStatus: UniffiRustCallStatusByValue,
    ) : UniffiForeignFutureStructI8(returnValue, callStatus), ByValue
}

typealias UniffiForeignFutureStructI8 = UniffiForeignFutureStructI8Struct

fun UniffiForeignFutureStructI8.uniffiSetValue(other: UniffiForeignFutureStructI8) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

fun UniffiForeignFutureStructI8.uniffiSetValue(other: UniffiForeignFutureStructI8UniffiByValue) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

typealias UniffiForeignFutureStructI8UniffiByValue = UniffiForeignFutureStructI8Struct.UniffiByValue

interface UniffiForeignFutureCompleteI8 : Callback {
    fun callback(
        callbackData: Long,
        result: UniffiForeignFutureStructI8UniffiByValue,
    )
}

@Structure.FieldOrder("returnValue", "callStatus")
open class UniffiForeignFutureStructU16Struct(
    returnValue: Short,
    callStatus: UniffiRustCallStatusByValue,
) : Structure() {
    @JvmField var returnValue: Short = returnValue
    @JvmField var callStatus: UniffiRustCallStatusByValue = callStatus

    constructor() : this(
        returnValue = 0.toShort(),
        callStatus = UniffiRustCallStatusHelper.allocValue(),
    )

    class UniffiByValue(
        returnValue: Short,
        callStatus: UniffiRustCallStatusByValue,
    ) : UniffiForeignFutureStructU16(returnValue, callStatus), ByValue
}

typealias UniffiForeignFutureStructU16 = UniffiForeignFutureStructU16Struct

fun UniffiForeignFutureStructU16.uniffiSetValue(other: UniffiForeignFutureStructU16) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

fun UniffiForeignFutureStructU16.uniffiSetValue(other: UniffiForeignFutureStructU16UniffiByValue) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

typealias UniffiForeignFutureStructU16UniffiByValue = UniffiForeignFutureStructU16Struct.UniffiByValue

interface UniffiForeignFutureCompleteU16 : Callback {
    fun callback(
        callbackData: Long,
        result: UniffiForeignFutureStructU16UniffiByValue,
    )
}

@Structure.FieldOrder("returnValue", "callStatus")
open class UniffiForeignFutureStructI16Struct(
    returnValue: Short,
    callStatus: UniffiRustCallStatusByValue,
) : Structure() {
    @JvmField var returnValue: Short = returnValue
    @JvmField var callStatus: UniffiRustCallStatusByValue = callStatus

    constructor() : this(
        returnValue = 0.toShort(),
        callStatus = UniffiRustCallStatusHelper.allocValue(),
    )

    class UniffiByValue(
        returnValue: Short,
        callStatus: UniffiRustCallStatusByValue,
    ) : UniffiForeignFutureStructI16(returnValue, callStatus),ByValue
}

typealias UniffiForeignFutureStructI16 = UniffiForeignFutureStructI16Struct

fun UniffiForeignFutureStructI16.uniffiSetValue(other: UniffiForeignFutureStructI16) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

fun UniffiForeignFutureStructI16.uniffiSetValue(other: UniffiForeignFutureStructI16UniffiByValue) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

typealias UniffiForeignFutureStructI16UniffiByValue = UniffiForeignFutureStructI16Struct.UniffiByValue

interface UniffiForeignFutureCompleteI16 : Callback {
    fun callback(
        callbackData: Long,
        result: UniffiForeignFutureStructI16UniffiByValue,
    )
}

@Structure.FieldOrder("returnValue", "callStatus")
open class UniffiForeignFutureStructU32Struct(
    returnValue: Int,
    callStatus: UniffiRustCallStatusByValue,
) : Structure() {
    @JvmField var returnValue: Int = returnValue
    @JvmField var callStatus: UniffiRustCallStatusByValue = callStatus

    constructor() : this(
        returnValue = 0,
        callStatus = UniffiRustCallStatusHelper.allocValue(),
    )

    class UniffiByValue(
        returnValue: Int,
        callStatus: UniffiRustCallStatusByValue,
    ) : UniffiForeignFutureStructU32(returnValue, callStatus), ByValue
}

typealias UniffiForeignFutureStructU32 = UniffiForeignFutureStructU32Struct

fun UniffiForeignFutureStructU32.uniffiSetValue(other: UniffiForeignFutureStructU32) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

fun UniffiForeignFutureStructU32.uniffiSetValue(other: UniffiForeignFutureStructU32UniffiByValue) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

typealias UniffiForeignFutureStructU32UniffiByValue = UniffiForeignFutureStructU32Struct.UniffiByValue

interface UniffiForeignFutureCompleteU32 : Callback {
    fun callback(
        callbackData: Long,
        result: UniffiForeignFutureStructU32UniffiByValue,
    )
}

@Structure.FieldOrder("returnValue", "callStatus")
open class UniffiForeignFutureStructI32Struct(
    returnValue: Int,
    callStatus: UniffiRustCallStatusByValue,
) : Structure() {
    @JvmField var returnValue: Int = returnValue
    @JvmField var callStatus: UniffiRustCallStatusByValue = callStatus

    constructor() : this(
        returnValue = 0,
        callStatus = UniffiRustCallStatusHelper.allocValue(),
    )

    class UniffiByValue(
        returnValue: Int,
        callStatus: UniffiRustCallStatusByValue,
    ) : UniffiForeignFutureStructI32(returnValue, callStatus), ByValue
}

typealias UniffiForeignFutureStructI32 = UniffiForeignFutureStructI32Struct

fun UniffiForeignFutureStructI32.uniffiSetValue(other: UniffiForeignFutureStructI32) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

fun UniffiForeignFutureStructI32.uniffiSetValue(other: UniffiForeignFutureStructI32UniffiByValue) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

typealias UniffiForeignFutureStructI32UniffiByValue = UniffiForeignFutureStructI32Struct.UniffiByValue

interface UniffiForeignFutureCompleteI32 : Callback {
    fun callback(
        callbackData: Long,
        result: UniffiForeignFutureStructI32UniffiByValue,
    )
}

@Structure.FieldOrder("returnValue", "callStatus")
open class UniffiForeignFutureStructU64Struct(
    returnValue: Long,
    callStatus: UniffiRustCallStatusByValue,
) : Structure() {
    @JvmField var returnValue: Long = returnValue
    @JvmField var callStatus: UniffiRustCallStatusByValue = callStatus

    constructor() : this(
        returnValue = 0.toLong(),
        callStatus = UniffiRustCallStatusHelper.allocValue(),
    )

    class UniffiByValue(
        returnValue: Long,
        callStatus: UniffiRustCallStatusByValue,
    ) : UniffiForeignFutureStructU64(returnValue, callStatus), ByValue
}

typealias UniffiForeignFutureStructU64 = UniffiForeignFutureStructU64Struct

fun UniffiForeignFutureStructU64.uniffiSetValue(other: UniffiForeignFutureStructU64) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

fun UniffiForeignFutureStructU64.uniffiSetValue(other: UniffiForeignFutureStructU64UniffiByValue) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

typealias UniffiForeignFutureStructU64UniffiByValue = UniffiForeignFutureStructU64Struct.UniffiByValue

interface UniffiForeignFutureCompleteU64 : Callback {
    fun callback(
        callbackData: Long,
        result: UniffiForeignFutureStructU64UniffiByValue,
    )
}

@Structure.FieldOrder("returnValue", "callStatus")
open class UniffiForeignFutureStructI64Struct(
    returnValue: Long,
    callStatus: UniffiRustCallStatusByValue,
) : Structure() {
    @JvmField var returnValue: Long = returnValue
    @JvmField var callStatus: UniffiRustCallStatusByValue = callStatus

    constructor() : this(
        returnValue = 0.toLong(),
        callStatus = UniffiRustCallStatusHelper.allocValue(),
    )

    class UniffiByValue(
        returnValue: Long,
        callStatus: UniffiRustCallStatusByValue,
    ) : UniffiForeignFutureStructI64(returnValue, callStatus), ByValue
}

typealias UniffiForeignFutureStructI64 = UniffiForeignFutureStructI64Struct

fun UniffiForeignFutureStructI64.uniffiSetValue(other: UniffiForeignFutureStructI64) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

fun UniffiForeignFutureStructI64.uniffiSetValue(other: UniffiForeignFutureStructI64UniffiByValue) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

typealias UniffiForeignFutureStructI64UniffiByValue = UniffiForeignFutureStructI64Struct.UniffiByValue

interface UniffiForeignFutureCompleteI64 : Callback {
    fun callback(
        callbackData: Long,
        result: UniffiForeignFutureStructI64UniffiByValue,
    )
}

@Structure.FieldOrder("returnValue", "callStatus")
open class UniffiForeignFutureStructF32Struct(
    returnValue: Float,
    callStatus: UniffiRustCallStatusByValue,
) : Structure() {
    @JvmField var returnValue: Float = returnValue
    @JvmField var callStatus: UniffiRustCallStatusByValue = callStatus

    constructor() : this(
        returnValue = 0.0f,
        callStatus = UniffiRustCallStatusHelper.allocValue(),
    )

    class UniffiByValue(
        returnValue: Float,
        callStatus: UniffiRustCallStatusByValue,
    ) : UniffiForeignFutureStructF32(returnValue, callStatus), ByValue
}

typealias UniffiForeignFutureStructF32 = UniffiForeignFutureStructF32Struct

fun UniffiForeignFutureStructF32.uniffiSetValue(other: UniffiForeignFutureStructF32) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

fun UniffiForeignFutureStructF32.uniffiSetValue(other: UniffiForeignFutureStructF32UniffiByValue) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

typealias UniffiForeignFutureStructF32UniffiByValue = UniffiForeignFutureStructF32Struct.UniffiByValue

interface UniffiForeignFutureCompleteF32 : Callback {
    fun callback(
        callbackData: Long,
        result: UniffiForeignFutureStructF32UniffiByValue,
    )
}

@Structure.FieldOrder("returnValue", "callStatus")
open class UniffiForeignFutureStructF64Struct(
    returnValue: Double,
    callStatus: UniffiRustCallStatusByValue,
) : Structure() {
    @JvmField var returnValue: Double = returnValue
    @JvmField var callStatus: UniffiRustCallStatusByValue = callStatus

    constructor() : this(
        returnValue = 0.0,
        callStatus = UniffiRustCallStatusHelper.allocValue(),
    )

    class UniffiByValue(
        returnValue: Double,
        callStatus: UniffiRustCallStatusByValue,
    ) : UniffiForeignFutureStructF64(returnValue, callStatus), ByValue
}

typealias UniffiForeignFutureStructF64 = UniffiForeignFutureStructF64Struct

fun UniffiForeignFutureStructF64.uniffiSetValue(other: UniffiForeignFutureStructF64) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

fun UniffiForeignFutureStructF64.uniffiSetValue(other: UniffiForeignFutureStructF64UniffiByValue) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

typealias UniffiForeignFutureStructF64UniffiByValue = UniffiForeignFutureStructF64Struct.UniffiByValue

interface UniffiForeignFutureCompleteF64 : Callback {
    fun callback(
        callbackData: Long,
        result: UniffiForeignFutureStructF64UniffiByValue,
    )
}

@Structure.FieldOrder("returnValue", "callStatus")
open class UniffiForeignFutureStructPointerStruct(
    returnValue: Pointer?,
    callStatus: UniffiRustCallStatusByValue,
) : Structure() {
    @JvmField var returnValue: Pointer? = returnValue
    @JvmField var callStatus: UniffiRustCallStatusByValue = callStatus

    constructor() : this(
        returnValue = NullPointer,
        callStatus = UniffiRustCallStatusHelper.allocValue(),
    )

    class UniffiByValue(
        returnValue: Pointer?,
        callStatus: UniffiRustCallStatusByValue,
    ) : UniffiForeignFutureStructPointer(returnValue, callStatus), ByValue
}

typealias UniffiForeignFutureStructPointer = UniffiForeignFutureStructPointerStruct

fun UniffiForeignFutureStructPointer.uniffiSetValue(other: UniffiForeignFutureStructPointer) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

fun UniffiForeignFutureStructPointer.uniffiSetValue(other: UniffiForeignFutureStructPointerUniffiByValue) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

typealias UniffiForeignFutureStructPointerUniffiByValue = UniffiForeignFutureStructPointerStruct.UniffiByValue

interface UniffiForeignFutureCompletePointer : Callback {
    fun callback(
        callbackData: Long,
        result: UniffiForeignFutureStructPointerUniffiByValue,
    )
}

@Structure.FieldOrder("returnValue", "callStatus")
open class UniffiForeignFutureStructRustBufferStruct(
    returnValue: RustBufferByValue,
    callStatus: UniffiRustCallStatusByValue,
) : Structure() {
    @JvmField var returnValue: RustBufferByValue = returnValue
    @JvmField var callStatus: UniffiRustCallStatusByValue = callStatus

    constructor() : this(
        returnValue = RustBufferHelper.allocValue(),
        callStatus = UniffiRustCallStatusHelper.allocValue(),
    )

    class UniffiByValue(
        returnValue: RustBufferByValue,
        callStatus: UniffiRustCallStatusByValue,
    ) : UniffiForeignFutureStructRustBuffer(returnValue, callStatus), ByValue
}

typealias UniffiForeignFutureStructRustBuffer = UniffiForeignFutureStructRustBufferStruct

fun UniffiForeignFutureStructRustBuffer.uniffiSetValue(other: UniffiForeignFutureStructRustBuffer) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

fun UniffiForeignFutureStructRustBuffer.uniffiSetValue(other: UniffiForeignFutureStructRustBufferUniffiByValue) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

typealias UniffiForeignFutureStructRustBufferUniffiByValue = UniffiForeignFutureStructRustBufferStruct.UniffiByValue

interface UniffiForeignFutureCompleteRustBuffer : Callback {
    fun callback(
        callbackData: Long,
        result: UniffiForeignFutureStructRustBufferUniffiByValue,
    )
}

@Structure.FieldOrder("callStatus")
open class UniffiForeignFutureStructVoidStruct(
    callStatus: UniffiRustCallStatusByValue,
) : Structure() {
    @JvmField var callStatus: UniffiRustCallStatusByValue = callStatus

    constructor() : this(
        callStatus = UniffiRustCallStatusHelper.allocValue(),
    )

    class UniffiByValue(
        callStatus: UniffiRustCallStatusByValue,
    ) : UniffiForeignFutureStructVoid(callStatus), ByValue
}

typealias UniffiForeignFutureStructVoid = UniffiForeignFutureStructVoidStruct

fun UniffiForeignFutureStructVoid.uniffiSetValue(other: UniffiForeignFutureStructVoid) {
    callStatus = other.callStatus
}

fun UniffiForeignFutureStructVoid.uniffiSetValue(other: UniffiForeignFutureStructVoidUniffiByValue) {
    callStatus = other.callStatus
}

typealias UniffiForeignFutureStructVoidUniffiByValue = UniffiForeignFutureStructVoidStruct.UniffiByValue
