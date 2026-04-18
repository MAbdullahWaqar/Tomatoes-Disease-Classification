# ProGuard configuration for Tomato Disease Classifier

# Keep main application class
-keep class com.tomato.disease.classifier.MainActivity { *; }

# Keep all composables and Compose-related classes
-keep class androidx.compose.** { *; }
-keep interface androidx.compose.** { *; }

# Keep TensorFlow Lite
-keep class org.tensorflow.lite.** { *; }
-keep interface org.tensorflow.lite.** { *; }
-keepclasseswithmembers class org.tensorflow.lite.** {
    native <methods>;
}

# Keep all data classes
-keepclasseswithmembers class com.tomato.disease.classifier.ml.** {
    public <methods>;
}

# Keep Coil image loader
-keep class coil.** { *; }
-keep interface coil.** { *; }

# Keep Kotlin metadata for reflection
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep service/broadcast receivers
-keep class com.tomato.disease.classifier.*.** {
    <init>(...);
}

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
