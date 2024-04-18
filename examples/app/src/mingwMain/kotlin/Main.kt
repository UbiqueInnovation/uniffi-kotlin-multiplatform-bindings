/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.uniffi.examples.app

import io.gitlab.trixnity.uniffi.examples.app.windows.Window
import io.gitlab.trixnity.uniffi.examples.todolist.TodoList

@OptIn(ExperimentalStdlibApi::class)
fun main() = ContentWindow(TodoList()).use { window ->
    window.open()
    Window.runMessageLoop()
}
