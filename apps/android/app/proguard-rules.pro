# R8 / Proguard Aggressive Optimization & Obfuscation
-optimizationpasses 5
-allowaccessmodification
-repackageclasses 'com.azokle.search.obf'

# General attributes to preserve for stack traces and coroutines
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Native JNI Preservation
# 1. Native methods and classes containing native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# 2. Azokle Core JNI bridge for libazokle_core.so
-keep class com.azokle.search.core.AzokleCore {
    native <methods>;
    *;
}

# 3. Mozilla GeckoView & Gecko JNI (Full Native Preservation for libxul.so)
-keep class org.mozilla.gecko.** { *; }
-keep class org.mozilla.geckoview.** { *; }
-keepclassmembers class org.mozilla.gecko.** { *; }
-keepclassmembers class org.mozilla.geckoview.** { *; }
-keep interface org.mozilla.gecko.** { *; }
-keep interface org.mozilla.geckoview.** { *; }
-dontwarn org.mozilla.gecko.**
-dontwarn org.mozilla.geckoview.**

# 4. Tor & GuardianProject JNI
-keep class info.guardianproject.netcipher.** { *; }
-keep class org.torproject.jni.** { *; }
-keep class com.msopentech.thali.toronionproxy.** { *; }
-dontwarn info.guardianproject.**
-dontwarn org.torproject.**

# SnakeYAML & Java Beans warnings ignore
-dontwarn java.beans.**
-dontwarn org.yaml.snakeyaml.**

# OkHttp & Coroutines warnings ignore
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn kotlin.**
-dontwarn kotlin.coroutines.jvm.internal.**

# AndroidX Security Crypto & Biometrics
-keep class androidx.security.crypto.** { *; }
-keep class androidx.biometric.** { *; }
-dontwarn androidx.security.crypto.**
-dontwarn androidx.biometric.**
