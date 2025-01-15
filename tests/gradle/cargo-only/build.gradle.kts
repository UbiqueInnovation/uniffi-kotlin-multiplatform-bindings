import io.gitlab.trixnity.gradle.cargo.rust.profiles.CargoProfile

plugins {
    id("cargo-tests")
}

// Defined in the workspace manifest
val myCustomProfile = CargoProfile("my-opt-level-2-profile")

cargo {
    features.add("feature2")
    variants {
        profile = myCustomProfile
        features.add("feature3")
    }
    builds.configureEach {
        features.add("feature5")
        variants {
            features.add("feature7")
        }
    }
}
