/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.uniffi.examples.app

import io.gitlab.trixnity.uniffi.examples.arithmeticpm.add
import io.gitlab.trixnity.uniffi.examples.arithmeticpm.sub
import io.gitlab.trixnity.uniffi.examples.todolist.TodoEntry
import io.gitlab.trixnity.uniffi.examples.todolist.TodoList
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.*

class ContentPanel(private val todoList: TodoList) : JPanel() {
    private val stride = 1UL
    private var clicked = 0UL

    private val clickedLabel = JLabel(clicked.toString())
    private val textField = JTextField()
    private val listView = JList<String>()
    private val listModel = DefaultListModel<String>()

    init {
        layout = GridBagLayout()
        initializeChildren()
    }

    private fun initializeChildren() {
        add(
            JPanel().apply {
                layout = BoxLayout(this, BoxLayout.X_AXIS)
                add(clickedLabel)
                add(textField)
            },
            GridBagConstraints().apply {
                fill = GridBagConstraints.HORIZONTAL
                gridy = 0
                weightx = 1.0
                weighty = 0.0
            },
        )
        add(
            JScrollPane(listView),
            GridBagConstraints().apply {
                fill = GridBagConstraints.BOTH
                gridy = 1
                weightx = 1.0
                weighty = 1.0
                gridheight = GridBagConstraints.REMAINDER
            },
        )

        textField.addActionListener {
            val entryText = "$clicked ${textField.text}"
            todoList.addEntry(TodoEntry(entryText))
            textField.text = ""
            clicked = add(clicked, stride)
            updateViews()
        }

        listView.addKeyListener(object : KeyListener {
            override fun keyTyped(e: KeyEvent?) = Unit
            override fun keyPressed(e: KeyEvent?) = Unit
            override fun keyReleased(e: KeyEvent?) {
                if (e?.keyCode == KeyEvent.VK_BACK_SPACE) {
                    val selectedIndices = listView.selectedIndices
                    val entries = selectedIndices.asList().mapNotNull { idx -> todoList.getEntries().getOrNull(idx) }
                    for (entry in entries) {
                        todoList.clearItem(entry.text)
                        clicked = sub(clicked, stride)
                    }
                    SwingUtilities.invokeLater {
                        updateViews()
                    }
                }
            }
        })

        listView.model = listModel
        listView.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION

        preferredSize = Dimension(300, 200)
        updateViews()
    }

    private fun updateViews() {
        clickedLabel.text = clicked.toString()
        listModel.clear()
        listModel.addAll(todoList.getEntries().map { it.text })
    }
}