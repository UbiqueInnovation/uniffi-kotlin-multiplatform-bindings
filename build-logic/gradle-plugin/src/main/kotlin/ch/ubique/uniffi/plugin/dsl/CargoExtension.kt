package ch.ubique.uniffi.plugin.dsl

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty

abstract class CargoExtension(project: Project) {
    /**
     * Where the rust code is located
     */
    val packageDirectory: DirectoryProperty =
        project.objects.directoryProperty()
            .convention(project.layout.projectDirectory)
}