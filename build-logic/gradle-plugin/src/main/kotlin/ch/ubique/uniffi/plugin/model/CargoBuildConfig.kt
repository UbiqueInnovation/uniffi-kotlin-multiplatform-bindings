package ch.ubique.uniffi.plugin.model

import org.gradle.api.Named
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property

abstract class CargoBuildConfig constructor(
    private val name: String,
    objects: ObjectFactory
) : Named {
    override fun getName() = name

    val useCross: Property<Boolean> = objects.property<Boolean>().convention(false)
}

