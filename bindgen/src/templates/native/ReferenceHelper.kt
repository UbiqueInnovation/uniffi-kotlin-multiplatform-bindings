actual typealias ByteByReference = CPointer<ByteVar>
fun ByteByReference.setValue(value: Byte) {
    this.pointed.value = value
}
fun ByteByReference.getValue() : Byte {
    return this.pointed.value
}

actual typealias DoubleByReference = CPointer<DoubleVar>

fun DoubleByReference.setValue(value: Double) {
    this.pointed.value = value
}
fun DoubleByReference.getValue() : Double {
    return this.pointed.value
}

actual typealias FloatByReference = CPointer<FloatVar>

fun FloatByReference.setValue(value: Float) {
    this.pointed.value = value
}
fun FloatByReference.getValue() : Float {
    return this.pointed.value
}

actual typealias IntByReference = CPointer<IntVar>

fun IntByReference.setValue(value: Int) {
    this.pointed.value = value
}
fun IntByReference.getValue() : Int {
    return this.pointed.value
}

actual typealias LongByReference = CPointer<LongVar>
fun LongByReference.setValue(value: Long) {
    this.pointed.value = value
}
fun LongByReference.getValue() : Long {
    return this.pointed.value
}

actual typealias PointerByReference = CPointer<out CPointed>


actual typealias ShortByReference = CPointer<ShortVar>