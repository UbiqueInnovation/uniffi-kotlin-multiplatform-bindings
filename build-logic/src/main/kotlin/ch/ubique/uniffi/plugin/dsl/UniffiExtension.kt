package ch.ubique.uniffi.plugin.dsl

import ch.ubique.uniffi.plugin.Constants
import ch.ubique.uniffi.plugin.utils.BindgenSource
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property

abstract class UniffiExtension(project: Project) {
    internal var bindgenSource: Property<BindgenSource> =
        project.objects.property<BindgenSource>().convention(Constants.BINDGEN_SOURCE)

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
    ) {
        bindgenSource.set(BindgenSource.Registry(packageName, version))
    }

    /**
     * Install the bindgen located in the given [path].
     */
    fun bindgenFromPath(path: Directory) {
        bindgenSource.set(BindgenSource.Path(path.asFile.absolutePath))
    }

    /**
     * Download and install the bindgen from the given Git repository. If [commit] is specified, `cargo install` will
     * install the bindgen of that [commit].
     */
    fun bindgenFromGit(repository: String, commit: BindgenSource.Git.Commit? = null) {
        bindgenSource.set(BindgenSource.Git(repository, commit))
    }

    /**
     * Download and install the bindgen from the given Git repository, using the given [branch].
     */
    fun bindgenFromGitBranch(repository: String, branch: String) {
        bindgenFromGit(repository, BindgenSource.Git.Commit.Branch(branch))
    }

    /**
     * Download and install the bindgen from the given Git repository, using the given [tag].
     */
    fun bindgenFromGitTag(repository: String, tag: String) {
        bindgenFromGit(repository, BindgenSource.Git.Commit.Tag(tag))
    }

    /**
     * Download and install the bindgen from the given Git repository, using the given commit [revision].
     */
    fun bindgenFromGitRevision(repository: String, revision: String) {
        bindgenFromGit(repository, BindgenSource.Git.Commit.Revision(revision))
    }
}
