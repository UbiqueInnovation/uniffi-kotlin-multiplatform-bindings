plugins {
    id("uniffi-tests")
}

uniffi {
    generateFromLibrary {
        crateDirectory = layout.projectDirectory.dir("uniffi")
        namespace = name.replace('-', '_')
    }
}
