plugins {
    id("uniffi-tests-from-library")
}

uniffi {
    generateFromLibrary {
        packageName = "runtime_test"
    }
}
