/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import io.kotest.matchers.shouldBe
import mutable_records.*
import kotlin.test.Test

/**
 * `mutable_records` in `uniffi.toml` names the records that keep `var` fields while
 * `generate_immutable_records` is on for the rest of the binding.
 *
 * `var` is what these assertions rest on: an assignment to a field of `UdlThawed` or
 * `ProcThawed` is only valid Kotlin if the field was generated as `var`, so this file not
 * compiling is itself the failure for the listed records. The unlisted ones are covered
 * the other way round - `bindgen`'s `test_mutable_records_exempts_listed_records` reads
 * the generated source, since a `val` assignment could not be written here at all.
 */
class MutableRecordsTest {
    @Test
    fun testUdlListedRecordFieldsAreMutable() {
        val record = UdlThawed(counter = 1L, label = "one")
        record.counter = 2L
        record.label = "two"

        record shouldBe UdlThawed(counter = 2L, label = "two")
    }

    @Test
    fun testProcMacroListedRecordFieldsAreMutable() {
        val record = ProcThawed(counter = 1L, label = "one")
        record.counter = 2L
        record.label = "two"

        record shouldBe ProcThawed(counter = 2L, label = "two")
    }

    @Test
    fun testMutatedFieldsCrossTheFfi() {
        // Mutating a record only touches the Kotlin object; these prove the new values are
        // what gets lowered on the next call rather than the ones it was constructed with.
        val udl = UdlThawed(counter = 1L, label = "one")
        udl.counter = 41L
        udl.label = "forty-one"
        echoUdlThawed(udl) shouldBe UdlThawed(counter = 41L, label = "forty-one")

        val proc = ProcThawed(counter = 1L, label = "one")
        proc.counter = 42L
        proc.label = "forty-two"
        echoProcThawed(proc) shouldBe ProcThawed(counter = 42L, label = "forty-two")
    }

    @Test
    fun testMutableRecordKeepsItsDefault() {
        // A `var` field with a default value still carries the default.
        ProcThawed(counter = 7L).label shouldBe "unset"
    }

    @Test
    fun testUnlistedRecordsStillRoundTrip() {
        // Immutable records are unaffected by the list beyond losing their setters; `copy`
        // is how a caller changes one.
        val frozen = UdlFrozen(counter = 1L, label = "one")
        echoUdlFrozen(frozen.copy(counter = 2L)) shouldBe UdlFrozen(counter = 2L, label = "one")

        val procFrozen = ProcFrozen(counter = 1L, label = "one")
        echoProcFrozen(procFrozen.copy(label = "two")) shouldBe
            ProcFrozen(counter = 1L, label = "two")
    }
}
