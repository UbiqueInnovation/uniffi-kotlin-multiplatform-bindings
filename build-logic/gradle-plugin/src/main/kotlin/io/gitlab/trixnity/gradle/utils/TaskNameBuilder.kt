/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.utils

import io.gitlab.trixnity.gradle.Variant
import io.gitlab.trixnity.gradle.cargo.dsl.CargoBuildVariant
import io.gitlab.trixnity.gradle.cargo.rust.profiles.CargoProfile
import io.gitlab.trixnity.gradle.cargo.rust.targets.RustTarget
import org.gradle.api.Task
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import kotlin.reflect.KClass

internal class TaskNameBuilder {
    private var prefix: KClass<*>? = null
    private var infixes: MutableList<String> = mutableListOf()
    private var target: RustTarget? = null
    private var profile: CargoProfile? = null
    private var variant: Variant? = null

    operator fun KClass<*>.unaryPlus(): TaskNameBuilder {
        prefix = this
        return this@TaskNameBuilder
    }

    operator fun String.unaryPlus(): TaskNameBuilder {
        infixes.add(this)
        return this@TaskNameBuilder
    }

    operator fun RustTarget.unaryPlus(): TaskNameBuilder {
        target = this
        return this@TaskNameBuilder
    }

    operator fun CargoProfile.unaryPlus(): TaskNameBuilder {
        profile = this
        return this@TaskNameBuilder
    }

    operator fun Variant.unaryPlus(): TaskNameBuilder {
        variant = this
        return this@TaskNameBuilder
    }

    operator fun CargoBuildVariant<*>.unaryPlus(): TaskNameBuilder {
        this@TaskNameBuilder.target = rustTarget
        this@TaskNameBuilder.variant = variant
        return this@TaskNameBuilder
    }

    override fun toString() = StringBuilder().apply {
        prefix?.simpleName?.substringBefore("Task")?.replaceFirstChar(Char::lowercaseChar)?.let(::append)
        var firstInfix = true
        for (infix in infixes) {
            append(
                infix.replaceFirstChar(
                    if (firstInfix && prefix == null) Char::lowercaseChar else Char::uppercaseChar
                )
            )
            firstInfix = false
        }

        target?.friendlyName?.uppercaseFirstChar()?.let(::append)
        profile?.profileName?.uppercaseFirstChar()?.let(::append)
        variant?.toString()?.uppercaseFirstChar()?.let(::append)
    }.toString()
}

internal inline fun <reified T : Task> TaskContainer.register(
    name: TaskNameBuilder.() -> Unit,
    noinline configuration: T.() -> Unit
): TaskProvider<T> = register<T>(
    TaskNameBuilder().apply {
        +T::class
        name()
    }.toString(),
    configuration,
)

internal inline fun <reified T : Task> TaskContainer.named(
    name: TaskNameBuilder.() -> Unit,
): TaskProvider<T> = named<T>(
    TaskNameBuilder().apply {
        +T::class
        name()
    }.toString()
)
