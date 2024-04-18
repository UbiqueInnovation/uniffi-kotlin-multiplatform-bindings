/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.uniffi.examples.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.gitlab.trixnity.uniffi.examples.arithmeticpm.add
import io.gitlab.trixnity.uniffi.examples.arithmeticpm.sub
import io.gitlab.trixnity.uniffi.examples.todolist.TodoEntry
import io.gitlab.trixnity.uniffi.examples.todolist.TodoList

@Composable
fun ContentView(todoList: TodoList) {
    val stride = remember { 1UL }
    var clicked by remember { mutableStateOf(0UL) }
    var text by remember { mutableStateOf("") }

    Surface {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("$clicked")
                Spacer(Modifier.width(8.dp))
                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("New Task") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions {
                        todoList.addEntry(TodoEntry("$clicked $text"))
                        text = ""
                        clicked = add(clicked, stride)
                    },
                    singleLine = true,
                )
            }

            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(8.dp),
            ) {
                items(todoList.getEntries()) { entry ->
                    ListItem(
                        headlineContent = { Text(entry.text) },
                        trailingContent = {
                            IconButton(
                                onClick = {
                                    todoList.clearItem(entry.text)
                                    clicked = sub(clicked, stride)
                                },
                            ) {
                                Icon(
                                    Icons.Outlined.Delete,
                                    contentDescription = "Delete ${entry.text}",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun ContentViewPreview() {
    ContentView(TodoList())
}
