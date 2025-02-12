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

typealias UniffiForeignFuture = CPointer<uniffi_runtime.cinterop.UniffiForeignFuture>

var UniffiForeignFuture.handle: Long
    get() = pointed.handle
    set(value) {
        pointed.handle = value
    }

var UniffiForeignFuture.free: Any?
    get() = pointed.free
    set(value) {
        pointed.free = value as UniffiForeignFutureFree?
    }

fun UniffiForeignFuture.uniffiSetValue(other: UniffiForeignFuture) {
    handle = other.handle
    free = other.free
}

fun UniffiForeignFuture.uniffiSetValue(other: UniffiForeignFutureUniffiByValue) {
    handle = other.handle
    free = other.free
}

typealias UniffiForeignFutureUniffiByValue = CValue<uniffi_runtime.cinterop.UniffiForeignFuture>

fun UniffiForeignFutureUniffiByValue(
    handle: Long,
    free: Any?,
): UniffiForeignFutureUniffiByValue =
    cValue<uniffi_runtime.cinterop.UniffiForeignFuture> {
        this.handle = handle

        this.free = free as UniffiForeignFutureFree?
    }

val UniffiForeignFutureUniffiByValue.handle: Long
    get() = useContents { handle }

val UniffiForeignFutureUniffiByValue.free: Any?
    get() = useContents { free }

typealias UniffiForeignFutureStructU8 = CPointer<uniffi_runtime.cinterop.UniffiForeignFutureStructU8>

var UniffiForeignFutureStructU8.returnValue: Byte
    get() = pointed.returnValue
    set(value) {
        pointed.returnValue = value
    }

var UniffiForeignFutureStructU8.callStatus: UniffiRustCallStatusByValue
    get() = pointed.callStatus.readValue()
    set(value) {
        value.write(pointed.callStatus.rawPtr)
    }

fun UniffiForeignFutureStructU8.uniffiSetValue(other: UniffiForeignFutureStructU8) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

fun UniffiForeignFutureStructU8.uniffiSetValue(other: UniffiForeignFutureStructU8UniffiByValue) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

typealias UniffiForeignFutureStructU8UniffiByValue = CValue<uniffi_runtime.cinterop.UniffiForeignFutureStructU8>

fun UniffiForeignFutureStructU8UniffiByValue(
    returnValue: Byte,
    callStatus: UniffiRustCallStatusByValue,
): UniffiForeignFutureStructU8UniffiByValue =
    cValue<uniffi_runtime.cinterop.UniffiForeignFutureStructU8> {
        this.returnValue = returnValue

        callStatus.write(this.callStatus.rawPtr)
    }

val UniffiForeignFutureStructU8UniffiByValue.returnValue: Byte
    get() =
        useContents {
            returnValue
        }

val UniffiForeignFutureStructU8UniffiByValue.callStatus: UniffiRustCallStatusByValue
    get() = useContents { callStatus.readValue() }

typealias UniffiForeignFutureCompleteU8 = uniffi_runtime.cinterop.UniffiForeignFutureCompleteU8
typealias UniffiForeignFutureStructI8 = CPointer<uniffi_runtime.cinterop.UniffiForeignFutureStructI8>

var UniffiForeignFutureStructI8.returnValue: Byte
    get() = pointed.returnValue
    set(value) {
        pointed.returnValue = value
    }

var UniffiForeignFutureStructI8.callStatus: UniffiRustCallStatusByValue
    get() = pointed.callStatus.readValue()
    set(value) {
        value.write(pointed.callStatus.rawPtr)
    }

fun UniffiForeignFutureStructI8.uniffiSetValue(other: UniffiForeignFutureStructI8) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

fun UniffiForeignFutureStructI8.uniffiSetValue(other: UniffiForeignFutureStructI8UniffiByValue) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

typealias UniffiForeignFutureStructI8UniffiByValue = CValue<uniffi_runtime.cinterop.UniffiForeignFutureStructI8>

fun UniffiForeignFutureStructI8UniffiByValue(
    returnValue: Byte,
    callStatus: UniffiRustCallStatusByValue,
): UniffiForeignFutureStructI8UniffiByValue =
    cValue<uniffi_runtime.cinterop.UniffiForeignFutureStructI8> {
        this.returnValue = returnValue

        callStatus.write(this.callStatus.rawPtr)
    }

val UniffiForeignFutureStructI8UniffiByValue.returnValue: Byte
    get() =
        useContents {
            returnValue
        }

val UniffiForeignFutureStructI8UniffiByValue.callStatus: UniffiRustCallStatusByValue
    get() = useContents { callStatus.readValue() }

typealias UniffiForeignFutureCompleteI8 = uniffi_runtime.cinterop.UniffiForeignFutureCompleteI8
typealias UniffiForeignFutureStructU16 = CPointer<uniffi_runtime.cinterop.UniffiForeignFutureStructU16>

var UniffiForeignFutureStructU16.returnValue: Short
    get() = pointed.returnValue
    set(value) {
        pointed.returnValue = value
    }

var UniffiForeignFutureStructU16.callStatus: UniffiRustCallStatusByValue
    get() = pointed.callStatus.readValue()
    set(value) {
        value.write(pointed.callStatus.rawPtr)
    }

fun UniffiForeignFutureStructU16.uniffiSetValue(other: UniffiForeignFutureStructU16) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

fun UniffiForeignFutureStructU16.uniffiSetValue(other: UniffiForeignFutureStructU16UniffiByValue) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

typealias UniffiForeignFutureStructU16UniffiByValue = CValue<uniffi_runtime.cinterop.UniffiForeignFutureStructU16>

fun UniffiForeignFutureStructU16UniffiByValue(
    returnValue: Short,
    callStatus: UniffiRustCallStatusByValue,
): UniffiForeignFutureStructU16UniffiByValue =
    cValue<uniffi_runtime.cinterop.UniffiForeignFutureStructU16> {
        this.returnValue = returnValue

        callStatus.write(this.callStatus.rawPtr)
    }

val UniffiForeignFutureStructU16UniffiByValue.returnValue: Short
    get() =
        useContents {
            returnValue
        }

val UniffiForeignFutureStructU16UniffiByValue.callStatus: UniffiRustCallStatusByValue
    get() = useContents { callStatus.readValue() }

typealias UniffiForeignFutureCompleteU16 = uniffi_runtime.cinterop.UniffiForeignFutureCompleteU16
typealias UniffiForeignFutureStructI16 = CPointer<uniffi_runtime.cinterop.UniffiForeignFutureStructI16>

var UniffiForeignFutureStructI16.returnValue: Short
    get() = pointed.returnValue
    set(value) {
        pointed.returnValue = value
    }

var UniffiForeignFutureStructI16.callStatus: UniffiRustCallStatusByValue
    get() = pointed.callStatus.readValue()
    set(value) {
        value.write(pointed.callStatus.rawPtr)
    }

fun UniffiForeignFutureStructI16.uniffiSetValue(other: UniffiForeignFutureStructI16) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

fun UniffiForeignFutureStructI16.uniffiSetValue(other: UniffiForeignFutureStructI16UniffiByValue) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

typealias UniffiForeignFutureStructI16UniffiByValue = CValue<uniffi_runtime.cinterop.UniffiForeignFutureStructI16>

fun UniffiForeignFutureStructI16UniffiByValue(
    returnValue: Short,
    callStatus: UniffiRustCallStatusByValue,
): UniffiForeignFutureStructI16UniffiByValue =
    cValue<uniffi_runtime.cinterop.UniffiForeignFutureStructI16> {
        this.returnValue = returnValue

        callStatus.write(this.callStatus.rawPtr)
    }

val UniffiForeignFutureStructI16UniffiByValue.returnValue: Short
    get() =
        useContents {
            returnValue
        }

val UniffiForeignFutureStructI16UniffiByValue.callStatus: UniffiRustCallStatusByValue
    get() = useContents { callStatus.readValue() }

typealias UniffiForeignFutureCompleteI16 = uniffi_runtime.cinterop.UniffiForeignFutureCompleteI16
typealias UniffiForeignFutureStructU32 = CPointer<uniffi_runtime.cinterop.UniffiForeignFutureStructU32>

var UniffiForeignFutureStructU32.returnValue: Int
    get() = pointed.returnValue
    set(value) {
        pointed.returnValue = value
    }

var UniffiForeignFutureStructU32.callStatus: UniffiRustCallStatusByValue
    get() = pointed.callStatus.readValue()
    set(value) {
        value.write(pointed.callStatus.rawPtr)
    }

fun UniffiForeignFutureStructU32.uniffiSetValue(other: UniffiForeignFutureStructU32) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

fun UniffiForeignFutureStructU32.uniffiSetValue(other: UniffiForeignFutureStructU32UniffiByValue) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

typealias UniffiForeignFutureStructU32UniffiByValue = CValue<uniffi_runtime.cinterop.UniffiForeignFutureStructU32>

fun UniffiForeignFutureStructU32UniffiByValue(
    returnValue: Int,
    callStatus: UniffiRustCallStatusByValue,
): UniffiForeignFutureStructU32UniffiByValue =
    cValue<uniffi_runtime.cinterop.UniffiForeignFutureStructU32> {
        this.returnValue = returnValue

        callStatus.write(this.callStatus.rawPtr)
    }

val UniffiForeignFutureStructU32UniffiByValue.returnValue: Int
    get() =
        useContents {
            returnValue
        }

val UniffiForeignFutureStructU32UniffiByValue.callStatus: UniffiRustCallStatusByValue
    get() = useContents { callStatus.readValue() }

typealias UniffiForeignFutureCompleteU32 = uniffi_runtime.cinterop.UniffiForeignFutureCompleteU32
typealias UniffiForeignFutureStructI32 = CPointer<uniffi_runtime.cinterop.UniffiForeignFutureStructI32>

var UniffiForeignFutureStructI32.returnValue: Int
    get() = pointed.returnValue
    set(value) {
        pointed.returnValue = value
    }

var UniffiForeignFutureStructI32.callStatus: UniffiRustCallStatusByValue
    get() = pointed.callStatus.readValue()
    set(value) {
        value.write(pointed.callStatus.rawPtr)
    }

fun UniffiForeignFutureStructI32.uniffiSetValue(other: UniffiForeignFutureStructI32) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

fun UniffiForeignFutureStructI32.uniffiSetValue(other: UniffiForeignFutureStructI32UniffiByValue) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

typealias UniffiForeignFutureStructI32UniffiByValue = CValue<uniffi_runtime.cinterop.UniffiForeignFutureStructI32>

fun UniffiForeignFutureStructI32UniffiByValue(
    returnValue: Int,
    callStatus: UniffiRustCallStatusByValue,
): UniffiForeignFutureStructI32UniffiByValue =
    cValue<uniffi_runtime.cinterop.UniffiForeignFutureStructI32> {
        this.returnValue = returnValue

        callStatus.write(this.callStatus.rawPtr)
    }

val UniffiForeignFutureStructI32UniffiByValue.returnValue: Int
    get() =
        useContents {
            returnValue
        }

val UniffiForeignFutureStructI32UniffiByValue.callStatus: UniffiRustCallStatusByValue
    get() = useContents { callStatus.readValue() }

typealias UniffiForeignFutureCompleteI32 = uniffi_runtime.cinterop.UniffiForeignFutureCompleteI32
typealias UniffiForeignFutureStructU64 = CPointer<uniffi_runtime.cinterop.UniffiForeignFutureStructU64>

var UniffiForeignFutureStructU64.returnValue: Long
    get() = pointed.returnValue
    set(value) {
        pointed.returnValue = value
    }

var UniffiForeignFutureStructU64.callStatus: UniffiRustCallStatusByValue
    get() = pointed.callStatus.readValue()
    set(value) {
        value.write(pointed.callStatus.rawPtr)
    }

fun UniffiForeignFutureStructU64.uniffiSetValue(other: UniffiForeignFutureStructU64) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

fun UniffiForeignFutureStructU64.uniffiSetValue(other: UniffiForeignFutureStructU64UniffiByValue) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

typealias UniffiForeignFutureStructU64UniffiByValue = CValue<uniffi_runtime.cinterop.UniffiForeignFutureStructU64>

fun UniffiForeignFutureStructU64UniffiByValue(
    returnValue: Long,
    callStatus: UniffiRustCallStatusByValue,
): UniffiForeignFutureStructU64UniffiByValue =
    cValue<uniffi_runtime.cinterop.UniffiForeignFutureStructU64> {
        this.returnValue = returnValue

        callStatus.write(this.callStatus.rawPtr)
    }

val UniffiForeignFutureStructU64UniffiByValue.returnValue: Long
    get() =
        useContents {
            returnValue
        }

val UniffiForeignFutureStructU64UniffiByValue.callStatus: UniffiRustCallStatusByValue
    get() = useContents { callStatus.readValue() }

typealias UniffiForeignFutureCompleteU64 = uniffi_runtime.cinterop.UniffiForeignFutureCompleteU64
typealias UniffiForeignFutureStructI64 = CPointer<uniffi_runtime.cinterop.UniffiForeignFutureStructI64>

var UniffiForeignFutureStructI64.returnValue: Long
    get() = pointed.returnValue
    set(value) {
        pointed.returnValue = value
    }

var UniffiForeignFutureStructI64.callStatus: UniffiRustCallStatusByValue
    get() = pointed.callStatus.readValue()
    set(value) {
        value.write(pointed.callStatus.rawPtr)
    }

fun UniffiForeignFutureStructI64.uniffiSetValue(other: UniffiForeignFutureStructI64) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

fun UniffiForeignFutureStructI64.uniffiSetValue(other: UniffiForeignFutureStructI64UniffiByValue) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

typealias UniffiForeignFutureStructI64UniffiByValue = CValue<uniffi_runtime.cinterop.UniffiForeignFutureStructI64>

fun UniffiForeignFutureStructI64UniffiByValue(
    returnValue: Long,
    callStatus: UniffiRustCallStatusByValue,
): UniffiForeignFutureStructI64UniffiByValue =
    cValue<uniffi_runtime.cinterop.UniffiForeignFutureStructI64> {
        this.returnValue = returnValue

        callStatus.write(this.callStatus.rawPtr)
    }

val UniffiForeignFutureStructI64UniffiByValue.returnValue: Long
    get() =
        useContents {
            returnValue
        }

val UniffiForeignFutureStructI64UniffiByValue.callStatus: UniffiRustCallStatusByValue
    get() = useContents { callStatus.readValue() }

typealias UniffiForeignFutureCompleteI64 = uniffi_runtime.cinterop.UniffiForeignFutureCompleteI64
typealias UniffiForeignFutureStructF32 = CPointer<uniffi_runtime.cinterop.UniffiForeignFutureStructF32>

var UniffiForeignFutureStructF32.returnValue: Float
    get() = pointed.returnValue
    set(value) {
        pointed.returnValue = value
    }

var UniffiForeignFutureStructF32.callStatus: UniffiRustCallStatusByValue
    get() = pointed.callStatus.readValue()
    set(value) {
        value.write(pointed.callStatus.rawPtr)
    }

fun UniffiForeignFutureStructF32.uniffiSetValue(other: UniffiForeignFutureStructF32) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

fun UniffiForeignFutureStructF32.uniffiSetValue(other: UniffiForeignFutureStructF32UniffiByValue) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

typealias UniffiForeignFutureStructF32UniffiByValue = CValue<uniffi_runtime.cinterop.UniffiForeignFutureStructF32>

fun UniffiForeignFutureStructF32UniffiByValue(
    returnValue: Float,
    callStatus: UniffiRustCallStatusByValue,
): UniffiForeignFutureStructF32UniffiByValue =
    cValue<uniffi_runtime.cinterop.UniffiForeignFutureStructF32> {
        this.returnValue = returnValue

        callStatus.write(this.callStatus.rawPtr)
    }

val UniffiForeignFutureStructF32UniffiByValue.returnValue: Float
    get() =
        useContents {
            returnValue
        }

val UniffiForeignFutureStructF32UniffiByValue.callStatus: UniffiRustCallStatusByValue
    get() = useContents { callStatus.readValue() }

typealias UniffiForeignFutureCompleteF32 = uniffi_runtime.cinterop.UniffiForeignFutureCompleteF32
typealias UniffiForeignFutureStructF64 = CPointer<uniffi_runtime.cinterop.UniffiForeignFutureStructF64>

var UniffiForeignFutureStructF64.returnValue: Double
    get() = pointed.returnValue
    set(value) {
        pointed.returnValue = value
    }

var UniffiForeignFutureStructF64.callStatus: UniffiRustCallStatusByValue
    get() = pointed.callStatus.readValue()
    set(value) {
        value.write(pointed.callStatus.rawPtr)
    }

fun UniffiForeignFutureStructF64.uniffiSetValue(other: UniffiForeignFutureStructF64) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

fun UniffiForeignFutureStructF64.uniffiSetValue(other: UniffiForeignFutureStructF64UniffiByValue) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

typealias UniffiForeignFutureStructF64UniffiByValue = CValue<uniffi_runtime.cinterop.UniffiForeignFutureStructF64>

fun UniffiForeignFutureStructF64UniffiByValue(
    returnValue: Double,
    callStatus: UniffiRustCallStatusByValue,
): UniffiForeignFutureStructF64UniffiByValue =
    cValue<uniffi_runtime.cinterop.UniffiForeignFutureStructF64> {
        this.returnValue = returnValue

        callStatus.write(this.callStatus.rawPtr)
    }

val UniffiForeignFutureStructF64UniffiByValue.returnValue: Double
    get() =
        useContents {
            returnValue
        }

val UniffiForeignFutureStructF64UniffiByValue.callStatus: UniffiRustCallStatusByValue
    get() = useContents { callStatus.readValue() }

typealias UniffiForeignFutureCompleteF64 = uniffi_runtime.cinterop.UniffiForeignFutureCompleteF64
typealias UniffiForeignFutureStructPointer = CPointer<uniffi_runtime.cinterop.UniffiForeignFutureStructPointer>

var UniffiForeignFutureStructPointer.returnValue: Pointer?
    get() = pointed.returnValue?.let { Pointer(it) }
    set(value) {
        pointed.returnValue = value?.inner
    }

var UniffiForeignFutureStructPointer.callStatus: UniffiRustCallStatusByValue
    get() = pointed.callStatus.readValue()
    set(value) {
        value.write(pointed.callStatus.rawPtr)
    }

fun UniffiForeignFutureStructPointer.uniffiSetValue(other: UniffiForeignFutureStructPointer) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

fun UniffiForeignFutureStructPointer.uniffiSetValue(other: UniffiForeignFutureStructPointerUniffiByValue) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

typealias UniffiForeignFutureStructPointerUniffiByValue = CValue<uniffi_runtime.cinterop.UniffiForeignFutureStructPointer>

fun UniffiForeignFutureStructPointerUniffiByValue(
    returnValue: Pointer?,
    callStatus: UniffiRustCallStatusByValue,
): UniffiForeignFutureStructPointerUniffiByValue =
    cValue<uniffi_runtime.cinterop.UniffiForeignFutureStructPointer> {
        this.returnValue = returnValue?.inner

        callStatus.write(this.callStatus.rawPtr)
    }

val UniffiForeignFutureStructPointerUniffiByValue.returnValue: Pointer?
    get() =
        useContents {
            returnValue
        }?.let { Pointer(it) }

val UniffiForeignFutureStructPointerUniffiByValue.callStatus: UniffiRustCallStatusByValue
    get() = useContents { callStatus.readValue() }

typealias UniffiForeignFutureCompletePointer = uniffi_runtime.cinterop.UniffiForeignFutureCompletePointer
typealias UniffiForeignFutureStructRustBuffer = CPointer<uniffi_runtime.cinterop.UniffiForeignFutureStructRustBuffer>

var UniffiForeignFutureStructRustBuffer.returnValue: RustBufferByValue
    get() = pointed.returnValue.readValue()
    set(value) {
        value.write(pointed.returnValue.rawPtr)
    }

var UniffiForeignFutureStructRustBuffer.callStatus: UniffiRustCallStatusByValue
    get() = pointed.callStatus.readValue()
    set(value) {
        value.write(pointed.callStatus.rawPtr)
    }

fun UniffiForeignFutureStructRustBuffer.uniffiSetValue(other: UniffiForeignFutureStructRustBuffer) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

fun UniffiForeignFutureStructRustBuffer.uniffiSetValue(other: UniffiForeignFutureStructRustBufferUniffiByValue) {
    returnValue = other.returnValue
    callStatus = other.callStatus
}

typealias UniffiForeignFutureStructRustBufferUniffiByValue = CValue<uniffi_runtime.cinterop.UniffiForeignFutureStructRustBuffer>

fun UniffiForeignFutureStructRustBufferUniffiByValue(
    returnValue: RustBufferByValue,
    callStatus: UniffiRustCallStatusByValue,
): UniffiForeignFutureStructRustBufferUniffiByValue =
    cValue<uniffi_runtime.cinterop.UniffiForeignFutureStructRustBuffer> {
        returnValue.write(this.returnValue.rawPtr)

        callStatus.write(this.callStatus.rawPtr)
    }

val UniffiForeignFutureStructRustBufferUniffiByValue.returnValue: RustBufferByValue
    get() = useContents { returnValue.readValue() }

val UniffiForeignFutureStructRustBufferUniffiByValue.callStatus: UniffiRustCallStatusByValue
    get() = useContents { callStatus.readValue() }

typealias UniffiForeignFutureCompleteRustBuffer = uniffi_runtime.cinterop.UniffiForeignFutureCompleteRustBuffer
typealias UniffiForeignFutureStructVoid = CPointer<uniffi_runtime.cinterop.UniffiForeignFutureStructVoid>

var UniffiForeignFutureStructVoid.callStatus: UniffiRustCallStatusByValue
    get() = pointed.callStatus.readValue()
    set(value) {
        value.write(pointed.callStatus.rawPtr)
    }

fun UniffiForeignFutureStructVoid.uniffiSetValue(other: UniffiForeignFutureStructVoid) {
    callStatus = other.callStatus
}

fun UniffiForeignFutureStructVoid.uniffiSetValue(other: UniffiForeignFutureStructVoidUniffiByValue) {
    callStatus = other.callStatus
}

typealias UniffiForeignFutureStructVoidUniffiByValue = CValue<uniffi_runtime.cinterop.UniffiForeignFutureStructVoid>

fun UniffiForeignFutureStructVoidUniffiByValue(callStatus: UniffiRustCallStatusByValue): UniffiForeignFutureStructVoidUniffiByValue =
    cValue<uniffi_runtime.cinterop.UniffiForeignFutureStructVoid> {
        callStatus.write(this.callStatus.rawPtr)
    }

val UniffiForeignFutureStructVoidUniffiByValue.callStatus: UniffiRustCallStatusByValue
    get() = useContents { callStatus.readValue() }
