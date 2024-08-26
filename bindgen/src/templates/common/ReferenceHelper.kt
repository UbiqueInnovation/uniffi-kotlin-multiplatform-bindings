expect class ByteByReference {
  fun setValue(value: Byte)
  fun getValue(): Byte
}

expect class DoubleByReference {
  fun setValue(value: Double)
  fun getValue(): Double
}

expect class FloatByReference {
  fun setValue(value: Float)
  fun getValue(): Float
}

expect class IntByReference {
  fun setValue(value: Int)
  fun getValue(): Int
}

expect class LongByReference {
  fun setValue(value: Long)
  fun getValue(): Long
}

expect class PointerByReference {
  fun setValue(value: Pointer)
  fun getValue(): Pointer
}

expect class ShortByReference {
  fun setValue(value: Short)
  fun getValue(): Short
}