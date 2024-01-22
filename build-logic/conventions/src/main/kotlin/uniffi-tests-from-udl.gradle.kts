plugins {
    id("uniffi-tests")
}

uniffi {
    generateFromUdl {
        crateDirectory = layout.projectDirectory.dir("uniffi")
        namespace = name.replace('-', '_')
        udlFile = crateDirectory.get().dir("src").file("$name.udl")
    }
}
