# Compose / Kotlin defaults are handled by the AGP-supplied rules.
-dontwarn org.jetbrains.annotations.**

# Cafe Bazaar Poolakey billing SDK
-keep class com.farsitel.bazaar.** { *; }
-keep interface com.farsitel.bazaar.** { *; }
-keep class ir.cafebazaar.** { *; }
-keep class com.phelat.poolakey.** { *; }

# Keep our monetization interfaces (plugged at runtime).
-keep class ir.hadipoor.eviltower.monetization.** { *; }
