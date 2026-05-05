# OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-dontwarn okio.**
-keep class okio.** { *; }

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

# DataStore
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# Hilt
-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel

# Coroutines
-dontwarn kotlinx.coroutines.**

# Keep serializable data classes for network DTOs
-keep class com.lingualink.network.dto.** { *; }
-keep class com.lingualink.update.GitHubRelease { *; }
-keep class com.lingualink.update.GitHubAsset { *; }
