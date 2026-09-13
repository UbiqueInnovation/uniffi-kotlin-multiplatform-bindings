package ch.ubique.uniffi.plugin.utils

import java.io.Serializable
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

sealed class BindgenSource(
    open val bindgenName: String?,
    open val packageName: String?,
) : Serializable {
    /**
     * A stable, filesystem-safe identity for the bindgen installation.
     *
     * Multiple projects in one Gradle build commonly use the same bindgen source. The identity
     * lets the plugin share the installation without allowing two different sources to overwrite
     * one another.
     */
    val cacheKey: String
        get() = MessageDigest.getInstance("SHA-256")
            .digest(toString().toByteArray(StandardCharsets.UTF_8))
            .take(12)
            .joinToString("") { byte -> "%02x".format(byte) }

    data class Registry(
        override val packageName: String,
        val version: String,
        override val bindgenName: String? = null,
    ) : BindgenSource(bindgenName, packageName), Serializable

    data class Path(
        val path: String,
        override val bindgenName: String? = null,
        override val packageName: String? = null,
        val features: List<String> = emptyList(),
    ) : BindgenSource(bindgenName, packageName), Serializable

    data class Git(
        val repository: String,
        val commit: Commit? = null,
        override val bindgenName: String? = null,
        override val packageName: String? = null,
    ) : BindgenSource(bindgenName, packageName), Serializable {
        sealed interface Commit : Serializable {
            data class Branch(val branch: String) : Commit, Serializable
            data class Tag(val tag: String) : Commit, Serializable
            data class Revision(val revision: String) : Commit, Serializable
        }
    }
}
