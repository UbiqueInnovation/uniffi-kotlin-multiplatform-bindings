plugins {
    id("uniffi-tests")
}

uniffi {
    generateFromLibrary {
        packageName = name.replace("-", "_")
    }
}
