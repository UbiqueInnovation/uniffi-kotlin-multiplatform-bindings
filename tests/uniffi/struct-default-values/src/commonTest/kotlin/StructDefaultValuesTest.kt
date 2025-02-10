/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import io.kotest.matchers.*
import struct_default_values.*
import kotlin.test.*

class StructDefaultValuesTest {
    companion object {
        const val URL = "https://mozilla.github.io/uniffi-rs"
    }

    @Test
    fun testBookmark() {
        var bookmark = Bookmark(position = 2, url = URL)
        bookmark.guid shouldBe null
        bookmark.position shouldBe 2
        bookmark.url shouldBe URL
        bookmark shouldBe createBookmark(position = 2, url = URL)

        bookmark = Bookmark(position = 3, url = URL, guid = "c0ffee")
        bookmark.guid shouldBe "c0ffee"
        bookmark.position shouldBe 3
        bookmark.url shouldBe URL
        bookmark shouldBe createBookmark(position = 3, url = URL, guid = "c0ffee")
    }

    @Test
    fun testBookmarkWithNamedArguments() {
        // Order doesn't matter here.
        val bookmark = Bookmark(url = URL, guid = "c0ffee", position = 4)
        bookmark.guid shouldBe "c0ffee"
        bookmark.position shouldBe 4
        bookmark.url shouldBe URL
        bookmark.lastModified shouldBe null
        bookmark.title shouldBe null
        bookmark shouldBe createBookmark(url = URL, guid = "c0ffee", position = 4)
    }

    @Test
    fun testBookmarkWithUnnamedArguments() {
        // Order matters here when unnamed.
        val bookmark = Bookmark("c0ffee", 5, 17, URL)
        bookmark.guid shouldBe "c0ffee"
        bookmark.position shouldBe 5
        bookmark.url shouldBe URL
        bookmark.lastModified shouldBe 17
        bookmark.title shouldBe null
        bookmark shouldBe createBookmark("c0ffee", 5, 17, URL)
    }

    @Test
    fun testBookmark2() {
        val bookmark = Bookmark2(guid = null, lastModified = null, title = null)
        bookmark.guid shouldBe null
        bookmark.position shouldBe 26
        bookmark.lastModified shouldBe null
        bookmark.url shouldBe URL
        bookmark.title shouldBe null
        bookmark shouldBe createBookmark2(guid = null, lastModified = null, title = null)
    }
}
