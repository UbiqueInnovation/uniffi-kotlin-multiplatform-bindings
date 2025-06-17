package ch.ubique.uniffi.plugin.utils

import java.io.Serializable

sealed interface BindgenSource : Serializable {
    data class Registry(
        val packageName: String,
        val version: String,
    ) : BindgenSource, Serializable

    data class Path(val path: String) : BindgenSource, Serializable

    data class Git(
        val repository: String,
        val commit: Commit? = null,
    ) : BindgenSource, Serializable {
        sealed interface Commit : Serializable {
            data class Branch(val branch: String) : Commit, Serializable
            data class Tag(val tag: String) : Commit, Serializable
            data class Revision(val revision: String) : Commit, Serializable
        }
    }
}