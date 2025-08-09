# Keep Compose metadata
-keep class kotlin.Metadata { *; }
-keep class androidx.** { *; }
-dontwarn androidx.**

# If unluac.jar is included, try to keep its classes
-keep class unluac.** { *; }
-dontwarn unluac.**