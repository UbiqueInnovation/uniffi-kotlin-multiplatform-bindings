/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.uniffi.examples.app.windows.controls

import io.gitlab.trixnity.uniffi.examples.app.windows.Window
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.sizeOf
import platform.windows.*

abstract class CommonControl(parent: Window) : Window(parent) {
    @OptIn(ExperimentalForeignApi::class)
    companion object {
        protected fun loadCommonControls(controlClass: Int) {
            memScoped {
                val initCommonControls = cValue<INITCOMMONCONTROLSEX> {
                    dwSize = sizeOf<INITCOMMONCONTROLSEX>().toUInt()
                    dwICC = controlClass.toUInt()
                }
                InitCommonControlsEx(initCommonControls.ptr)
            }
        }
    }
}
