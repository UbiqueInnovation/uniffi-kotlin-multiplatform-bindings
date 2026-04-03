plugins {
    id("uniffi-tests-from-library")
}

uniffi {
	generateBindingsForExternalCrates = true
}
kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.ktor.client.core)
                api(project(":examples:custom-types"))
                api(project(":tests:uniffi:ext-types:custom-types"))
                api(project(":tests:uniffi:ext-types:uniffi-one"))
                api(project(":tests:uniffi:ext-types:sub-lib"))
            }
        }
    }
}