/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.uniffi.examples.app

import kotlinx.cinterop.*
import platform.posix.memcpy
import platform.windows.*

@OptIn(ExperimentalForeignApi::class)
class Win32Exception(errorCode: DWORD = GetLastError()) : Exception() {
    private val errorCodeMessage: String = if (errorCode == 0u) "$errorCode" else {
        memScoped {
            val stringBuffer = alloc<LPWSTRVar>()
            val stringLength = FormatMessageW(
                dwFlags = (FORMAT_MESSAGE_ALLOCATE_BUFFER or FORMAT_MESSAGE_FROM_SYSTEM or FORMAT_MESSAGE_IGNORE_INSERTS).toUInt(),
                lpSource = null,
                errorCode,
                dwLanguageId = LANG_SYSTEM_DEFAULT.toUInt(),
                lpBuffer = stringBuffer.ptr.reinterpret(),
                nSize = 0u,
                Arguments = null,
            )
            CharArray(stringLength.toInt()).apply {
                val pointerValue = stringBuffer.ptr.pointed.value!!
                usePinned {
                    memcpy(it.addressOf(0), pointerValue, (stringLength * sizeOf<WCHARVar>().toUInt()).toULong())
                }
                LocalFree(pointerValue)
            }.concatToString().trim()
        }
    }

    override val message = "$errorCodeMessage ($errorCode)"
}
