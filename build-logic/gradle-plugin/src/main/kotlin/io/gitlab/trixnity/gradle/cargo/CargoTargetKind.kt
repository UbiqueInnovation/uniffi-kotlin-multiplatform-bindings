/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.cargo

import io.gitlab.trixnity.gradle.cargo.rust.CrateType
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
    Library(crateType = CrateType.Library),
    StaticLibrary(crateType = CrateType.StaticLibrary),
    DynamicLibrary(crateType = CrateType.DynamicLibrary),
    SystemStaticLibrary(crateType = CrateType.SystemStaticLibrary),
    SystemDynamicLibrary(crateType = CrateType.SystemDynamicLibrary),
    ProceduralMacro(crateType = CrateType.ProceduralMacro),

    // binary targets
    Binary(actualName = "bin"),
    Example(actualName = "example"),
    Test(actualName = "test"),
    Benchmark(actualName = "bench"),
    BuildScript(actualName = "custom-build");

    override fun toString(): String = actualName

    fun isNormalLibrary() = when (crateType) {
        CrateType.Executable, CrateType.ProceduralMacro -> false
        else -> true
    }
}

private object TargetTypeSerializer : KSerializer<CargoTargetKind> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("TargetType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: CargoTargetKind) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): CargoTargetKind {
        val actualName = decoder.decodeString()
        return CargoTargetKind.entries.first { it.toString() == actualName }
    }
}
