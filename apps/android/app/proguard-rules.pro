# R8 / Proguard Aggressive Optimization & Obfuscation
-optimizationpasses 5
-allowaccessmodification
-overloadaggressively
-mergeinterfacesaggressively
-repackageclasses 'com.azokle.search.obf'

# General attributes to preserve for stack traces and coroutines
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Native JNI Preservation
# 1. Preserve all native methods across the application
-keepclasseswithmembernames class * {
    native <methods>;
}

# 2. Azokle Core native Rust JNI bridge (libazokle_core.so)
-keep class com.azokle.search.core.AzokleCore {
    native <methods>;
}

# 3. Tor Service JNI Native bindings
-keep class org.torproject.jni.TorService {
    native <methods>;
}

# 4. GeckoView JNI & Engine Preservation
-keep class org.mozilla.gecko.** { *; }
-keep class org.mozilla.geckoview.** { *; }
-keep class org.yaml.snakeyaml.** { *; }

# 5. Warnings suppression for optional / reflection-based dependencies
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
