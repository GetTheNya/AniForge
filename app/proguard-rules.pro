# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep generic signatures and annotations for reflection (crucial for Gson mapping)
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses
-dontwarn com.google.gson.**

# Keep Gson serialization model classes in all modules
-keep class moe.GetTheNya.AniForge.core.network.model.** { *; }
-keep class moe.GetTheNya.AniForge.core.model.** { *; }
-keep class moe.GetTheNya.AniForge.core.database.entity.** { *; }