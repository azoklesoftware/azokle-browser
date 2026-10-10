# 📜 Azokle Browser — Release & Hardening Changelog

All notable changes to the Azokle Browser project across all platforms are documented here and in versioned release manifests under [`.azokle/changelog/`](.azokle/changelog/).

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [v2.0.6] — 2026-10-10

### 🛡️ Android & Web Engine Hardening Release
Detailed release specifications and verification artifacts can be reviewed in [`.azokle/changelog/android/v-2.0.6.md`](.azokle/changelog/android/v-2.0.6.md).

#### Added
- **Text-to-Speech "Read Aloud" Engine**: Native Android `TextToSpeech` integration in Reader Mode with interactive Listen / Pause / Resume controls (`ReaderTextToSpeech.kt`, `ReaderModeScreen.kt`).
- **Encrypted Password Manager & Autofill Vault**: Zero-knowledge AES-256-GCM vault with Material 3 management UI (`PasswordVaultManager.kt`, `PasswordVaultSheet.kt`).
- **Enterprise Management Policy Engine**: MDM `RestrictionsManager` and `policies.json` enforcement (`EnterprisePolicyManager.kt`).
- **Software Bill of Materials (SBOM)**: Automated CycloneDX 1.5 JSON generation task cataloging all 151 dependencies (`build.gradle.kts`).
- **Automated Security Scan CI**: Aqua Security Trivy vulnerability scanner and regression pipeline (`.github/workflows/security-scan.yml`).
- **Staged Rollout Pipeline**: Fastlane 10% → 25% → 50% → 100% staged distribution lanes (`apps/android/fastlane/Fastfile`).
- **Web Platform Standards & Compatibility Suite**: Full test matrix expansion covering 42 test suites with 100% pass rate.

#### Changed
- **GeckoSession PromptDelegate Modernization**: 100% coverage (all 25 callback methods) in `GeckoPromptDelegateImpl.kt`, including dialog leak registry and KeyChain mTLS client certificates.
- **App Links & Deep Link Targeting**: Direct package targeting breaking infinite loop redirects when opening native apps (`AppLinkHandler.kt`).
- **Edge Gesture Navigation Hardening**: 32dp left/right exclusion zones in `NestedGeckoView.kt` suppressing false context menus during edge back swipes.
- **Data Incineration Hardening**: Elimination of race conditions and multi-file shredding for legacy tab state backups (`DataBurnEngine.kt`).

---

## Prior Releases
Historical release notes for legacy and desktop builds can be inspected in the Git commit history and platform tags.
