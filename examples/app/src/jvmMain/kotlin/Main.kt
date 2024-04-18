/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.uniffi.examples.app

import io.gitlab.trixnity.uniffi.examples.todolist.TodoList
import javax.swing.JFrame
import javax.swing.SwingUtilities

fun main() {
    SwingUtilities.invokeLater {
        val frame = JFrame("TodoList App")
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.contentPane = ContentPanel(TodoList())
        frame.pack()
        frame.isVisible = true
    }
}
