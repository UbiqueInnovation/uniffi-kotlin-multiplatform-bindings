package ch.ubique.uniffi.plugin.utils

import java.io.Serializable

sealed class BindgenSource(
    val bindgenName: String?,
    val packageName: String?,
) : Serializable {
    class Registry(
        packageName: String,
        val version: String,
        bindgenName: String? = null,
    ) : BindgenSource(bindgenName, packageName), Serializable

    class Path(
        val path: String,
        bindgenName: String? = null,
        packageName: String? = null,
    ) : BindgenSource(bindgenName, packageName), Serializable

    class Git(
        val repository: String,
        val commit: Commit? = null,
        bindgenName: String? = null,
        packageName: String? = null,
    ) : BindgenSource(bindgenName, packageName), Serializable {
        sealed interface Commit : Serializable {
            data class Branch(val branch: String) : Commit, Serializable
            data class Tag(val tag: String) : Commit, Serializable
            data class Revision(val revision: String) : Commit, Serializable
        }
    }
}