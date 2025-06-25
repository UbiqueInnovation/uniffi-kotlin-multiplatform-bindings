plugins {
    id("uniffi-tests-from-library")
}

uniffi {
    generateFromLibrary {
        namespace = "runtime_test"
    }
}
