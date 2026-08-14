import ch.ubique.uniffi.plugin.model.RustHost

plugins {
    id("uniffi-tests-from-library")
}

/*
 * `@Throws` is what makes a throwing method reachable as `throws` from the outside: on the jvm it
 * becomes the `throws` clause in the class file, on apple targets it becomes an `NSError` out
 * parameter in the generated objective-c header. The jvm side is covered by the java test, this
 * covers the apple side.
 *
 * The bindings deliberately leave the `@Throws` off the native overrides, see the comment in
 * `bindgen/src/templates/macros.kt`, so this pins the assumption that makes that safe: an override
 * inherits the filter of the interface method it implements, and is exported with the out parameter
 * all the same.
 *
 * Objective-c headers only exist for apple targets, so this only runs on a mac.
 */
if (RustHost.Platform.MacOS.isCurrent) {
    val frameworkName = "InterfaceThrows"

    kotlin {
        macosArm64 {
            binaries.framework {
                baseName = frameworkName
            }
        }
    }

    // The declarations that have to carry the out parameter: the protocol generated for the trait,
    // and the class generated for the object implementing it. The class is the interesting one,
    // its `greet` is the unannotated override.
    val exportedGreeters = listOf("${frameworkName}Greeter", "${frameworkName}RustGreeter")

    val checkObjCThrows = tasks.register("checkObjCThrows") {
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        description = "Checks that throwing methods are exported with an NSError out parameter."

        // `outputs` of the link task is the directory the framework bundle is written into.
        val header = tasks.named("linkDebugFrameworkMacosArm64").map { link ->
            link.outputs.files.singleFile
                .resolve("$frameworkName.framework/Headers/$frameworkName.h")
        }
        inputs.file(header).withPropertyName("header")

        val report = layout.buildDirectory.file("reports/objc-throws.txt")
        outputs.file(report).withPropertyName("report")

        doLast {
            val headerFile = header.get()

            // The `greet` declarations, grouped by the class or protocol declaring them.
            val declarations = mutableMapOf<String, MutableList<String>>()
            var owner: String? = null

            headerFile.forEachLine { line ->
                val trimmed = line.trim()
                when {
                    // `@class A, B;` and `@protocol A, B;` are forward declarations, not a body.
                    trimmed.endsWith(";") && !trimmed.contains("greetName:") -> Unit

                    trimmed.startsWith("@interface") || trimmed.startsWith("@protocol") ->
                        owner = trimmed
                            .removePrefix("@interface")
                            .removePrefix("@protocol")
                            .trimStart()
                            .takeWhile { !it.isWhitespace() && it != ':' && it != '<' && it != '(' }

                    trimmed == "@end" -> owner = null

                    trimmed.contains("greetName:") ->
                        owner?.let { declarations.getOrPut(it) { mutableListOf() }.add(trimmed) }
                }
            }

            val checked = exportedGreeters.map { greeter ->
                val greets = declarations[greeter].orEmpty()
                require(greets.isNotEmpty()) {
                    "$greeter declares no `greet` in ${headerFile.name}, has the fixture been renamed?"
                }
                greets.forEach { declaration ->
                    require(declaration.contains("error:(NSError")) {
                        "$greeter.greet is exported without an NSError out parameter, swift callers " +
                            "cannot catch GreeterException:\n    $declaration"
                    }
                }
                "$greeter: ${greets.size} declaration(s) with an NSError out parameter"
            }

            report.get().asFile.writeText(checked.joinToString("\n", postfix = "\n"))
        }
    }

    tasks.named("check") {
        dependsOn(checkObjCThrows)
    }
}
