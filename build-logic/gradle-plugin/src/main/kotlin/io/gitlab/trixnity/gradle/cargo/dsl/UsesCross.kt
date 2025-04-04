/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.cargo.dsl

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

interface UsesCross {
    /**
     Use cross to build the target. Note this is especially useful when building linux targets on a OSX host, as there is
     random issues with libc.
     */
    val usesCross: Property<Boolean>
}
