package ch.ubique.uniffi.plugin.model

enum class BuildTarget(
    /**
     * The default name of the sourceSet used for kotlin multiplatform
     */
    val sourceSetName: String,

    /**
     * The name of the kotlin build target
     */
    val targetName: String,

    /**
     * Targets to build and include for a debug build
     */
    val debugTargets: List<RustTarget>,

    /**
     * Targets to build and include for a release build
     */
    val releaseTargets: List<RustTarget>,

    /**
     * An additional set of targets to always build.
     */
    val baseTargets: List<RustTarget> = listOf(),

    /**
     * Build with the dynamic library
     */
    val useDynamicLib: Boolean? = null
) {
    Jvm(
        sourceSetName = "jvmMain",
        targetName = "jvm",
        debugTargets = listOf(
            RustTarget.forCurrentPlatform
        ),
        releaseTargets = listOf(
            RustTarget.Aarch64AppleDarwin,
            RustTarget.X64AppleDarwin,
            RustTarget.Aarch64LinuxGnu,
            RustTarget.X64LinuxGnu,
            RustTarget.X64WindowsGnu,
        )
    ),

    Android(
        sourceSetName = "androidMain",
        targetName = "android",
        baseTargets = listOf(
            // For executing "android (local)" tests
            RustTarget.forCurrentPlatform,
        ),
        debugTargets = listOf(
            RustTarget.androidTargetForCurrentPlatform,
        ),
        releaseTargets = listOf(
            RustTarget.Aarch64Android,
            RustTarget.X64Android,
            RustTarget.ArmV7Android,
        ),
    ),

    // The native targets are architecture specific
    MacosArm64(
        sourceSetName = "macosArm64Main",
        targetName = "macosArm64",
        debugTargets = listOf(RustTarget.Aarch64AppleDarwin),
        releaseTargets = listOf(RustTarget.Aarch64AppleDarwin)
    ),
    MacosX64(
        sourceSetName = "macosX64Main",
        targetName = "macosX64",
        debugTargets = listOf(RustTarget.X64AppleDarwin),
        releaseTargets = listOf(RustTarget.X64AppleDarwin)
    ),

    LinuxAarch64(
        sourceSetName = "linuxArm64Main",
        targetName = "linuxArm64",
        debugTargets = listOf(RustTarget.Aarch64LinuxGnu),
        releaseTargets = listOf(RustTarget.Aarch64LinuxGnu)
    ),
    LinuxX64(
        sourceSetName = "linuxX64Main",
        targetName = "linuxX64",
        debugTargets = listOf(RustTarget.X64LinuxGnu),
        releaseTargets = listOf(RustTarget.X64LinuxGnu)
    ),

    WindowsX64(
        sourceSetName = "mingwX64Main",
        targetName = "mingwX64",
        debugTargets = listOf(RustTarget.X64WindowsGnu),
        releaseTargets = listOf(RustTarget.X64WindowsGnu),
    ),
    // NOTE: WindowsArm64 doesn't seem to build out of the box.
    //       Using `cargo zigbuild` does work though, and maybe
    //       can be implemented later (or using `cargo cross`).

    IosArm64(
        sourceSetName = "iosArm64Main",
        targetName = "iosArm64",
        debugTargets = listOf(RustTarget.Aarch64AppleIos),
        releaseTargets = listOf(RustTarget.Aarch64AppleIos),
    ),
    IosX64(
        sourceSetName = "iosX64Main",
        targetName = "iosX64",
        debugTargets = listOf(RustTarget.X64AppleIos),
        releaseTargets = listOf(RustTarget.X64AppleIos),
    ),
    IosSimulatorArm64(
        sourceSetName = "iosSimulatorArm64Main",
        targetName = "iosSimulatorArm64",
        debugTargets = listOf(RustTarget.Aarch64AppleIosSimulator),
        releaseTargets = listOf(RustTarget.Aarch64AppleIosSimulator)
    );

    enum class RustTarget(
        /**
         * The target triple string passed via --target argument to Cargo commands to build for this target.
         */
        val rustTriple: String,

        /**
         * Where the native library should be located within the JAR, such that JNA can find it.
         */
        val jarLibraryPath: String,

        /**
         * ABI Name of the target. Also used by JNI to find the correct library.
         */
        val abiName: String? = null,

        /**
         * The LLVM triple prefix of the ABI, which is used by the LLVM toolchain in NDK.
         */
        val ndkLlvmTriple: String = rustTriple,
    ) {
        // iOS
        Aarch64AppleIos("aarch64-apple-ios", "ios-aarch64"),
        X64AppleIos("x86_64-apple-ios", "ios-x86-64"),
        Aarch64AppleIosSimulator("aarch64-apple-ios-sim", "ios-sim-aarch64"),

        // MacOS
        Aarch64AppleDarwin("aarch64-apple-darwin", "darwin-aarch64"),
        X64AppleDarwin("x86_64-apple-darwin", "darwin-x86-64"),

        // Linux
        Aarch64LinuxGnu(
            "aarch64-unknown-linux-gnu",
            "linux-aarch64",
        ),
        X64LinuxGnu(
            "x86_64-unknown-linux-gnu",
            "linux-x86-64",
        ),

        // Windows
        Aarch64WindowsMsvc("aarch64-pc-windows-msvc", "win32-aarch64"),
        X64WindowsMsvc("x86_64-pc-windows-msvc", "win32-x86-64"),
        X64WindowsGnu("x86_64-pc-windows-gnu", "win32-x86-64"),

        // Android
        Aarch64Android(
            "aarch64-linux-android",
            "android-aarch64",
            abiName = "arm64-v8a",
        ),
        ArmV7Android(
            "armv7-linux-androideabi",
            "android-arm",
            abiName = "armeabi-v7a",
            ndkLlvmTriple = "armv7a-linux-androideabi",
        ),
        X64Android(
            "x86_64-linux-android",
            "android-x86-64",
            abiName = "x86_64",
        );

        fun dynamicLibraryName(packageName: String): String? = when (this) {
            Aarch64AppleDarwin, X64AppleDarwin, Aarch64AppleIosSimulator, Aarch64AppleIos, X64AppleIos ->
                CrateType.SystemDynamicLibrary.outputFileNameForMacOS(packageName)

            Aarch64LinuxGnu, X64LinuxGnu, Aarch64Android, ArmV7Android, X64Android ->
                CrateType.SystemDynamicLibrary.outputFileNameForLinux(packageName)

            Aarch64WindowsMsvc, X64WindowsMsvc ->
                CrateType.SystemDynamicLibrary.outputFileNameForMsvc(packageName)

            X64WindowsGnu ->
                CrateType.SystemDynamicLibrary.outputFileNameForMsvc(packageName)
        }

        fun staticLibraryName(packageName: String): String? = when (this) {
            Aarch64AppleDarwin, X64AppleDarwin, Aarch64AppleIosSimulator, Aarch64AppleIos, X64AppleIos ->
                CrateType.SystemStaticLibrary.outputFileNameForMacOS(packageName)

            Aarch64LinuxGnu, X64LinuxGnu, Aarch64Android, ArmV7Android, X64Android ->
                CrateType.SystemStaticLibrary.outputFileNameForLinux(packageName)

            Aarch64WindowsMsvc, X64WindowsMsvc ->
                CrateType.SystemStaticLibrary.outputFileNameForMsvc(packageName)

            X64WindowsGnu ->
                CrateType.SystemStaticLibrary.outputFileNameForMinGW(packageName)
        }

        val isAndroid: Boolean
            get() = when (this) {
                Aarch64AppleDarwin, X64AppleDarwin, Aarch64LinuxGnu, X64LinuxGnu,
                Aarch64WindowsMsvc, X64WindowsMsvc, X64WindowsGnu,
                Aarch64AppleIosSimulator, Aarch64AppleIos, X64AppleIos
                    -> false

                Aarch64Android, ArmV7Android, X64Android -> true
            }

        val konanName: String
            get() = when (this) {
                // iOS
                Aarch64AppleIos -> "ios_arm64"
                X64AppleIos -> "ios_x64"
                Aarch64AppleIosSimulator -> "ios_simulator_arm64"

                // macOS
                Aarch64AppleDarwin -> "macos_arm64"
                X64AppleDarwin -> "macos_x64"

                // Linux
                Aarch64LinuxGnu -> "linux_arm64"
                X64LinuxGnu -> "linux_x64"

                // Windows
                Aarch64WindowsMsvc -> "mingw_arm64"
                X64WindowsMsvc -> "mingw_x64"
                X64WindowsGnu -> "mingw_x64"

                // Android
                Aarch64Android -> "android_arm64"
                ArmV7Android -> "android_arm32"
                X64Android -> "android_x64"
            }

        companion object {
            val forCurrentPlatform: RustTarget
                get() = when (RustHost.Platform.current) {
                    RustHost.Platform.MacOS -> when (RustHost.Arch.current) {
                        RustHost.Arch.Arm64 -> Aarch64AppleDarwin
                        RustHost.Arch.X64 -> X64AppleDarwin
                    }

                    RustHost.Platform.Linux -> when (RustHost.Arch.current) {
                        RustHost.Arch.Arm64 -> Aarch64LinuxGnu
                        RustHost.Arch.X64 -> X64LinuxGnu
                    }

                    RustHost.Platform.Windows -> when (RustHost.Arch.current) {
                        RustHost.Arch.Arm64 -> Aarch64WindowsMsvc
                        RustHost.Arch.X64 -> X64WindowsMsvc
                    }
                }

            val androidTargetForCurrentPlatform: RustTarget
                get() = when (RustHost.Platform.current) {
                    RustHost.Platform.MacOS -> when (RustHost.Arch.current) {
                        RustHost.Arch.Arm64 -> Aarch64Android
                        RustHost.Arch.X64 -> X64Android
                    }

                    RustHost.Platform.Linux -> when (RustHost.Arch.current) {
                        RustHost.Arch.Arm64 -> Aarch64Android
                        RustHost.Arch.X64 -> X64Android
                    }

                    RustHost.Platform.Windows -> when (RustHost.Arch.current) {
                        RustHost.Arch.Arm64 -> Aarch64Android
                        RustHost.Arch.X64 -> X64Android
                    }
                }
        }
    }

    val targets: List<RustTarget>
        get() = (baseTargets + debugTargets + releaseTargets).distinct()

    val debugTargetsAll: List<RustTarget>
        get() = baseTargets + debugTargets

    val releaseTargetsAll: List<RustTarget>
        get() = baseTargets + releaseTargets

    val checkedNativeTarget: RustTarget
        get() {
            check(debugTargets.size == 1) {
                "$name has more than one debug target"
            }

            check(releaseTargets.size == 1) {
                "$name has more than one release target"
            }

            check(debugTargets[0] == releaseTargets[0]) {
                "$name has different debug and release targets"
            }

            return releaseTargets[0]
        }

    companion object {
        fun fromSourceSetName(name: String): BuildTarget? =
            BuildTarget.entries.find { it.sourceSetName == name }

        fun fromTargetName(name: String): BuildTarget? =
            BuildTarget.entries.find { it.targetName == name }

        val nativeTargets: List<BuildTarget> = listOf(
            MacosArm64, MacosX64,
            LinuxAarch64, LinuxX64,
            WindowsX64,
            IosSimulatorArm64, IosArm64, IosX64
        )
    }
}