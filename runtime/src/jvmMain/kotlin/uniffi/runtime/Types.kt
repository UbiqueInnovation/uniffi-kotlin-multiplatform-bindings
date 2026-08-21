@file:Suppress("CanBePrimaryConstructorProperty", "unused")

package uniffi.runtime

import com.sun.jna.Callback
import com.sun.jna.Structure

@Structure.FieldOrder("handle", "free")
open class UniffiForeignFutureDroppedCallbackStructStruct(
    handle: Long,
    free: Any?,
) : Structure() {
    @JvmField var handle: Long = handle
    @JvmField var free: UniffiForeignFutureDroppedCallback? = free as UniffiForeignFutureDroppedCallback?

    constructor() : this(
        handle = 0.toLong(),
        free = null,
    )

    class UniffiByValue(
        handle: Long,
        free: Any?,
    ) : UniffiForeignFutureDroppedCallbackStruct(handle, free), ByValue
}

typealias UniffiForeignFutureDroppedCallbackStruct = UniffiForeignFutureDroppedCallbackStructStruct

var UniffiForeignFutureDroppedCallbackStruct.handle: Long
    get() = this.handle
    set(value) {
        this.handle = value
    }

var UniffiForeignFutureDroppedCallbackStruct.free: Any?
    get() = this.free
    set(value) {
        this.free = value as UniffiForeignFutureDroppedCallback?
    }

fun UniffiForeignFutureDroppedCallbackStruct.uniffiSetValue(other: UniffiForeignFutureDroppedCallbackStruct) {
    handle = other.handle
    free = other.free
}

fun UniffiForeignFutureDroppedCallbackStruct.uniffiSetValue(other: UniffiForeignFutureDroppedCallbackStructUniffiByValue) {
    handle = other.handle
    free = other.free
}

typealias UniffiForeignFutureDroppedCallbackStructUniffiByValue = UniffiForeignFutureDroppedCallbackStructStruct.UniffiByValue

val UniffiForeignFutureDroppedCallbackStructUniffiByValue.handle: Long
    get() = this.handle

val UniffiForeignFutureDroppedCallbackStructUniffiByValue.free: Any?
    get() = this.free

@Structure.FieldOrder("returnValue", "callStatus")
open class UniffiForeignFutureResultU8Struct(
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
    ) : UniffiForeignFutureResultU8(returnValue, callStatus), ByValue
}

typealias UniffiForeignFutureResultU8 = UniffiForeignFutureResultU8Struct

fun UniffiForeignFutureResultU8.uniffiSetValue(other: UniffiForeignFutureResultU8) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

fun UniffiForeignFutureResultU8.uniffiSetValue(other: UniffiForeignFutureResultU8UniffiByValue) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

typealias UniffiForeignFutureResultU8UniffiByValue = UniffiForeignFutureResultU8Struct.UniffiByValue

interface UniffiForeignFutureCompleteU8 : Callback {
    fun callback(
        callbackData: Long,
        result: UniffiForeignFutureResultU8UniffiByValue,
    )
}

@Structure.FieldOrder("returnValue", "callStatus")
open class UniffiForeignFutureResultI8Struct(
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
    ) : UniffiForeignFutureResultI8(returnValue, callStatus), ByValue
}

typealias UniffiForeignFutureResultI8 = UniffiForeignFutureResultI8Struct

fun UniffiForeignFutureResultI8.uniffiSetValue(other: UniffiForeignFutureResultI8) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

fun UniffiForeignFutureResultI8.uniffiSetValue(other: UniffiForeignFutureResultI8UniffiByValue) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

typealias UniffiForeignFutureResultI8UniffiByValue = UniffiForeignFutureResultI8Struct.UniffiByValue

interface UniffiForeignFutureCompleteI8 : Callback {
    fun callback(
        callbackData: Long,
        result: UniffiForeignFutureResultI8UniffiByValue,
    )
}

@Structure.FieldOrder("returnValue", "callStatus")
open class UniffiForeignFutureResultU16Struct(
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
    ) : UniffiForeignFutureResultU16(returnValue, callStatus), ByValue
}

typealias UniffiForeignFutureResultU16 = UniffiForeignFutureResultU16Struct

fun UniffiForeignFutureResultU16.uniffiSetValue(other: UniffiForeignFutureResultU16) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

fun UniffiForeignFutureResultU16.uniffiSetValue(other: UniffiForeignFutureResultU16UniffiByValue) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

typealias UniffiForeignFutureResultU16UniffiByValue = UniffiForeignFutureResultU16Struct.UniffiByValue

interface UniffiForeignFutureCompleteU16 : Callback {
    fun callback(
        callbackData: Long,
        result: UniffiForeignFutureResultU16UniffiByValue,
    )
}

@Structure.FieldOrder("returnValue", "callStatus")
open class UniffiForeignFutureResultI16Struct(
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
    ) : UniffiForeignFutureResultI16(returnValue, callStatus),ByValue
}

typealias UniffiForeignFutureResultI16 = UniffiForeignFutureResultI16Struct

fun UniffiForeignFutureResultI16.uniffiSetValue(other: UniffiForeignFutureResultI16) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

fun UniffiForeignFutureResultI16.uniffiSetValue(other: UniffiForeignFutureResultI16UniffiByValue) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

typealias UniffiForeignFutureResultI16UniffiByValue = UniffiForeignFutureResultI16Struct.UniffiByValue

interface UniffiForeignFutureCompleteI16 : Callback {
    fun callback(
        callbackData: Long,
        result: UniffiForeignFutureResultI16UniffiByValue,
    )
}

@Structure.FieldOrder("returnValue", "callStatus")
open class UniffiForeignFutureResultU32Struct(
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
    ) : UniffiForeignFutureResultU32(returnValue, callStatus), ByValue
}

typealias UniffiForeignFutureResultU32 = UniffiForeignFutureResultU32Struct

fun UniffiForeignFutureResultU32.uniffiSetValue(other: UniffiForeignFutureResultU32) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

fun UniffiForeignFutureResultU32.uniffiSetValue(other: UniffiForeignFutureResultU32UniffiByValue) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

typealias UniffiForeignFutureResultU32UniffiByValue = UniffiForeignFutureResultU32Struct.UniffiByValue

interface UniffiForeignFutureCompleteU32 : Callback {
    fun callback(
        callbackData: Long,
        result: UniffiForeignFutureResultU32UniffiByValue,
    )
}

@Structure.FieldOrder("returnValue", "callStatus")
open class UniffiForeignFutureResultI32Struct(
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
    ) : UniffiForeignFutureResultI32(returnValue, callStatus), ByValue
}

typealias UniffiForeignFutureResultI32 = UniffiForeignFutureResultI32Struct

fun UniffiForeignFutureResultI32.uniffiSetValue(other: UniffiForeignFutureResultI32) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

fun UniffiForeignFutureResultI32.uniffiSetValue(other: UniffiForeignFutureResultI32UniffiByValue) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

typealias UniffiForeignFutureResultI32UniffiByValue = UniffiForeignFutureResultI32Struct.UniffiByValue

interface UniffiForeignFutureCompleteI32 : Callback {
    fun callback(
        callbackData: Long,
        result: UniffiForeignFutureResultI32UniffiByValue,
    )
}

@Structure.FieldOrder("returnValue", "callStatus")
open class UniffiForeignFutureResultU64Struct(
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
    ) : UniffiForeignFutureResultU64(returnValue, callStatus), ByValue
}

typealias UniffiForeignFutureResultU64 = UniffiForeignFutureResultU64Struct

fun UniffiForeignFutureResultU64.uniffiSetValue(other: UniffiForeignFutureResultU64) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

fun UniffiForeignFutureResultU64.uniffiSetValue(other: UniffiForeignFutureResultU64UniffiByValue) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

typealias UniffiForeignFutureResultU64UniffiByValue = UniffiForeignFutureResultU64Struct.UniffiByValue

interface UniffiForeignFutureCompleteU64 : Callback {
    fun callback(
        callbackData: Long,
        result: UniffiForeignFutureResultU64UniffiByValue,
    )
}

@Structure.FieldOrder("returnValue", "callStatus")
open class UniffiForeignFutureResultI64Struct(
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
    ) : UniffiForeignFutureResultI64(returnValue, callStatus), ByValue
}

typealias UniffiForeignFutureResultI64 = UniffiForeignFutureResultI64Struct

fun UniffiForeignFutureResultI64.uniffiSetValue(other: UniffiForeignFutureResultI64) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

fun UniffiForeignFutureResultI64.uniffiSetValue(other: UniffiForeignFutureResultI64UniffiByValue) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

typealias UniffiForeignFutureResultI64UniffiByValue = UniffiForeignFutureResultI64Struct.UniffiByValue

interface UniffiForeignFutureCompleteI64 : Callback {
    fun callback(
        callbackData: Long,
        result: UniffiForeignFutureResultI64UniffiByValue,
    )
}

@Structure.FieldOrder("returnValue", "callStatus")
open class UniffiForeignFutureResultF32Struct(
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
    ) : UniffiForeignFutureResultF32(returnValue, callStatus), ByValue
}

typealias UniffiForeignFutureResultF32 = UniffiForeignFutureResultF32Struct

fun UniffiForeignFutureResultF32.uniffiSetValue(other: UniffiForeignFutureResultF32) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

fun UniffiForeignFutureResultF32.uniffiSetValue(other: UniffiForeignFutureResultF32UniffiByValue) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

typealias UniffiForeignFutureResultF32UniffiByValue = UniffiForeignFutureResultF32Struct.UniffiByValue

interface UniffiForeignFutureCompleteF32 : Callback {
    fun callback(
        callbackData: Long,
        result: UniffiForeignFutureResultF32UniffiByValue,
    )
}

@Structure.FieldOrder("returnValue", "callStatus")
open class UniffiForeignFutureResultF64Struct(
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
    ) : UniffiForeignFutureResultF64(returnValue, callStatus), ByValue
}

typealias UniffiForeignFutureResultF64 = UniffiForeignFutureResultF64Struct

fun UniffiForeignFutureResultF64.uniffiSetValue(other: UniffiForeignFutureResultF64) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

fun UniffiForeignFutureResultF64.uniffiSetValue(other: UniffiForeignFutureResultF64UniffiByValue) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

typealias UniffiForeignFutureResultF64UniffiByValue = UniffiForeignFutureResultF64Struct.UniffiByValue

interface UniffiForeignFutureCompleteF64 : Callback {
    fun callback(
        callbackData: Long,
        result: UniffiForeignFutureResultF64UniffiByValue,
    )
}

@Structure.FieldOrder("returnValue", "callStatus")
open class UniffiForeignFutureResultRustBufferStruct(
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
    ) : UniffiForeignFutureResultRustBuffer(returnValue, callStatus), ByValue
}

typealias UniffiForeignFutureResultRustBuffer = UniffiForeignFutureResultRustBufferStruct

fun UniffiForeignFutureResultRustBuffer.uniffiSetValue(other: UniffiForeignFutureResultRustBuffer) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

fun UniffiForeignFutureResultRustBuffer.uniffiSetValue(other: UniffiForeignFutureResultRustBufferUniffiByValue) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

typealias UniffiForeignFutureResultRustBufferUniffiByValue = UniffiForeignFutureResultRustBufferStruct.UniffiByValue

interface UniffiForeignFutureCompleteRustBuffer : Callback {
    fun callback(
        callbackData: Long,
        result: UniffiForeignFutureResultRustBufferUniffiByValue,
    )
}

@Structure.FieldOrder("callStatus")
open class UniffiForeignFutureResultVoidStruct(
    callStatus: UniffiRustCallStatusByValue,
) : Structure() {
    @JvmField var callStatus: UniffiRustCallStatusByValue = callStatus

    constructor() : this(
        callStatus = UniffiRustCallStatusHelper.allocValue(),
    )

    class UniffiByValue(
        callStatus: UniffiRustCallStatusByValue,
    ) : UniffiForeignFutureResultVoid(callStatus), ByValue
}

typealias UniffiForeignFutureResultVoid = UniffiForeignFutureResultVoidStruct

fun UniffiForeignFutureResultVoid.uniffiSetValue(other: UniffiForeignFutureResultVoid) {
    callStatus = other.callStatus
}

fun UniffiForeignFutureResultVoid.uniffiSetValue(other: UniffiForeignFutureResultVoidUniffiByValue) {
    callStatus = other.callStatus
}

typealias UniffiForeignFutureResultVoidUniffiByValue = UniffiForeignFutureResultVoidStruct.UniffiByValue

interface UniffiForeignFutureCompleteVoid: Callback {
    fun callback(callbackData: Long, result: UniffiForeignFutureResultVoidUniffiByValue,)
}
