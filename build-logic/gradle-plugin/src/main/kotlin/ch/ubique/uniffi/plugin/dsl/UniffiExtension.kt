package ch.ubique.uniffi.plugin.dsl

import ch.ubique.uniffi.plugin.Constants
import ch.ubique.uniffi.plugin.utils.BindgenSource
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

// NOTE: this used to be `UniffiExtension(internal val project: Project)`. Holding a
// Project made every provider rooted in `bindingsGeneration` un-serialisable, which
// the configuration cache rejects with
// "cannot serialize object of type 'DefaultProject' ... as these are not supported".
// ObjectFactory is the injectable service that was actually being used.
abstract class UniffiExtension @Inject internal constructor(private val objects: ObjectFactory) {
    internal var bindgenSource: Property<BindgenSource> =
        objects.property<BindgenSource>().convention(Constants.BINDGEN_SOURCE)

    internal abstract val bindingsGeneration: Property<BindingsGeneration>

    /**
     * Runs `ktlint` on the generated bindings. `ktlint` needs to be in PATH.
     */
    val formatCode: Property<Boolean> =
        objects.property<Boolean>().convention(false)

    /**
     * Add the runtime dependency to commonMain.
     *
     * TODO: Allow for configuration like bindgen source
     */
    val addRuntime: Property<Boolean> =
        objects.property<Boolean>().convention(true)

	val addDependencies: Property<Boolean> =
		objects.property<Boolean>().convention(true)

	/**
	 * Whether bindings for external crates should be generated. Default: false
	 */
	val generateBindingsForExternalCrates: Property<Boolean> =
		objects.property<Boolean>().convention(false)

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
        features: List<String> = emptyList(),
    ) {
        bindgenSource.set(BindgenSource.Path(path.asFile.absolutePath, bindgenName, packageName, features))
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
        val generation = bindingsGeneration.orNull ?: objects.newInstance<BindingsGenerationFromUdl>()
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
            bindingsGeneration.orNull ?: objects.newInstance<BindingsGenerationFromLibrary>()
                .also { bindingsGeneration.set(it) }

        generation as? BindingsGenerationFromLibrary
            ?: throw GradleException("A `generateFromUdl` block has already been defined.")

        configure.execute(generation)
    }
}

// `project` was never read by either subclass; it only served to drag a Project
// reference into task state via the `bindingsGeneration` providers.
sealed class BindingsGeneration {
    /**
     * The UDL namespace. Defaults to `"$libraryName"`.
     */
    abstract val namespace: Property<String>
}

abstract class BindingsGenerationFromUdl @Inject internal constructor() : BindingsGeneration() {
    /**
     * The UDL file. Defaults to `"${crateDirectory}/src/${crateName}.udl"`.
     */
    abstract val udlFile: RegularFileProperty
}

abstract class BindingsGenerationFromLibrary @Inject internal constructor() : BindingsGeneration()

