# ==============================================================================
# Azokle Browser - High-Efficiency R8 Optimization & ProGuard Rules
# ==============================================================================

# 1. Aggressive R8 Optimization & Code Shrinking Passes
-optimizationpasses 5
-allowaccessmodification
-overloadaggressively
-mergeinterfacesaggressively
-repackageclasses 'com.azokle.search.obf'

# 2. Critical Attributes for Stacktraces, Coroutines, and JNI
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# 3. Native JNI Method Signature Preservation
# Preserves all native method names and their descriptor classes across the app
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# 4. Azokle Core Rust Engine Bridge (libazokle_core.so)
-keepclassmembers class com.azokle.search.core.AzokleCore {
    public static native <methods>;
    public static <methods>;
}

# 5. Embedded Tor Onion Daemon & JNI Bridge (libtor.so)
-keep class org.torproject.** { *; }
-keepclassmembers class org.torproject.** { *; }
-keep class info.guardianproject.** { *; }
-keepclassmembers class info.guardianproject.** { *; }
-keep class com.msopentech.thali.toronionproxy.** { *; }
-keepclassmembers class com.msopentech.thali.toronionproxy.** { *; }

# 6. Mozilla GeckoView Quantum Engine Complete JNI & API Keeps
-keep class org.mozilla.geckoview.** { *; }
-keepclassmembers class org.mozilla.geckoview.** { *; }
-keep class org.mozilla.gecko.** { *; }
-keepclassmembers class org.mozilla.gecko.** { *; }

# 7. Jetpack Compose & Lifecycle Optimization
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# 8. Release Log Call Stripping (Eliminates verbose & debug log overhead)
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}

# 9. Suppress Warnings for Optional Reflection / Stubs
-dontwarn org.mozilla.gecko.**
-dontwarn org.mozilla.geckoview.**
-dontwarn info.guardianproject.**
-dontwarn org.torproject.**
-dontwarn com.msopentech.thali.toronionproxy.**
-dontwarn java.beans.**
-dontwarn org.yaml.snakeyaml.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn kotlin.**
-dontwarn kotlin.coroutines.jvm.internal.**
-dontwarn androidx.security.crypto.**
-dontwarn androidx.biometric.**
-dontwarn androidx.camera.**
-dontwarn com.google.zxing.**
