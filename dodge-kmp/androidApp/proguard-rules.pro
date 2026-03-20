# Keep serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class io.dodge.**$$serializer { *; }
-keepclassmembers class io.dodge.** { *** Companion; }
-keepclasseswithmembers class io.dodge.** { kotlinx.serialization.KSerializer serializer(...); }

# Keep Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
