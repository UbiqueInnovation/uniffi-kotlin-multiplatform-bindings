package ch.ubique.uniffi.plugin.utils

import java.io.Serializable

sealed class BindgenSource(
    open val bindgenName: String?,
    open val packageName: String?,
) : Serializable {
    data class Registry(
        override val packageName: String,
        val version: String,
        override val bindgenName: String? = null,
    ) : BindgenSource(bindgenName, packageName), Serializable

    data class Path(
        val path: String,
        override val bindgenName: String? = null,
        override val packageName: String? = null,
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