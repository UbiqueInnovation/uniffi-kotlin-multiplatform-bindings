/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.cargo

import io.kotest.matchers.shouldBe
import kotlin.test.Test

// Tests CargoPackageId parses examples in https://doc.rust-lang.org/cargo/reference/pkgid-spec.html correctly.
class CargoPackageIdTest {
    @Test
    fun testCratesIoDependencies() {
        "regex" shouldBeParsedAs "regex"
        "regex@1.4" shouldBeParsedAs ("regex" version "1.4")
        "regex@1.4.3" shouldBeParsedAs ("regex" version "1.4.3")
        "https://github.com/rust-lang/crates.io-index#regex" shouldBeParsedAs "regex"
        "https://github.com/rust-lang/crates.io-index#regex@1.4.3" shouldBeParsedAs ("regex" version "1.4.3")
        "registry+https://github.com/rust-lang/crates.io-index#regex@1.4.3" shouldBeParsedAs ("regex" version "1.4.3")
    }

    @Test
    fun testGitDependencies() {
        "https://github.com/rust-lang/cargo#0.52.0" shouldBeParsedAs ("cargo" version "0.52.0")
        "https://github.com/rust-lang/cargo#cargo-platform@0.1.2" shouldBeParsedAs ("cargo-platform" version "0.1.2")
        "ssh://git@github.com/rust-lang/regex.git#regex@1.4.3" shouldBeParsedAs ("regex" version "1.4.3")
        "git+ssh://git@github.com/rust-lang/regex.git#regex@1.4.3" shouldBeParsedAs ("regex" version "1.4.3")
        "git+ssh://git@github.com/rust-lang/regex.git?branch=dev#regex@1.4.3" shouldBeParsedAs ("regex" version "1.4.3")
    }

    @Test
    fun testLocalPackageDependencies() {
        "file:///path/to/my/project/foo" shouldBeParsedAs "foo"
        "file:///path/to/my/project/foo#1.1.8" shouldBeParsedAs ("foo" version "1.1.8")
        "path+file:///path/to/my/project/foo#1.1.8" shouldBeParsedAs ("foo" version "1.1.8")
    }

    private infix fun String.shouldBeParsedAs(expectedPackageId: CargoPackageId) {
        val packageId = CargoPackageId(this)
        packageId.name shouldBe expectedPackageId.name
        packageId.version shouldBe expectedPackageId.version
    }

    private infix fun String.shouldBeParsedAs(expectedName: String) {
        this shouldBeParsedAs (expectedName version null)
    }

    private infix fun String.version(version: String?) = CargoPackageId(url = null, name = this, version = version)
}
