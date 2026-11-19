package ch.ubique.uniffi.plugin.utils

import ch.ubique.uniffi.plugin.model.CargoMetadata
import ch.ubique.uniffi.plugin.model.CrateType
import java.io.File

fun CargoMetadata.targetPackage(packageDirectory: File): CargoMetadata.Package {
    val manifest = packageDirectory.resolve("Cargo.toml").canonicalFile
    return requireNotNull(packages.find { File(it.manifestPath).canonicalFile == manifest }) {
        "Couldn't find the package corresponding to $manifest!"
    }
}

/** The names of the package's dependencies as in its `Cargo.toml`, which `<dependency>/<feature>` refers to. */
fun CargoMetadata.Package.dependencyNames(): Set<String> =
    dependencies.mapTo(sortedSetOf()) { it.rename ?: it.name }

/** The package's library target, if it has one. */
val CargoMetadata.Package.libraryTarget: CargoMetadata.Package.Target?
    get() = targets.firstOrNull { target -> target.kinds.none { it.crateType == CrateType.Executable } }

/**
 * The workspace members [pkg] depends on, directly or through other members, whose library
 * declares a `cdylib`. Only normal dependencies count, the ones `cargo rustc --lib` builds.
 */
fun CargoMetadata.cdylibWorkspaceDependencies(pkg: CargoMetadata.Package): Set<String> {
    val members = packages
        .filter { it.id in workspaceMembers }
        .associateBy { File(it.manifestPath).parentFile.canonicalFile }
    val visited = mutableSetOf(pkg.id)
    val cdylibs = sortedSetOf<String>()

    fun visit(dependent: CargoMetadata.Package) {
        dependent.dependencies
            .filter { it.kind == null }
            .mapNotNull { dependency -> dependency.path?.let { members[File(it).canonicalFile] } }
            .filter { visited.add(it.id) }
            .forEach { member ->
                if (CrateType.SystemDynamicLibrary in member.libraryTarget?.crateTypes.orEmpty()) {
                    cdylibs += member.name
                }
                visit(member)
            }
    }
    visit(pkg)
    return cdylibs
}
