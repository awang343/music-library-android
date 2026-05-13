# R8 / ProGuard rules for release builds.
#
# Dependencies bundle their own consumer rules (Compose, Ktor, Media3, etc.),
# so this file only holds project-specific overrides. Add rules here if R8
# strips something that's actually needed at runtime.

# Keep kotlinx.serialization @Serializable types and their generated companions
# so JSON encoding/decoding survives obfuscation.
-keep,includedescriptorclasses class com.musiclib.**$$serializer { *; }
-keepclassmembers class com.musiclib.** {
    *** Companion;
}
-keepclasseswithmembers class com.musiclib.** {
    kotlinx.serialization.KSerializer serializer(...);
}
