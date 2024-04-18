plugins {
    id("uniffi-tests")
}

uniffi {
    generateFromUdl {
        namespace = name.replace('-', '_')
        udlFile = layout.projectDirectory.file("src/$name.udl")
    }
}
