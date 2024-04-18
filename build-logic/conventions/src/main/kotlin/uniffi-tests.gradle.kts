import io.gitlab.trixnity.gradle.CargoHost
import io.gitlab.trixnity.gradle.cargo.dsl.jvm
import io.gitlab.trixnity.gradle.uniffi.tasks.InstallBindgenTask

plugins {
    id("host-jvm-native-tests")
    id("io.gitlab.trixnity.cargo.kotlin.multiplatform")
    id("io.gitlab.trixnity.uniffi.kotlin.multiplatform")
}

cargo {
    packageDirectory = layout.projectDirectory.dir("uniffi")
    builds.jvm {
        jvm.set(rustTarget == CargoHost.current.hostTarget)
    }
}

uniffi {
    bindgenCratePath = rootProject.layout.projectDirectory.dir("bindgen")
}

tasks.withType<InstallBindgenTask> {
    quiet = false
}
