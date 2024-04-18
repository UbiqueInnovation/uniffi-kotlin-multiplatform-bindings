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
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.zeroValue
import platform.AppKit.*
import platform.Foundation.NSIndexSet
import platform.Foundation.NSNotFound
import platform.darwin.NSInteger
import platform.darwin.NSObject
import platform.darwin.sel_registerName

@OptIn(ExperimentalForeignApi::class)
class ContentViewController(private val todoList: TodoList) : NSViewController(nibName = null, bundle = null) {
    private val stride = 1UL
    private var clicked = 0UL

    private val clickedView = NSTextField().apply {
        editable = false
        bordered = false
        bezeled = false
        drawsBackground = false
        stringValue = clicked.toString()
    }

    private val textFieldDelegate = object : NSObject(), NSTextFieldDelegateProtocol {
        override fun control(control: NSControl, textView: NSTextView, doCommandBySelector: COpaquePointer?): Boolean {
            if (doCommandBySelector == sel_registerName("insertNewline:")) {
                todoList.addEntry(TodoEntry("$clicked ${textView.string}"))
                textView.string = ""
                clicked = add(clicked, stride)
                updateViews()
                return true
            }

            return false
        }
    }
    private val textField = NSTextField().apply {
        delegate = textFieldDelegate
        placeholderString = "New task"
    }

    private val listViewDataSource = object : NSObject(), NSTableViewDataSourceProtocol {
        override fun numberOfRowsInTableView(tableView: NSTableView): NSInteger {
            return todoList.getEntries().count().toLong()
        }

        override fun tableView(
            tableView: NSTableView,
            objectValueForTableColumn: NSTableColumn?,
            row: NSInteger
        ): Any? {
            return todoList.getEntries().getOrNull(row.toInt())?.text
        }
    }

    private inner class ListView : NSTableView(frame = zeroValue()) {
        override fun keyDown(event: NSEvent) {
            when (event.charactersIgnoringModifiers?.firstOrNull()?.code?.toUInt()) {
                NSDeleteCharacter -> {
                    val entries = mutableListOf<TodoEntry>()
                    selectedRowIndexes.enumerateIndexesUsingBlock { selectedRowIndex, _ ->
                        val entry = todoList.getEntries().getOrNull(selectedRowIndex.toInt())
                        if (entry != null) {
                            entries.add(entry)
                        }
                    }
                    for (entry in entries) {
                        todoList.clearItem(entry.text)
                        clicked = sub(clicked, stride)
                    }
                    updateViews()
                }

                NSUpArrowFunctionKey -> {
                    if (selectedRowIndexes.firstIndex > 0UL) {
                        selectRowIndexes(NSIndexSet(selectedRowIndexes.firstIndex - 1UL), byExtendingSelection = false)
                    }
                }

                NSDownArrowFunctionKey -> {
                    if (selectedRowIndexes.firstIndex < (NSNotFound - 1).toUInt()) {
                        selectRowIndexes(NSIndexSet(selectedRowIndexes.firstIndex + 1UL), byExtendingSelection = false)
                    }
                }
            }
        }
    }

    private val listView = ListView().apply {
        dataSource = listViewDataSource
        addTableColumn(NSTableColumn(identifier = "Column"))
    }

    override fun loadView() {
        view = NSView()

        val stackView = NSStackView().apply {
            orientation = NSUserInterfaceLayoutOrientationVertical
            spacing = 8.0
            addArrangedSubview(NSStackView().apply {
                orientation = NSUserInterfaceLayoutOrientationHorizontal
                spacing = 8.0
                addArrangedSubview(clickedView)
                addArrangedSubview(textField)
            })
            addArrangedSubview(listView)
        }
        view.addSubview(stackView)

        stackView.apply {
            translatesAutoresizingMaskIntoConstraints = false
            topAnchor.constraintEqualToAnchor(view.topAnchor, constant = 20.0).active = true
            leadingAnchor.constraintEqualToAnchor(view.leadingAnchor, constant = 20.0).active = true
            trailingAnchor.constraintEqualToAnchor(view.trailingAnchor, constant = -20.0).active = true
            bottomAnchor.constraintEqualToAnchor(view.bottomAnchor, constant = -20.0).active = true
        }
    }

    override fun viewDidLoad() {
        super.viewDidLoad()
        updateViews()
    }

    private fun updateViews() {
        clickedView.stringValue = clicked.toString()
        listView.reloadData()
    }
}