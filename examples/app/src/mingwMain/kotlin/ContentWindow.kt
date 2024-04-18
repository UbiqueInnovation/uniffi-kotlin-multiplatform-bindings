/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.uniffi.examples.app

import io.gitlab.trixnity.uniffi.examples.app.windows.Window
import io.gitlab.trixnity.uniffi.examples.app.windows.controls.ButtonControl
import io.gitlab.trixnity.uniffi.examples.app.windows.controls.EditControl
import io.gitlab.trixnity.uniffi.examples.app.windows.controls.ListControl
import io.gitlab.trixnity.uniffi.examples.app.windows.controls.TextControl
import io.gitlab.trixnity.uniffi.examples.arithmeticpm.add
import io.gitlab.trixnity.uniffi.examples.arithmeticpm.sub
import io.gitlab.trixnity.uniffi.examples.todolist.TodoEntry
import io.gitlab.trixnity.uniffi.examples.todolist.TodoList
import kotlinx.cinterop.ExperimentalForeignApi
import platform.windows.*

class ContentWindow(private val todoList: TodoList) : Window() {
    private var count = 0uL

    init {
        title = "TodoList App"
        width = 360
        height = 390
    }

    private fun updateData() {
        listControl.clear()
        todoList.getEntries().forEach { listControl.add(it.text) }
        textControl.title = count.toString()
    }


    private fun addTodo() {
        val todo = editControl.title
        editControl.title = ""

        todoList.addEntry(TodoEntry(todo))
        count = add(count, 1uL)

        updateData()
    }

    private fun removeTodo() {
        val todo = todoList.getEntries().getOrNull(listControl.selectedIndex)
        if (todo != null) {
            todoList.clearItem(todo.text)
            count = sub(count, 1uL)
            updateData()
        }
    }

    private val listControl = ListControl(this, "Still To Do", 10, 40, 330, 300)
    private val textControl = TextControl(this, "0", 10, 10, 50, 20)
    private val editControl = EditControl(this, 70, 10, 200, 20)
    private val buttonControl = ButtonControl(this, "Add", 280, 10, 60, 20)

    init {
        buttonControl.onClicked += ::addTodo
        editControl.onReturn += ::addTodo
        listControl.onBackspace += ::removeTodo
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun handleMessage(msg: UINT, wParam: WPARAM, lParam: LPARAM): LRESULT {
        when (msg) {
            WM_CLOSE.toUInt() -> DestroyWindow(windowHandle)
            WM_DESTROY.toUInt() -> PostQuitMessage(0)
        }
        return super.handleMessage(msg, wParam, lParam)
    }
}
