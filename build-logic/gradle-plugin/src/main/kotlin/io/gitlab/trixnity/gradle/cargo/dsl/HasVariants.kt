/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.cargo.dsl

import io.gitlab.trixnity.gradle.Variant

/**
 * Implemented by DSL objects containing settings varying by variants.
 */
interface HasVariants<out VariantT : CargoVariant> {
    /**
     * Settings for the debug variant.
     */
    val debug: VariantT

    /**
     * Settings for the debug variant.
     */
    fun debug(action: VariantT.() -> Unit) = debug.apply(action)

    /**
     * Settings for the release variant.
     */
    val release: VariantT

    /**
     * Settings for the release variant.
     */
    fun release(action: VariantT.() -> Unit) = release.apply(action)

    /**
     * Settings for all variants.
     */
    val variants: Iterable<VariantT>

    /**
     * Settings for all variants.
     */
    fun variants(action: VariantT.() -> Unit) {
        debug(action)
        release(action)
    }

    /**
     * Settings of the given variant.
     */
    fun variant(variant: Variant) = when (variant) {
        Variant.Debug -> debug
        Variant.Release -> release
    }

    /**
     * Settings of the given variant.
     */
    fun variant(variant: Variant, action: VariantT.() -> Unit) = when (variant) {
        Variant.Debug -> debug(action)
        Variant.Release -> release(action)
    }
}
