# GeckoView Engine Integration (Android)

This directory documents the Android GeckoView engine delegates, runtime options, and lifecycle management used by Azokle Browser on Android.

## Architecture
- **Engine Provider**: `org.mozilla.geckoview:geckoview-omni:154.0.20260824154132` (16 KB page-size compliant).
- **Core Orchestrator**: `apps/android/app/src/main/java/com/azokle/search/engine/GeckoEngineManager.kt`.
- **Delegates**:
  - `AzokleNavigationDelegate.kt` (Shield interceptor, bounce mitigation, eTLD+1 boundary checks).
  - `AzokleProgressDelegate.kt` (Page loading indicators, security state transitions).
- **WebExtension Bridge**:
  - WebExtension messaging port over `GeckoWebExecutor` connecting `extensions/azokle-shield` directly to the Kotlin and native Rust layers.
