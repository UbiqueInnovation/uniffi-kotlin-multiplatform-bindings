package ch.ubique.uniffi.plugin.utils

import org.gradle.api.GradleException
import java.io.File

object RustLocator {
	private val cache: MutableMap<String, File> = mutableMapOf()

	/**
	 * Resolves the location of a binary tool (like 'cargo' or 'rustup').
	 * Searches the system PATH first, then falls back to ~/.cargo/bin/.
	 */
	fun findRustExecutable(baseName: String): File {
		// Don't lookup the value twice
		cache[baseName]?.let { return@let it }

		val isWindows = System.getProperty("os.name").lowercase().contains("windows")
		val exeName = if (isWindows) "$baseName.exe" else baseName

		val pathEnv = System.getenv("PATH")
		if (pathEnv != null) {
			// File.pathSeparator automatically handles ';' on Windows and ':' on Unix
			val pathDirs = pathEnv.split(File.pathSeparator)
			for (dir in pathDirs) {
				val candidate = File(dir, exeName)
				// Check if it's a file and actually executable
				if (candidate.isFile && candidate.canExecute()) {
					cache[baseName] = candidate
					return candidate
				}
			}
		}

		// Fallback to ~/.cargo/bin
		val userHome = System.getProperty("user.home")
		val fallbackCandidate = File(userHome, ".cargo/bin/$exeName")

		if (fallbackCandidate.isFile && fallbackCandidate.canExecute()) {
			cache[baseName] = fallbackCandidate
			return fallbackCandidate
		}

		throw GradleException("$baseName not found in PATH and $userHome/.cargo/bin/")
	}
}