# 📜 Azokle Browser — Release & Hardening Changelog

All notable changes to the Azokle Browser project across all platforms are documented here and in versioned release manifests under [`.azokle/changelog/`](.azokle/changelog/).

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [v2.0.9] — 2026-10-15

### 🛡️ Independent Deep Security Audit Remediation Release
Detailed audit findings remediation and verification artifacts can be reviewed in [`.azokle/changelog/android/v-2.0.9.md`](.azokle/changelog/android/v-2.0.9.md).

#### Fixed & Hardened (68 Audit Findings Addressed)
- **CSS Injection & Filter-list Sanitization (AZK-F-001)**: Enforced strict CSS selector sanitization preventing style breakouts (`{`, `}`, `/*`, `url(`) and enabled validation features in adblock engine.
- **Component Synchronization & DOM Hardening (AZK-F-002)**: Synchronized and hardened duplicate modular chrome components (`BookmarksBar.ts`, `TorCircuitModal.ts`, `Omnibar.ts`) with safe DOM creation and HTML escaping.
- **WHATWG URL Parsing & Normalization (AZK-F-005, AZK-F-006)**: Restored ASCII tab/LF/CR stripping and lowercase host normalization in adblock engine to eliminate tracker filter bypasses.
- **Omnibox Search Query Percent-Encoding (AZK-F-007)**: Implemented robust `application/x-www-form-urlencoded` serialization preventing parameter injection (`&`, `#`, `?`, `%`).
- **Apex Sovereign Domain Whitelisting (AZK-F-013, AZK-F-033)**: Replaced loose substring checks with exact and suffix domain matching; restricted Cloudflare challenge path exemptions.
- **Flatbuffer Bounds & Deserialization Resilience (AZK-F-014, AZK-F-015, AZK-F-016, AZK-F-019)**: Enforced runtime length validation, bounded hash table probe loops to prevent infinite DoS, added regex slicing checks, and handled scriptlet arguments safely.
- **FFI & JNI Panic Barriers (AZK-F-020, AZK-F-021)**: Wrapped all 87 JNI entry points with `catch_unwind` and removed `panic = "abort"` from release profile to protect host process integrity.
- **Privacy Telemetry & Hotlink Elimination (AZK-F-027, AZK-F-028, AZK-F-029)**: Gated omnibox autosuggestions on non-Tor and non-private sessions; eliminated third-party Google favicon tracking and Unsplash wallpaper hotlinks in favor of local CSS gradients.
- **iOS Keychain & Certificate Trust Realism (AZK-F-045, AZK-F-046)**: Replaced destructive key wipes on transient errors with fail-safe recovery; removed decorative/fabricated certificate hashes and dates in favor of accurate OS WebKit verification status.
- **CI/CD Supply Chain Hardening (AZK-F-055, AZK-F-056, AZK-F-057, AZK-F-059, AZK-F-060, AZK-F-067)**: Pinned all GitHub Actions by immutable 40-character commit SHAs, added Gradle wrapper validation, enforced fail-closed signing, and generated SHA256 checksums.
- **Desktop UI Dependency & CSP Hardening (AZK-F-064, AZK-F-065)**: Locked `@types/bun` to `^1.3.5` in lockfile and added `frame-src 'self' https: http:;` to UI CSP.

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
