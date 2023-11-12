import kotlinx.cinterop.*

// TODO remove suppress when https://youtrack.jetbrains.com/issue/KT-29819/New-rules-for-expect-actual-declarations-in-MPP is solved
@Suppress("ACTUAL_WITHOUT_EXPECT", "ACTUAL_TYPE_ALIAS_WITH_COMPLEX_SUBSTITUTION")
actual typealias ForeignCallback = CPointer<CFunction<(ULong, Int, UBytePointer?, Int, RustBufferPointer?) -> Int>>
