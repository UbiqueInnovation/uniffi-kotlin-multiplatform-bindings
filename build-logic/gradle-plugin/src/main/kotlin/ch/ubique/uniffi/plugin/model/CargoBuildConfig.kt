package ch.ubique.uniffi.plugin.model

import org.gradle.api.Named
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

abstract class CargoBuildConfig constructor(
    private val name: String,
    objects: ObjectFactory
) : Named {
    override fun getName() = name

    val useCross: Property<Boolean> = objects.property(Boolean::class.javaObjectType).convention(false)

    /**
     * Cargo features to enable for this target, in addition to `cargo { features }`. The bindings
     * come from the host's library, so these must not change the exported UniFFI interface.
     */
    val features: SetProperty<String> = objects.setProperty(String::class.java)
}

