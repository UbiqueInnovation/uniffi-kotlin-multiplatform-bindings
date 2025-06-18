plugins {
    id("uniffi-tests")
}

uniffi {
    generateFromLibrary {
        namespace = name.replace("-", "_")
    }
}
