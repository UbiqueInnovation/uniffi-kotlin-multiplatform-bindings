/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import io.gitlab.trixnity.uniffi.examples.todolist.*
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test

class TodoListTest {
    @Test
    fun testEmptyTodoList() {
        val todo = TodoList()
        shouldThrowExactly<TodoException.EmptyTodoList> {
            todo.getLast()
        }
    }

    @Test
    fun testEmptyStringEntry() {
        shouldThrowExactly<TodoException.EmptyString> {
            createEntryWith("")
        }
    }

    @Test
    fun testEntryOperations() {
        val todo = TodoList()

        todo.addItem("Write strings support")

        todo.getLast() shouldBe "Write strings support"

        todo.addItem("Write tests for strings support")

        todo.getLast() shouldBe "Write tests for strings support"

        val entry = createEntryWith("Write bindings for strings as record members")

        todo.addEntry(entry)
        todo.getLast() shouldBe "Write bindings for strings as record members"
        todo.getLastEntry().text shouldBe "Write bindings for strings as record members"

        todo.addItem("Test Ünicode hàndling without an entry can't believe I didn't test this at first 🤣")
        todo.getLast() shouldBe "Test Ünicode hàndling without an entry can't believe I didn't test this at first 🤣"

        val entry2 = TodoEntry("Test Ünicode hàndling in an entry can't believe I didn't test this at first 🤣")
        todo.addEntry(entry2)
        todo.getLastEntry().text shouldBe "Test Ünicode hàndling in an entry can't believe I didn't test this at first 🤣"

        todo.getEntries().size shouldBe 5

        todo.addEntries(listOf(TodoEntry("foo"), TodoEntry("bar")))
        todo.getEntries().size shouldBe 7
        todo.getLastEntry().text shouldBe "bar"

        todo.addItems(listOf("bobo", "fofo"))
        todo.getItems().size shouldBe 9
        todo.getItems()[7] shouldBe "bobo"

        getDefaultList() shouldBe null

        // Note that each individual object instance needs to be explicitly destroyed,
        // either by using the `.use` helper or explicitly calling its `.destroy` method.
        // Failure to do so will leak the underlying Rust object.
        TodoList().use { todo2 ->
            setDefaultList(todo)
            getDefaultList()!!.use { default ->
                todo.getEntries() shouldBe default.getEntries()
                todo2.getEntries() shouldNotBe default.getEntries()
            }

            todo2.makeDefault()
            getDefaultList()!!.use { default ->
                todo.getEntries() shouldNotBe default.getEntries()
                todo2.getEntries() shouldBe default.getEntries()
            }

            todo.addItem("Test liveness after being demoted from default")
            todo.getLast() shouldBe "Test liveness after being demoted from default"

            todo2.addItem("Test shared state through local vs default reference")
            getDefaultList()!!.use { default ->
                default.getLast() shouldBe "Test shared state through local vs default reference"
            }
        }

        // Ensure the kotlin version of deinit doesn't crash, and is idempotent.
        todo.destroy()
        todo.destroy()
    }
}