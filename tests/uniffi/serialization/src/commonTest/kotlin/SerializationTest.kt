/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import io.kotest.matchers.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import serialization.*
import kotlin.test.*

class SerializationTest {
    @Test
    fun testSerializeEnumSimple() {
        Json.encodeToString(Difficulty.EASY) shouldBe "\"EASY\""
    }

    @Test
    fun testDeserializeEnumSimple() {
        Json.decodeFromString<Difficulty>("\"HARD\"") shouldBe Difficulty.HARD
    }

    @Test
    fun testSerializeStructSimple() {
        Json.encodeToString(Values(4L, 2L)) shouldBe "{\"a\":4,\"b\":2}"
    }

    @Test
    fun testDeserializeStructSimple() {
        Json.decodeFromString<Values>("{\"a\":13,\"b\":37}") shouldBe Values(13, 37)
    }

    @Test
    fun testDeserializeImplicitNull() {
        Json.decodeFromString<ValuesOptional>("{\"a\":13}") shouldBe ValuesOptional(13, null)
    }
}
