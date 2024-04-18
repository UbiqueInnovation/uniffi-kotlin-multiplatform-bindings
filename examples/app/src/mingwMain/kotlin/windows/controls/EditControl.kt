/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.uniffi.examples.app.windows.controls

import io.gitlab.trixnity.uniffi.examples.app.windows.Window
import platform.windows.*

class EditControl(
    parent: Window,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
) : Window(parent) {
    override val windowClassName: String = "EDIT"
    override val windowStyle: Int = WS_VISIBLE or WS_CHILD or WS_BORDER or ES_AUTOHSCROLL

    val onReturn = mutableSetOf<() -> Unit>()

    override fun handleMessage(msg: UINT, wParam: WPARAM, lParam: LPARAM): LRESULT {
        if (msg == WM_KEYDOWN.toUInt() && wParam == VK_RETURN.toULong()) {
            onReturn.forEach { it() }
            return 0
        }

        return super.handleMessage(msg, wParam, lParam)
    }

    init {
        this.x = x
        this.y = y
        this.width = width
        this.height = height
    }
}
