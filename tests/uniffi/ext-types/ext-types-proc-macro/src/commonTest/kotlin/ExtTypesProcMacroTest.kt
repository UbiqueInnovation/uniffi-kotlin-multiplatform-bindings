/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import kotlin.test.Test
import ext_types_proc_macro.*
import io.kotest.matchers.shouldBe
import uniffi_one.*
import io.ktor.http.Url
import kotlinx.coroutines.runBlocking

class ExtTypesProcMacroTest {
    @Test
    fun run() {
        val ct = getCombinedType(null)
        ct.uot.sval shouldBe "hello"
        ct.guid shouldBe  "a-guid"
        ct.url shouldBe  Url("http://example.com/")

        val ct2 = getCombinedType(ct)
        ct shouldBe ct2

        getObjectsType(null).maybeInterface shouldBe null
        getObjectsType(null).maybeTrait shouldBe null
        getUniffiOneTrait(null) shouldBe null

        val url = Url("http://example.com/")
        getUrl(url) shouldBe  url
        getMaybeUrl(url)!! shouldBe  url
        getMaybeUrl(null) shouldBe  null
        getUrls(listOf(url)) shouldBe  listOf(url)
        getMaybeUrls(listOf(url, null)) shouldBe listOf(url, null)

        val uot = UniffiOneType("hello")
        getUniffiOneType(uot) shouldBe uot
        getMaybeUniffiOneType(uot)!! shouldBe uot
        getMaybeUniffiOneType(null) shouldBe null
        getUniffiOneTypes(listOf(uot)) shouldBe listOf(uot)
        getMaybeUniffiOneTypes(listOf(uot, null)) shouldBe listOf(uot, null)

        runBlocking {
            // This async function comes from the `uniffi-one` crate
            getUniffiOneAsync() shouldBe UniffiOneEnum.ONE
            // This async function comes from the `proc-macro-lib` crate
            getUniffiOneTypeAsync(uot) shouldBe uot
        }

        val uopmt = UniffiOneProcMacroType("hello from proc-macro world")
        getUniffiOneProcMacroType(uopmt) shouldBe uopmt
        getMyProcMacroType(uopmt) shouldBe uopmt

        val uoe = UniffiOneEnum.ONE
        getUniffiOneEnum(uoe) shouldBe uoe
        getMaybeUniffiOneEnum(uoe)!! shouldBe uoe
        getMaybeUniffiOneEnum(null) shouldBe null
        getUniffiOneEnums(listOf(uoe)) shouldBe listOf(uoe)
        getMaybeUniffiOneEnums(listOf(uoe, null)) shouldBe listOf(uoe, null)

        val g = getGuidProcmacro(null)
        g shouldBe getGuidProcmacro(g)
    }
}