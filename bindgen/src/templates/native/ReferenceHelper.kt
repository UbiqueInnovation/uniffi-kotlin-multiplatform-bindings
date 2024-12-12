
actual class ByteByReference(val inner: CPointer<ByteVar>) {
    actual fun setValue(value: Byte) {
        this.inner.pointed.value = value
    }
    actual fun getValue() : Byte {
        return this.inner.pointed.value
    }
}

actual class DoubleByReference(val inner: CPointer<DoubleVar>) {
    actual fun setValue(value: Double) {
        this.inner.pointed.value = value
    }

    actual fun getValue() : Double {
        return this.inner.pointed.value
    }
}

actual class FloatByReference(val inner: CPointer<FloatVar>) {
    actual fun setValue(value: Float) {
        this.inner.pointed.value = value
    }
    actual fun getValue() : Float {
        return this.inner.pointed.value
    }
}



actual class IntByReference(val inner: CPointer<IntVar>) {
    actual fun setValue(value: Int) {
        this.inner.pointed.value = value
    }
    actual fun getValue() : Int {
        return this.inner.pointed.value
    }
}

actual class LongByReference(val inner: CPointer<LongVar>) {
    actual fun setValue(value: Long) {
        this.inner.pointed.value = value
    }
    actual fun getValue() : Long {
        return this.inner.pointed.value
    }
}

actual class PointerByReference(val inner: CPointerVarOf<CPointer<out CPointed>>) {
    actual fun setValue(value: Pointer) {
        this.inner.value = value.inner
    }
    actual fun getValue() : Pointer {
        return Pointer(this.inner.value ?: interpretCPointer(nativeNullPtr)!!)
    }
}


actual class ShortByReference(val inner: CPointer<ShortVar>) {
    actual fun setValue(value: Short) {
        this.inner.pointed.value = value
    }
    actual fun getValue() : Short {
        return this.inner.pointed.value
    }
}
