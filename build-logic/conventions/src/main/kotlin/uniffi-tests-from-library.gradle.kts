plugins {
    id("uniffi-tests")
}

uniffi {
    val prefixedName = "uniffi-kmm-fixture-$name"

    generateFromLibrary {
        crateDirectory = layout.projectDirectory.dir("uniffi")
        crateName = prefixedName
        libraryName = prefixedName.replace('-', '_')
        namespace = name.replace('-', '_')
    }
}
