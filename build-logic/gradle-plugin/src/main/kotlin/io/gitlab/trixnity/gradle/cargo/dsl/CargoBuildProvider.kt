/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.cargo.dsl

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectProvider

interface CargoBuildProvider<T : CargoBuild<*>> : NamedDomainObjectProvider<T> {
    override fun configure(action: Action<in T>)
    override fun getName(): String
}

internal class CargoBuildProviderImpl<T : CargoBuild<*>>(
    private val base: NamedDomainObjectProvider<T>
) : NamedDomainObjectProvider<T> by base, CargoBuildProvider<T>
