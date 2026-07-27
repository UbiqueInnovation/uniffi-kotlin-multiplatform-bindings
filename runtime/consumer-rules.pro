# Consumer rules applied to every app that (transitively) depends on the runtime.
#
# JNA ships no ProGuard rules of its own, so without these an R8-minified app
# crashes with `UnsatisfiedLinkError: Can't obtain peer field ID for class
# com.sun.jna.Pointer`: libjnidispatch.so resolves fields such as `Pointer.peer`
# by name from `Native.initIDs()`, and R8 has renamed them.
-keep class com.sun.jna.** { *; }

# The generated bindings map directly onto JNA, and every one of these mappings
# is by name:
#   - `UniffiLib : Library` method names are looked up as native symbols
#   - `*Struct : Structure` field names must match `@Structure.FieldOrder`
#   - `Callback` implementations are invoked from native code
# `extends` also matches `implements`, directly or indirectly, so this covers
# all three.
-keepclassmembers class * extends com.sun.jna.** { *; }

# `@Structure.FieldOrder` is read reflectively when the struct layout is built.
-keepattributes RuntimeVisibleAnnotations

# JNA references desktop-only APIs that do not exist on Android.
-dontwarn java.awt.**
