import io.gitlab.trixnity.uniffi.gradle.tasks.*

plugins {
    id("uniffi-multiplatform")
    id("io.gitlab.trixnity.uniffi.kotlin.multiplatform")
}

uniffi {
    bindgenCratePath = rootProject.layout.projectDirectory.dir("bindgen")
}

tasks.withType<InstallBindgenTask> {
    quiet = false
}
