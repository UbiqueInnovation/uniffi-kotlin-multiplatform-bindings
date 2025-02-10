/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.utils

import io.gitlab.trixnity.gradle.RustHost
import org.gradle.api.file.FileSystemLocation
import java.io.File
import java.io.Serializable

/**
 * Utility class for storing a list of paths to be joined to a string with separators.
 */
data class PathList(val paths: List<File> = emptyList()) : Serializable {
    constructor(paths: String) : this(paths.split(separator).filter(String::isNotEmpty).map(::File))

    operator fun plus(other: File): PathList = PathList(paths + other)
    operator fun plus(other: FileSystemLocation): PathList = plus(other.asFile)
    operator fun plus(other: Iterable<File>): PathList = PathList(paths + other)
    operator fun plus(other: PathList): PathList = plus(other.paths)
    operator fun plus(other: String): PathList = plus(PathList(other))

    fun suppressPathsUnder(root: File): PathList {
        val pathsToSuppress = paths.filter { it.startsWith(root) }
        val otherPaths = paths.filterNot { it.startsWith(root) }
        return PathList(otherPaths + pathsToSuppress)
    }

    fun joinToString() = paths.joinToString(separator) { file -> file.absolutePath }

    override fun toString(): String = joinToString()

    companion object {
        val separator: String = RustHost.Platform.current.pathSeparator
    }
}
