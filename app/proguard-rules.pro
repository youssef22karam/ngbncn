# MediaPipe
-keep class com.google.mediapipe.** { *; }
-keepclassmembers class com.google.mediapipe.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** { volatile <fields>; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Compose
-keep class androidx.compose.** { *; }

# App data classes (Room entities)
-keep class com.jarvis.ai.data.** { *; }
