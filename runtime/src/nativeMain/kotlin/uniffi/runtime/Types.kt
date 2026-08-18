@file:Suppress("UNCHECKED_CAST", "unused")
@file:OptIn(ExperimentalForeignApi::class)

package uniffi.runtime

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import kotlinx.cinterop.pointed
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import kotlinx.cinterop.write

typealias UniffiForeignFutureDroppedCallbackStruct = CPointer<cinterop.UniffiForeignFutureDroppedCallbackStruct>

var UniffiForeignFutureDroppedCallbackStruct.handle: Long
    get() = pointed.handle
    set(value) {
        pointed.handle = value
    }

var UniffiForeignFutureDroppedCallbackStruct.free: Any?
    get() = pointed.free
    set(value) {
        pointed.free = value as UniffiForeignFutureDroppedCallback?
    }

fun UniffiForeignFutureDroppedCallbackStruct.uniffiSetValue(other: UniffiForeignFutureDroppedCallbackStruct) {
    handle = other.handle
    free = other.free
}

fun UniffiForeignFutureDroppedCallbackStruct.uniffiSetValue(other: UniffiForeignFutureDroppedCallbackStructUniffiByValue) {
    handle = other.handle
    free = other.free
}

typealias UniffiForeignFutureDroppedCallbackStructUniffiByValue = CValue<cinterop.UniffiForeignFutureDroppedCallbackStruct>

fun UniffiForeignFutureDroppedCallbackStructUniffiByValue(
    handle: Long,
    free: Any?,
): UniffiForeignFutureDroppedCallbackStructUniffiByValue =
    cValue<cinterop.UniffiForeignFutureDroppedCallbackStruct> {
        this.handle = handle

        this.free = free as UniffiForeignFutureDroppedCallback?
    }

val UniffiForeignFutureDroppedCallbackStructUniffiByValue.handle: Long
    get() = useContents { handle }

val UniffiForeignFutureDroppedCallbackStructUniffiByValue.free: Any?
    get() = useContents { free }

typealias UniffiForeignFutureResultU8 = CPointer<cinterop.UniffiForeignFutureResultU8>

var UniffiForeignFutureResultU8.returnValue: Byte
    get() = pointed.returnValue
    set(value) {
        pointed.returnValue = value
    }

var UniffiForeignFutureResultU8.callStatus: UniffiRustCallStatusByValue
    get() = pointed.callStatus.readValue()
    set(value) {
        value.write(pointed.callStatus.rawPtr)
    }

fun UniffiForeignFutureResultU8.uniffiSetValue(other: UniffiForeignFutureResultU8) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

fun UniffiForeignFutureResultU8.uniffiSetValue(other: UniffiForeignFutureResultU8UniffiByValue) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

typealias UniffiForeignFutureResultU8UniffiByValue = CValue<cinterop.UniffiForeignFutureResultU8>

fun UniffiForeignFutureResultU8UniffiByValue(
    returnValue: Byte,
    callStatus: UniffiRustCallStatusByValue,
): UniffiForeignFutureResultU8UniffiByValue =
    cValue<cinterop.UniffiForeignFutureResultU8> {
        this.returnValue = returnValue

        callStatus.write(this.callStatus.rawPtr)
    }

val UniffiForeignFutureResultU8UniffiByValue.returnValue: Byte
    get() =
        useContents {
            returnValue
        }

val UniffiForeignFutureResultU8UniffiByValue.callStatus: UniffiRustCallStatusByValue
    get() = useContents { callStatus.readValue() }

typealias UniffiForeignFutureCompleteU8 = cinterop.UniffiForeignFutureCompleteU8
typealias UniffiForeignFutureResultI8 = CPointer<cinterop.UniffiForeignFutureResultI8>

var UniffiForeignFutureResultI8.returnValue: Byte
    get() = pointed.returnValue
    set(value) {
        pointed.returnValue = value
    }

var UniffiForeignFutureResultI8.callStatus: UniffiRustCallStatusByValue
    get() = pointed.callStatus.readValue()
    set(value) {
        value.write(pointed.callStatus.rawPtr)
    }

fun UniffiForeignFutureResultI8.uniffiSetValue(other: UniffiForeignFutureResultI8) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

fun UniffiForeignFutureResultI8.uniffiSetValue(other: UniffiForeignFutureResultI8UniffiByValue) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

typealias UniffiForeignFutureResultI8UniffiByValue = CValue<cinterop.UniffiForeignFutureResultI8>

fun UniffiForeignFutureResultI8UniffiByValue(
    returnValue: Byte,
    callStatus: UniffiRustCallStatusByValue,
): UniffiForeignFutureResultI8UniffiByValue =
    cValue<cinterop.UniffiForeignFutureResultI8> {
        this.returnValue = returnValue

        callStatus.write(this.callStatus.rawPtr)
    }

val UniffiForeignFutureResultI8UniffiByValue.returnValue: Byte
    get() =
        useContents {
            returnValue
        }

val UniffiForeignFutureResultI8UniffiByValue.callStatus: UniffiRustCallStatusByValue
    get() = useContents { callStatus.readValue() }

typealias UniffiForeignFutureCompleteI8 = cinterop.UniffiForeignFutureCompleteI8
typealias UniffiForeignFutureResultU16 = CPointer<cinterop.UniffiForeignFutureResultU16>

var UniffiForeignFutureResultU16.returnValue: Short
    get() = pointed.returnValue
    set(value) {
        pointed.returnValue = value
    }

var UniffiForeignFutureResultU16.callStatus: UniffiRustCallStatusByValue
    get() = pointed.callStatus.readValue()
    set(value) {
        value.write(pointed.callStatus.rawPtr)
    }

fun UniffiForeignFutureResultU16.uniffiSetValue(other: UniffiForeignFutureResultU16) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

fun UniffiForeignFutureResultU16.uniffiSetValue(other: UniffiForeignFutureResultU16UniffiByValue) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

typealias UniffiForeignFutureResultU16UniffiByValue = CValue<cinterop.UniffiForeignFutureResultU16>

fun UniffiForeignFutureResultU16UniffiByValue(
    returnValue: Short,
    callStatus: UniffiRustCallStatusByValue,
): UniffiForeignFutureResultU16UniffiByValue =
    cValue<cinterop.UniffiForeignFutureResultU16> {
        this.returnValue = returnValue

        callStatus.write(this.callStatus.rawPtr)
    }

val UniffiForeignFutureResultU16UniffiByValue.returnValue: Short
    get() =
        useContents {
            returnValue
        }

val UniffiForeignFutureResultU16UniffiByValue.callStatus: UniffiRustCallStatusByValue
    get() = useContents { callStatus.readValue() }

typealias UniffiForeignFutureCompleteU16 = cinterop.UniffiForeignFutureCompleteU16
typealias UniffiForeignFutureResultI16 = CPointer<cinterop.UniffiForeignFutureResultI16>

var UniffiForeignFutureResultI16.returnValue: Short
    get() = pointed.returnValue
    set(value) {
        pointed.returnValue = value
    }

var UniffiForeignFutureResultI16.callStatus: UniffiRustCallStatusByValue
    get() = pointed.callStatus.readValue()
    set(value) {
        value.write(pointed.callStatus.rawPtr)
    }

fun UniffiForeignFutureResultI16.uniffiSetValue(other: UniffiForeignFutureResultI16) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

fun UniffiForeignFutureResultI16.uniffiSetValue(other: UniffiForeignFutureResultI16UniffiByValue) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

typealias UniffiForeignFutureResultI16UniffiByValue = CValue<cinterop.UniffiForeignFutureResultI16>

fun UniffiForeignFutureResultI16UniffiByValue(
    returnValue: Short,
    callStatus: UniffiRustCallStatusByValue,
): UniffiForeignFutureResultI16UniffiByValue =
    cValue<cinterop.UniffiForeignFutureResultI16> {
        this.returnValue = returnValue

        callStatus.write(this.callStatus.rawPtr)
    }

val UniffiForeignFutureResultI16UniffiByValue.returnValue: Short
    get() =
        useContents {
            returnValue
        }

val UniffiForeignFutureResultI16UniffiByValue.callStatus: UniffiRustCallStatusByValue
    get() = useContents { callStatus.readValue() }

typealias UniffiForeignFutureCompleteI16 = cinterop.UniffiForeignFutureCompleteI16
typealias UniffiForeignFutureResultU32 = CPointer<cinterop.UniffiForeignFutureResultU32>

var UniffiForeignFutureResultU32.returnValue: Int
    get() = pointed.returnValue
    set(value) {
        pointed.returnValue = value
    }

var UniffiForeignFutureResultU32.callStatus: UniffiRustCallStatusByValue
    get() = pointed.callStatus.readValue()
    set(value) {
        value.write(pointed.callStatus.rawPtr)
    }

fun UniffiForeignFutureResultU32.uniffiSetValue(other: UniffiForeignFutureResultU32) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

fun UniffiForeignFutureResultU32.uniffiSetValue(other: UniffiForeignFutureResultU32UniffiByValue) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

typealias UniffiForeignFutureResultU32UniffiByValue = CValue<cinterop.UniffiForeignFutureResultU32>

fun UniffiForeignFutureResultU32UniffiByValue(
    returnValue: Int,
    callStatus: UniffiRustCallStatusByValue,
): UniffiForeignFutureResultU32UniffiByValue =
    cValue<cinterop.UniffiForeignFutureResultU32> {
        this.returnValue = returnValue

        callStatus.write(this.callStatus.rawPtr)
    }

val UniffiForeignFutureResultU32UniffiByValue.returnValue: Int
    get() =
        useContents {
            returnValue
        }

val UniffiForeignFutureResultU32UniffiByValue.callStatus: UniffiRustCallStatusByValue
    get() = useContents { callStatus.readValue() }

typealias UniffiForeignFutureCompleteU32 = cinterop.UniffiForeignFutureCompleteU32
typealias UniffiForeignFutureResultI32 = CPointer<cinterop.UniffiForeignFutureResultI32>

var UniffiForeignFutureResultI32.returnValue: Int
    get() = pointed.returnValue
    set(value) {
        pointed.returnValue = value
    }

var UniffiForeignFutureResultI32.callStatus: UniffiRustCallStatusByValue
    get() = pointed.callStatus.readValue()
    set(value) {
        value.write(pointed.callStatus.rawPtr)
    }

fun UniffiForeignFutureResultI32.uniffiSetValue(other: UniffiForeignFutureResultI32) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

fun UniffiForeignFutureResultI32.uniffiSetValue(other: UniffiForeignFutureResultI32UniffiByValue) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

typealias UniffiForeignFutureResultI32UniffiByValue = CValue<cinterop.UniffiForeignFutureResultI32>

fun UniffiForeignFutureResultI32UniffiByValue(
    returnValue: Int,
    callStatus: UniffiRustCallStatusByValue,
): UniffiForeignFutureResultI32UniffiByValue =
    cValue<cinterop.UniffiForeignFutureResultI32> {
        this.returnValue = returnValue

        callStatus.write(this.callStatus.rawPtr)
    }

val UniffiForeignFutureResultI32UniffiByValue.returnValue: Int
    get() =
        useContents {
            returnValue
        }

val UniffiForeignFutureResultI32UniffiByValue.callStatus: UniffiRustCallStatusByValue
    get() = useContents { callStatus.readValue() }

typealias UniffiForeignFutureCompleteI32 = cinterop.UniffiForeignFutureCompleteI32
typealias UniffiForeignFutureResultU64 = CPointer<cinterop.UniffiForeignFutureResultU64>

var UniffiForeignFutureResultU64.returnValue: Long
    get() = pointed.returnValue
    set(value) {
        pointed.returnValue = value
    }

var UniffiForeignFutureResultU64.callStatus: UniffiRustCallStatusByValue
    get() = pointed.callStatus.readValue()
    set(value) {
        value.write(pointed.callStatus.rawPtr)
    }

fun UniffiForeignFutureResultU64.uniffiSetValue(other: UniffiForeignFutureResultU64) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

fun UniffiForeignFutureResultU64.uniffiSetValue(other: UniffiForeignFutureResultU64UniffiByValue) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

typealias UniffiForeignFutureResultU64UniffiByValue = CValue<cinterop.UniffiForeignFutureResultU64>

fun UniffiForeignFutureResultU64UniffiByValue(
    returnValue: Long,
    callStatus: UniffiRustCallStatusByValue,
): UniffiForeignFutureResultU64UniffiByValue =
    cValue<cinterop.UniffiForeignFutureResultU64> {
        this.returnValue = returnValue

        callStatus.write(this.callStatus.rawPtr)
    }

val UniffiForeignFutureResultU64UniffiByValue.returnValue: Long
    get() =
        useContents {
            returnValue
        }

val UniffiForeignFutureResultU64UniffiByValue.callStatus: UniffiRustCallStatusByValue
    get() = useContents { callStatus.readValue() }

typealias UniffiForeignFutureCompleteU64 = cinterop.UniffiForeignFutureCompleteU64
typealias UniffiForeignFutureResultI64 = CPointer<cinterop.UniffiForeignFutureResultI64>

var UniffiForeignFutureResultI64.returnValue: Long
    get() = pointed.returnValue
    set(value) {
        pointed.returnValue = value
    }

var UniffiForeignFutureResultI64.callStatus: UniffiRustCallStatusByValue
    get() = pointed.callStatus.readValue()
    set(value) {
        value.write(pointed.callStatus.rawPtr)
    }

fun UniffiForeignFutureResultI64.uniffiSetValue(other: UniffiForeignFutureResultI64) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

fun UniffiForeignFutureResultI64.uniffiSetValue(other: UniffiForeignFutureResultI64UniffiByValue) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

typealias UniffiForeignFutureResultI64UniffiByValue = CValue<cinterop.UniffiForeignFutureResultI64>

fun UniffiForeignFutureResultI64UniffiByValue(
    returnValue: Long,
    callStatus: UniffiRustCallStatusByValue,
): UniffiForeignFutureResultI64UniffiByValue =
    cValue<cinterop.UniffiForeignFutureResultI64> {
        this.returnValue = returnValue

        callStatus.write(this.callStatus.rawPtr)
    }

val UniffiForeignFutureResultI64UniffiByValue.returnValue: Long
    get() =
        useContents {
            returnValue
        }

val UniffiForeignFutureResultI64UniffiByValue.callStatus: UniffiRustCallStatusByValue
    get() = useContents { callStatus.readValue() }

typealias UniffiForeignFutureCompleteI64 = cinterop.UniffiForeignFutureCompleteI64
typealias UniffiForeignFutureResultF32 = CPointer<cinterop.UniffiForeignFutureResultF32>

var UniffiForeignFutureResultF32.returnValue: Float
    get() = pointed.returnValue
    set(value) {
        pointed.returnValue = value
    }

var UniffiForeignFutureResultF32.callStatus: UniffiRustCallStatusByValue
    get() = pointed.callStatus.readValue()
    set(value) {
        value.write(pointed.callStatus.rawPtr)
    }

fun UniffiForeignFutureResultF32.uniffiSetValue(other: UniffiForeignFutureResultF32) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

fun UniffiForeignFutureResultF32.uniffiSetValue(other: UniffiForeignFutureResultF32UniffiByValue) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

typealias UniffiForeignFutureResultF32UniffiByValue = CValue<cinterop.UniffiForeignFutureResultF32>

fun UniffiForeignFutureResultF32UniffiByValue(
    returnValue: Float,
    callStatus: UniffiRustCallStatusByValue,
): UniffiForeignFutureResultF32UniffiByValue =
    cValue<cinterop.UniffiForeignFutureResultF32> {
        this.returnValue = returnValue

        callStatus.write(this.callStatus.rawPtr)
    }

val UniffiForeignFutureResultF32UniffiByValue.returnValue: Float
    get() =
        useContents {
            returnValue
        }

val UniffiForeignFutureResultF32UniffiByValue.callStatus: UniffiRustCallStatusByValue
    get() = useContents { callStatus.readValue() }

typealias UniffiForeignFutureCompleteF32 = cinterop.UniffiForeignFutureCompleteF32
typealias UniffiForeignFutureResultF64 = CPointer<cinterop.UniffiForeignFutureResultF64>

var UniffiForeignFutureResultF64.returnValue: Double
    get() = pointed.returnValue
    set(value) {
        pointed.returnValue = value
    }

var UniffiForeignFutureResultF64.callStatus: UniffiRustCallStatusByValue
    get() = pointed.callStatus.readValue()
    set(value) {
        value.write(pointed.callStatus.rawPtr)
    }

fun UniffiForeignFutureResultF64.uniffiSetValue(other: UniffiForeignFutureResultF64) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

fun UniffiForeignFutureResultF64.uniffiSetValue(other: UniffiForeignFutureResultF64UniffiByValue) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

typealias UniffiForeignFutureResultF64UniffiByValue = CValue<cinterop.UniffiForeignFutureResultF64>

fun UniffiForeignFutureResultF64UniffiByValue(
    returnValue: Double,
    callStatus: UniffiRustCallStatusByValue,
): UniffiForeignFutureResultF64UniffiByValue =
    cValue<cinterop.UniffiForeignFutureResultF64> {
        this.returnValue = returnValue

        callStatus.write(this.callStatus.rawPtr)
    }

val UniffiForeignFutureResultF64UniffiByValue.returnValue: Double
    get() =
        useContents {
            returnValue
        }

val UniffiForeignFutureResultF64UniffiByValue.callStatus: UniffiRustCallStatusByValue
    get() = useContents { callStatus.readValue() }

typealias UniffiForeignFutureCompleteF64 = cinterop.UniffiForeignFutureCompleteF64
typealias UniffiForeignFutureResultPointer = CPointer<cinterop.UniffiForeignFutureResultPointer>

var UniffiForeignFutureResultPointer.returnValue: Pointer?
    get() = pointed.returnValue?.let { Pointer(it) }
    set(value) {
        pointed.returnValue = value?.inner
    }

var UniffiForeignFutureResultPointer.callStatus: UniffiRustCallStatusByValue
    get() = pointed.callStatus.readValue()
    set(value) {
        value.write(pointed.callStatus.rawPtr)
    }

fun UniffiForeignFutureResultPointer.uniffiSetValue(other: UniffiForeignFutureResultPointer) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

fun UniffiForeignFutureResultPointer.uniffiSetValue(other: UniffiForeignFutureResultPointerUniffiByValue) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

typealias UniffiForeignFutureResultPointerUniffiByValue = CValue<cinterop.UniffiForeignFutureResultPointer>

fun UniffiForeignFutureResultPointerUniffiByValue(
    returnValue: Pointer?,
    callStatus: UniffiRustCallStatusByValue,
): UniffiForeignFutureResultPointerUniffiByValue =
    cValue<cinterop.UniffiForeignFutureResultPointer> {
        this.returnValue = returnValue?.inner

        callStatus.write(this.callStatus.rawPtr)
    }

val UniffiForeignFutureResultPointerUniffiByValue.returnValue: Pointer?
    get() =
        useContents {
            returnValue
        }?.let { Pointer(it) }

val UniffiForeignFutureResultPointerUniffiByValue.callStatus: UniffiRustCallStatusByValue
    get() = useContents { callStatus.readValue() }

typealias UniffiForeignFutureCompletePointer = cinterop.UniffiForeignFutureCompletePointer
typealias UniffiForeignFutureResultRustBuffer = CPointer<cinterop.UniffiForeignFutureResultRustBuffer>

var UniffiForeignFutureResultRustBuffer.returnValue: RustBufferByValue
    get() = pointed.returnValue.readValue()
    set(value) {
        value.write(pointed.returnValue.rawPtr)
    }

var UniffiForeignFutureResultRustBuffer.callStatus: UniffiRustCallStatusByValue
    get() = pointed.callStatus.readValue()
    set(value) {
        value.write(pointed.callStatus.rawPtr)
    }

fun UniffiForeignFutureResultRustBuffer.uniffiSetValue(other: UniffiForeignFutureResultRustBuffer) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

fun UniffiForeignFutureResultRustBuffer.uniffiSetValue(other: UniffiForeignFutureResultRustBufferUniffiByValue) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

typealias UniffiForeignFutureResultRustBufferUniffiByValue = CValue<cinterop.UniffiForeignFutureResultRustBuffer>

fun UniffiForeignFutureResultRustBufferUniffiByValue(
    returnValue: RustBufferByValue,
    callStatus: UniffiRustCallStatusByValue,
): UniffiForeignFutureResultRustBufferUniffiByValue =
    cValue<cinterop.UniffiForeignFutureResultRustBuffer> {
        returnValue.write(this.returnValue.rawPtr)

        callStatus.write(this.callStatus.rawPtr)
    }

val UniffiForeignFutureResultRustBufferUniffiByValue.returnValue: RustBufferByValue
    get() = useContents { returnValue.readValue() }

val UniffiForeignFutureResultRustBufferUniffiByValue.callStatus: UniffiRustCallStatusByValue
    get() = useContents { callStatus.readValue() }

typealias UniffiForeignFutureCompleteRustBuffer = cinterop.UniffiForeignFutureCompleteRustBuffer
typealias UniffiForeignFutureResultVoid = CPointer<cinterop.UniffiForeignFutureResultVoid>

var UniffiForeignFutureResultVoid.callStatus: UniffiRustCallStatusByValue
    get() = pointed.callStatus.readValue()
    set(value) {
        value.write(pointed.callStatus.rawPtr)
    }

fun UniffiForeignFutureResultVoid.uniffiSetValue(other: UniffiForeignFutureResultVoid) {
    callStatus = other.callStatus
}

fun UniffiForeignFutureResultVoid.uniffiSetValue(other: UniffiForeignFutureResultVoidUniffiByValue) {
    callStatus = other.callStatus
}

typealias UniffiForeignFutureResultVoidUniffiByValue = CValue<cinterop.UniffiForeignFutureResultVoid>

fun UniffiForeignFutureResultVoidUniffiByValue(callStatus: UniffiRustCallStatusByValue): UniffiForeignFutureResultVoidUniffiByValue =
    cValue<cinterop.UniffiForeignFutureResultVoid> {
        callStatus.write(this.callStatus.rawPtr)
    }

val UniffiForeignFutureResultVoidUniffiByValue.callStatus: UniffiRustCallStatusByValue
    get() = useContents { callStatus.readValue() }

typealias UniffiForeignFutureCompleteVoid = cinterop.UniffiForeignFutureCompleteVoid
