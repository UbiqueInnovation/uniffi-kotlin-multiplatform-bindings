/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import kotlinx.cinterop.*
import rustBindings.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration

actual object CargoOnlyLibrary {
    actual fun getHelloWorld(): String = CargoOnlyLibrary_getHelloWorld()!!.toKStringFromUtf8()

    actual suspend fun wait(duration: Duration) {
        fun resumeContinuation(continuation: COpaquePointer?) {
            val stableRef = continuation!!.asStableRef<Continuation<Unit>>()
            stableRef.get().resume(Unit)
            stableRef.dispose()
        }

        if (duration.isNegative()) return
        return suspendCoroutine { continuation ->
            duration.toComponents { seconds, nanoseconds ->
                CargoOnlyLibrary_wait(
                    seconds.toULong(),
                    nanoseconds.toUInt(),
                    StableRef.create(continuation).asCPointer(),
                    staticCFunction(::resumeContinuation),
                )
            }
        }
    }

    actual val optLevel: Int
        get() = CargoOnlyLibrary_getOptLevel()

    actual val features: Array<String>
        get() = CargoOnlyLibrary_getFeatures()!!
            .toKStringFromUtf8()
            .split(',')
            .filter(String::isNotEmpty)
            .toTypedArray()
}
