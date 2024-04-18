/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.uniffi.examples.app.windows.controls

import io.gitlab.trixnity.uniffi.examples.app.windows.Window
import platform.windows.*

class ButtonControl(
    parent: Window,
    text: String,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
) : Window(parent) {
    override val windowClassName: String = "BUTTON"
    override val windowStyle: Int = WS_VISIBLE or WS_CHILD or BS_PUSHBUTTON

    val onClicked = mutableSetOf<() -> Unit>()
    override fun handleCommand(notification: WPARAM) {
        if (notification == BN_CLICKED.toULong()) {
            onClicked.forEach { it() }
        }
    }

    init {
        title = text
        this.x = x
        this.y = y
        this.width = width
        this.height = height
    }
}
