# Release Management & CI/CD Pipeline

This document details the multi-platform packaging matrices, release artifact generation, and continuous integration workflows for Azokle Browser.

---

## 1. Multi-Platform Artifact Matrix

| Platform | Format | Packaging Tool | Target Distribution |
| :--- | :--- | :--- | :--- |
| **Android** | `.aab` / `.apk` | Gradle (`bundleRelease`, `assembleRelease`) | Google Play, F-Droid, GitHub Releases |
| **Linux** | `.deb` / `.rpm` / `.tar.gz` | CMake CPack / FPM | Ubuntu, Debian, Fedora, Arch Linux |
| **Windows** | `.msix` / `.exe` | WiX Toolset / MSIX Packaging Tool | Windows Package Manager (winget), GitHub Releases |
| **macOS** | `.dmg` / `.app` | Xcode `xcodebuild` / `create-dmg` | Homebrew Cask, Direct Download |
| **iOS** | `.ipa` | Xcode Archive / TestFlight | Apple App Store, AltStore |

---

## 2. Release Signing Requirements

1. **Android APK/AAB**:
   - Must be signed with a production v2/v3 signing key (`apksigner`).
   - `build.gradle.kts` dynamically reads release keystore parameters from environment variables (`KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`).
2. **Apple (macOS/iOS)**:
   - macOS binaries must be signed with an Apple Developer ID certificate and notarized via `xcrun notarytool`.
   - Hardened runtime capability must be enabled.
3. **Windows Executables**:
   - Binaries signed using an Authenticode code-signing certificate via `signtool.exe`.

---

## 3. GitHub Actions CI/CD Pipeline Structure

Located in `.github/workflows/` (or repository CI runners):
- **`ci.yml`**:
  - Matrix runner executing on `ubuntu-latest`, `windows-latest`, and `macos-latest`.
  - Runs `cargo test --workspace`, `cargo clippy`, and `cargo fmt --check`.
  - Runs `cmake --build build` across Linux and Windows.
  - Builds Android debug APK with caching for Gradle and Rust artifacts.
- **`release.yml`**:
  - Triggered upon version tag push (e.g. `v2.0.0`).
  - Assembles multi-platform release binaries, computes SHA-256 checksums, and publishes GitHub Release assets.
