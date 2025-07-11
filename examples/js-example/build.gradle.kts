import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    js {
        moduleName = "js-example"

        browser()
        nodejs()

        binaries.library()
    }

    sourceSets {

    }
}

tasks.withType<KotlinJsCompile>().configureEach {
    compilerOptions {
        languageVersion = KotlinVersion.KOTLIN_2_1
        target = "es2015"
    }
}
