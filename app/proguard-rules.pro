# Ktor
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { volatile <fields>; }
-keep class io.ktor.client.engine.** { *; }
-keep class io.ktor.serialization.** { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.lingualink.**$$serializer { *; }
-keepclassmembers class com.lingualink.** { *** Companion; }
-keepclasseswithmembers class com.lingualink.** { kotlinx.serialization.KSerializer serializer(...); }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ML Kit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
