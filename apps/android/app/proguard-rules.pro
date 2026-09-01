# Mozilla GeckoView & Gecko JNI (Full Native Preservation for libxul.so)
-keep class org.mozilla.gecko.** { *; }
-keep class org.mozilla.geckoview.** { *; }
-keepclassmembers class org.mozilla.gecko.** { *; }
-keepclassmembers class org.mozilla.geckoview.** { *; }
-keep interface org.mozilla.gecko.** { *; }
-keep interface org.mozilla.geckoview.** { *; }
-dontwarn org.mozilla.gecko.**
-dontwarn org.mozilla.geckoview.**

# SnakeYAML & Java Beans warnings ignore
-dontwarn java.beans.**
-dontwarn org.yaml.snakeyaml.**

# OkHttp & Coroutines
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-dontwarn okhttp3.**
-dontwarn okio.**

# Jetpack Compose & KotlinX
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
-dontwarn kotlin.**
-dontwarn kotlin.coroutines.jvm.internal.**

# AndroidX Security Crypto & Biometrics
-keep class androidx.security.crypto.** { *; }
-keep class androidx.biometric.** { *; }
-dontwarn androidx.security.crypto.**
-dontwarn androidx.biometric.**

# Tor & GuardianProject JNI
-keep class info.guardianproject.netcipher.** { *; }
-keep class org.torproject.jni.** { *; }
-keep class com.msopentech.thali.toronionproxy.** { *; }
-dontwarn info.guardianproject.**
-dontwarn org.torproject.**

# Azokle Data & WebExtension Models
-keep class com.azokle.search.data.** { *; }
-keep class com.azokle.search.browser.** { *; }

