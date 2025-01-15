/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import enum_types.*
import io.kotest.matchers.collections.exist
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.shouldNot
import kotlin.reflect.full.functions
import kotlin.test.Test

class EnumTypesReflectionTest {
    @Test
    fun assertDestroyNotGenerated() {
        // Assert that no destroy() function is created for simple Enum
        val simpleCat: Animal = Animal.CAT
        simpleCat::class.functions shouldNot exist { it.name == "destroy" }

        // Assert that destroy() function is created for Enum with variants containing fields
        val cat: AnimalAssociatedType = AnimalAssociatedType.Cat
        cat::class.functions shouldHaveSingleElement { it.name == "destroy" }
    }
}