/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.cargo

internal data class CargoPackageId(
    val url: String?,
    val name: String?,
    val version: String?,
)

internal fun CargoPackageId(spec: String): CargoPackageId {
    if (!urlRegex.matches(spec)) {
        val atIndex = spec.indexOf('@')
        if (atIndex == -1) {
            return CargoPackageId(null, spec, null)
        }
        return CargoPackageId(null, spec.substring(0, atIndex), spec.substring(atIndex + 1))
    }

    val match = urlPackageIdRegex.matchEntire(spec.trim())!!
    val urlOrName = match.groupValues[1].takeIf(String::isNotEmpty)
    val nameOrVersion = match.groupValues[2].takeIf(String::isNotEmpty)
    val version = match.groupValues[3].takeIf(String::isNotEmpty)

    return CargoPackageId(
        spec,
        nameOrVersion?.takeUnless(versionRegex::matches)
            ?: urlOrName!!.substringAfterLast('/'),
        version ?: nameOrVersion?.takeIf(versionRegex::matches),
    )
}

private val urlRegex = Regex("""^((registry|git|file|path)\+)?(http|https|file|git|ssh)://(.*)${'$'}""")

private val urlPackageIdRegex = Regex("""^([^#]+)(?:#([^@]+?))?(?:@(.+))?${'$'}""")

/**
 * The regex conforming to [Semantic Versioning 2.0.0](https://semver.org).
 */
private val versionRegex =
    Regex("""^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-((?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\+([0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?${'$'}""")
