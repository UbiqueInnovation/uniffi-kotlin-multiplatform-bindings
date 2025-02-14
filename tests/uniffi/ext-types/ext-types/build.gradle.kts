plugins {
    id("uniffi-tests-from-library")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.ktor.http)
                api(project(":examples:custom-types"))
                api(project(":tests:ext-types:custom-types"))
                api(project(":tests:ext-types:uniffi-one"))
                api(project(":tests:ext-types:sub-lib"))
            }
        }
    }
}