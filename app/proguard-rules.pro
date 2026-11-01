# Add project specific ProGuard rules here.

# Keep model data classes from being obfuscated or stripped
-keep class com.axwhaven.walls.model.** { *; }

# Coil image loading rules
-keep class coil.** { *; }
-dontwarn coil.**
