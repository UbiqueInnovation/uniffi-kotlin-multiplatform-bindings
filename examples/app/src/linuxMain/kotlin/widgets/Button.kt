/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.uniffi.examples.app.widgets

import io.gitlab.trixnity.uniffi.examples.app.signalConnect
import kotlinx.cinterop.*
import org.gnome.gitlab.gtk.*

@OptIn(ExperimentalForeignApi::class)
open class Button(label: String? = null) : Widget() {
    final override val widget = gtk_button_new_with_label(label)!!
    open fun clicked() {}

    init {
        signalConnect(widget, "clicked", ::clicked)
    }
}
