package ch.ubique.uniffi.plugin.dsl

import ch.ubique.uniffi.plugin.Constants
import ch.ubique.uniffi.plugin.utils.BindgenSource
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

abstract class UniffiExtension(internal val project: Project) {
    internal var bindgenSource: Property<BindgenSource> =
        project.objects.property<BindgenSource>().convention(Constants.BINDGEN_SOURCE)

    internal abstract val bindingsGeneration: Property<BindingsGeneration>

    /**
     * Add the runtime dependency to commonMain.
     *
     * TODO: Allow for configuration like bindgen source
     */
    val addRuntime: Property<Boolean> =
        project.objects.property<Boolean>().convention(true)

    /**
     * Install the bindgen of the given [version] from the given [registry]. If [registry] is not specified, this will
     * download the bindgen from `crates.io`.
     */
    fun bindgenFromRegistry(
        packageName: String,
        version: String,
        bindgenName: String? = Constants.BINDGEN_BIN_NAME,
    ) {
        bindgenSource.set(BindgenSource.Registry(packageName, version, bindgenName))
    }

    /**
     * Install the bindgen located in the given [path].
     */
    fun bindgenFromPath(
        path: Directory,
        bindgenName: String? = Constants.BINDGEN_BIN_NAME,
        packageName: String? = Constants.BINDGEN_PACKAGE_NAME,
    ) {
        bindgenSource.set(BindgenSource.Path(path.asFile.absolutePath, bindgenName, packageName))
    }

    /**
     * Download and install the bindgen from the given Git repository. If [commit] is specified, `cargo install` will
     * install the bindgen of that [commit].
     */
    fun bindgenFromGit(
        repository: String,
        commit: BindgenSource.Git.Commit? = null,
        bindgenName: String? = Constants.BINDGEN_BIN_NAME,
        packageName: String? = Constants.BINDGEN_PACKAGE_NAME,
    ) {
        bindgenSource.set(BindgenSource.Git(repository, commit, bindgenName, packageName))
    }

    /**
     * Download and install the bindgen from the given Git repository, using the given [branch].
     */
    fun bindgenFromGitBranch(
        repository: String,
        branch: String,
        bindgenName: String? = Constants.BINDGEN_BIN_NAME,
        packageName: String? = Constants.BINDGEN_PACKAGE_NAME,
    ) {
        bindgenFromGit(repository, BindgenSource.Git.Commit.Branch(branch), bindgenName, packageName)
    }

    /**
     * Download and install the bindgen from the given Git repository, using the given [tag].
     */
    fun bindgenFromGitTag(
        repository: String,
        tag: String,
        bindgenName: String? = Constants.BINDGEN_BIN_NAME,
        packageName: String? = Constants.BINDGEN_PACKAGE_NAME,
    ) {
        bindgenFromGit(repository, BindgenSource.Git.Commit.Tag(tag), bindgenName, packageName)
    }

    /**
     * Download and install the bindgen from the given Git repository, using the given commit [revision].
     */
    fun bindgenFromGitRevision(
        repository: String,
        revision: String,
        bindgenName: String? = Constants.BINDGEN_BIN_NAME,
        packageName: String? = Constants.BINDGEN_PACKAGE_NAME,
    ) {
        bindgenFromGit(repository, BindgenSource.Git.Commit.Revision(revision), bindgenName, packageName)
    }

    /**
     * Generate bindings using a UDL file.
     */
    fun generateFromUdl(configure: Action<BindingsGenerationFromUdl> = Action { }) {
        val generation = bindingsGeneration.orNull ?: project.objects.newInstance<BindingsGenerationFromUdl>(project)
            .also { bindingsGeneration.set(it) }

        generation as? BindingsGenerationFromUdl
            ?: throw GradleException("A `generateFromLibrary` block has already been defined.")


        configure.execute(generation)
    }

    /**
     * Generate bindings from the build result library file.
     */
    fun generateFromLibrary(configure: Action<BindingsGenerationFromLibrary> = Action { }) {
        val generation =
            bindingsGeneration.orNull ?: project.objects.newInstance<BindingsGenerationFromLibrary>(project)
                .also { bindingsGeneration.set(it) }

        generation as? BindingsGenerationFromLibrary
            ?: throw GradleException("A `generateFromUdl` block has already been defined.")

        configure.execute(generation)
    }
}

sealed class BindingsGeneration(internal val project: Project) {
    /**
     * The UDL namespace. Defaults to `"$libraryName"`.
     */
    abstract val namespace: Property<String>
}

abstract class BindingsGenerationFromUdl @Inject internal constructor(project: Project) : BindingsGeneration(project) {
    /**
     * The UDL file. Defaults to `"${crateDirectory}/src/${crateName}.udl"`.
     */
    abstract val udlFile: RegularFileProperty
}

abstract class BindingsGenerationFromLibrary @Inject internal constructor(project: Project) :
    BindingsGeneration(project)

