/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import kotlin.test.Test
import ext_types.*
import custom.*
import io.kotest.matchers.shouldBe
import sub_lib.*
import uniffi_one.*
import io.ktor.http.Url
import kotlinx.coroutines.runBlocking

class ExtTypesTest {
    @Test
    fun run() {
        val ct = getCombinedType(null)
        ct.uot.sval shouldBe "hello"
        ct.guid shouldBe  "a-guid"
        ct.url shouldBe  java.net.URL("http://example.com/")

        val ct2 = getCombinedType(ct)
        ct shouldBe ct2

        getObjectsType(null).maybeInterface shouldBe null
        getObjectsType(null).maybeTrait shouldBe null
        getUniffiOneTrait(null) shouldBe null

        getSubType(null).maybeInterface shouldBe null
        getTraitImpl().hello() shouldBe "sub-lib trait impl says hello"

        val url = java.net.URL("http://example.com/")
        getUrl(url) shouldBe  url
        getMaybeUrl(url)!! shouldBe  url
        getMaybeUrl(null) shouldBe  null
        getUrls(listOf(url)) shouldBe  listOf(url)
        getMaybeUrls(listOf(url, null)) shouldBe listOf(url, null)

        getGuid("guid") shouldBe "guid"
        getOuid("ouid") shouldBe "ouid"
        //getImportedGuid("guid") shouldBe "guid"
        getImportedOuid("ouid") shouldBe "ouid"

        val uot = UniffiOneType("hello")
        getUniffiOneType(uot) shouldBe uot
        getMaybeUniffiOneType(uot)!! shouldBe uot
        getMaybeUniffiOneType(null) shouldBe null
        getUniffiOneTypes(listOf(uot)) shouldBe listOf(uot)
        getMaybeUniffiOneTypes(listOf(uot, null)) shouldBe listOf(uot, null)

        val uopmt = UniffiOneProcMacroType("hello from proc-macro world")
        getUniffiOneProcMacroType(uopmt) shouldBe uopmt
        getMyProcMacroType(uopmt) shouldBe uopmt

        val uoe = UniffiOneEnum.ONE
        getUniffiOneEnum(uoe) shouldBe uoe
        getMaybeUniffiOneEnum(uoe)!! shouldBe uoe
        getMaybeUniffiOneEnum(null) shouldBe null
        getUniffiOneEnums(listOf(uoe)) shouldBe listOf(uoe)
        getMaybeUniffiOneEnums(listOf(uoe, null)) shouldBe listOf(uoe, null)

        ct.ecd.sval shouldBe "ecd"
        getExternalCrateInterface("foo").value() shouldBe "foo"
    }
}