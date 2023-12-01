plugins {
    id("uniffi-tests")
}

uniffi {
    val prefixedName = "uniffi-kmm-fixture-$name"

    generateFromUdl {
        crateDirectory = layout.projectDirectory.dir("uniffi")
        crateName = prefixedName
        libraryName = prefixedName.replace('-', '_')
        namespace = name.replace('-', '_')
        udlFile = crateDirectory.get().dir("src").file("$name.udl")
    }
}
