# Add project specific ProGuard rules here.

# Keep Chaquopy Python bridge
-keep class com.chaquo.python.** { *; }
-dontwarn com.chaquo.python.**

# Keep Room generated classes
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *

# Keep OSMDroid
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# Keep ZXing QR
-keep class com.google.zxing.** { *; }

# Keep our callback interface so Chaquopy can call it from Python
-keep interface com.estate.manager.rns.RnsCallback { *; }
-keep class com.estate.manager.rns.MessageRouter { *; }

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
