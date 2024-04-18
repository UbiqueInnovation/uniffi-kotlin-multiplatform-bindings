/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.uniffi.examples.app.windows.controls

import io.gitlab.trixnity.uniffi.examples.app.windows.Window
import platform.windows.*

class TextControl(
    parent: Window,
    text: String,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
) : Window(parent) {
    override val windowClassName: String = "STATIC"
    override val windowStyle: Int = WS_VISIBLE or WS_CHILD

    init {
        title = text
        this.x = x
        this.y = y
        this.width = width
        this.height = height
    }
}
