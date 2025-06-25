package ch.ubique.uniffi.plugin.model

import ch.ubique.uniffi.plugin.model.CrateType.DynamicLibrary
import ch.ubique.uniffi.plugin.model.CrateType.StaticLibrary
import ch.ubique.uniffi.plugin.model.CrateType.SystemDynamicLibrary
import ch.ubique.uniffi.plugin.model.CrateType.SystemStaticLibrary
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Represents a Cargo target type.
 */
@Serializable(with = TargetTypeSerializer::class)
enum class CargoTargetKind(
    val crateType: CrateType = CrateType.Executable,
    private val actualName: String = crateType.toString(),
) {
    // library targets
    Library(crateType = CrateType.Library), StaticLibrary(crateType = CrateType.StaticLibrary), DynamicLibrary(
        crateType = CrateType.DynamicLibrary
    ),
    SystemStaticLibrary(crateType = CrateType.SystemStaticLibrary), SystemDynamicLibrary(crateType = CrateType.SystemDynamicLibrary), ProceduralMacro(
        crateType = CrateType.ProceduralMacro
    ),

    // binary targets
    Binary(actualName = "bin"), Example(actualName = "example"), Test(actualName = "test"), Benchmark(
        actualName = "bench"
    ),
    BuildScript(actualName = "custom-build");

    override fun toString(): String = actualName

    fun isNormalLibrary() = when (crateType) {
        CrateType.StaticLibrary,
        CrateType.DynamicLibrary,
        CrateType.SystemStaticLibrary,
        CrateType.SystemDynamicLibrary -> true

        else -> false
    }
}

private object TargetTypeSerializer : KSerializer<CargoTargetKind> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        "io.gitlab.trixnity.gradle.cargo.TargetTypeSerializer",
        PrimitiveKind.STRING,
    )

    override fun serialize(encoder: Encoder, value: CargoTargetKind) {
        encoder.encodeString(value.toString())
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun deserialize(decoder: Decoder): CargoTargetKind {
        val actualName = decoder.decodeString()
        return CargoTargetKind.entries.first { it.toString() == actualName }
    }
}
