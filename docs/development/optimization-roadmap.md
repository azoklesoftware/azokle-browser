# 🚀 Azokle Browser — Deep Optimization & Community Evolution Plan

This document establishes the strategic, technical, and open-source enhancement roadmap for **Azokle Browser** following the completion of the 80/80 Core Architecture Milestone and repository open-sourcing.

---

## 🏛️ Strategic Optimization Pillars

```
+---------------------------------------------------------------------------------------+
|                               AZOKLE EVOLUTION ROADMAP                                |
+---------------------------+---------------------------+-------------------------------+
| 1. ENGINE & RUNTIME       | 2. PRIVACY & SECURITY     | 3. OPEN SOURCE & COMMUNITY    |
| • Baseline Profiles (AOT) | • Post-Quantum Kyber768   | • F-Droid Inclusion Pipeline  |
| • GeckoRuntime Prewarming | • ECH (Encrypted Hello)   | • Automated PR APK Previews   |
| • Flatbuffer Mmap Filters | • Biometric Vault Auth    | • Good-First-Issue Triage     |
| • Frame Rate & JankStats  | • Custom Filter Lists     | • MkDocs Interactive Site     |
+---------------------------+---------------------------+-------------------------------+
| 4. UX & COMPOSE POLISH    | 5. DESKTOP RUNTIME        | 6. CONTINUOUS ASSURANCE       |
| • Predictive Back Gesture | • GTK4 / Libadwaita Chrome| • Macrobenchmark Testing      |
| • Dynamic Material You    | • Shared SQLite Schema    | • cargo-fuzz Parser Audits    |
| • Foreground TTS Service  | • Hardware Video Decode   | • Dependency Diff Reviews     |
+---------------------------+---------------------------+-------------------------------+
```

---

## 1. Engine & Runtime Acceleration

### 1.1 Android Baseline Profiles (ART AOT Compilation)
- **Objective**: Eliminate Just-In-Time (JIT) interpretation lag during initial application startup.
- **Implementation**:
  - Add AndroidX Baseline Profile Gradle plugin (`androidx.baselineprofile`).
  - Generate `baseline-prof.txt` capturing startup critical paths: `AzokleApplication.onCreate`, `NestedGeckoView` composition, and `BrowserScreen` rendering.
  - **Expected Benefit**: **30% to 40% reduction in cold launch latency** and smoother initial scrolling frames.

### 1.2 Non-Blocking GeckoRuntime Pre-warming
- **Objective**: Prevent the first webpage load from waiting on GeckoView engine initialization.
- **Implementation**:
  - In `AzokleApplication.onCreate`, spawn a background dispatcher coroutine that initializes `GeckoRuntime.getDefault(this)` and primes font caches before `MainActivity` attaches its first session.
  - **Expected Benefit**: First URL navigation starts up to **400ms faster**.

### 1.3 Memory-Mapped Binary Flatbuffer Filters (`crates/privacy/adblock`)
- **Objective**: Drastically reduce memory overhead of storing 100,000+ AdBlock and tracking rules.
- **Implementation**:
  - Compile EasyList, EasyPrivacy, and uBlock defusers into a binary Flatbuffer (`adblock_cache.bin`).
  - Memory-map (`mmap`) the binary file directly into native memory from assets rather than allocating millions of JVM/Rust heap strings.
  - **Expected Benefit**: Saves **~35 MB to 50 MB of RAM** and eliminates filter parsing CPU spikes on startup.

---

## 2. Advanced Privacy, Cryptography & Security

### 2.1 Post-Quantum TLS & Encrypted Client Hello (ECH)
- **Objective**: Prevent ISPs, network operators, and MITM proxies from inspecting the requested domain name via Server Name Indication (SNI).
- **Implementation**:
  - In `GeckoConfigManager.kt`, enable Quantum-safe cryptography and ECH prefs:
    ```yaml
    security.tls.enable_kyber: true
    network.dns.echconfig.enabled: true
    network.dns.echconfig.fallback_to_origin_when_ech_fails: false
    network.http.referer.trimmingPolicy: 2
    network.http.referer.XOriginPolicy: 1
    ```
  - Combines with existing DNS-over-HTTPS (DoH) stack to achieve total ISP metadata blindness.

### 2.2 Biometric Keystore Guard for Password Vault
- **Objective**: Require physical device owner verification before rendering plaintext passwords or exporting backups.
- **Implementation**:
  - Integrate Android `BiometricPrompt` into `PasswordVaultSheet.kt` when tapping "Show Password", "Copy Password", or "Export Vault Backup".
  - Keys remain hardware-encrypted until authentication passes.

### 2.3 User-Configurable Custom Filter Subscriptions
- **Objective**: Empower power users to subscribe to specialized regional and topical blocklists (e.g., Fanboy Annoyances, AdGuard Base, regional lists).
- **Implementation**:
  - Add "Custom Filter Lists" UI under Shield Settings.
  - Implement periodic background background sync via `WorkManager` updating rules over DoH/Tor.

---

## 3. Open Source Community & Developer Experience (DX)

### 3.1 F-Droid Inclusion & Reproducible Build Recipe
- **Objective**: Distribute Azokle Browser on F-Droid, the premier open-source Android app repository.
- **Implementation**:
  - Ensure zero proprietary Google Play Services dependencies remain in the core build path.
  - Prepare `metadata/com.azokle.search.yml` specifying reproducible build flags and tag matching.

### 3.2 Automated Pull Request APK Previews (GitHub Actions)
- **Objective**: Enable non-developer community reviewers and designers to test PR changes on physical devices without checking out code.
- **Implementation**:
  - In `.github/workflows/ci.yml`, add an automated bot comment on PRs containing download links to the compiled debug APK artifact.

### 3.3 "Good First Issue" Curation & Community Discussions
- **Objective**: Accelerate contributor onboarding and community engagement.
- **Implementation**:
  - Create labeled issue buckets: `good-first-issue`, `help-wanted`, `ui-polish`, `engine-perf`.
  - Enable GitHub Discussions with categories: *Q&A*, *Show and Tell*, *Ideas & Proposals*, and *Security Discussions*.

### 3.4 Interactive Documentation Website (MkDocs / GitHub Pages)
- **Objective**: Host beautiful, searchable, responsive architectural documentation.
- **Implementation**:
  - Deploy Material for MkDocs on `https://azoklesoftware.github.io/azokle-browser`.
  - Automatically sync markdown files from `docs/` and API references on tag releases.

---

## 4. User Experience & Jetpack Compose Polish

### 4.1 Android 14/15 Predictive Back Gesture Support
- **Objective**: Provide smooth, native edge-swipe previews of previously visited pages.
- **Implementation**:
  - Hook GeckoView's navigation history into Android's `OnBackAnimationCallback`.
  - Render live Compose cross-fade progress animations when users swipe in from screen edges.

### 4.2 Foreground Service for Reader Mode "Read Aloud"
- **Objective**: Allow articles to continue playing when the screen turns off or when users switch to other apps.
- **Implementation**:
  - Promote `ReaderTextToSpeech` to an Android Foreground Service with `MediaSessionCompat` media notifications (Play, Pause, Skip, Stop).

### 4.3 Dynamic Color (Material You) Monet Engine
- **Objective**: Seamlessly adapt to Android's wallpaper-derived color palettes while retaining the pure-black AMOLED high-contrast mode.
- **Implementation**:
  - In `Theme.kt`, integrate `dynamicDarkColorScheme(context)` and `dynamicLightColorScheme(context)` with an option toggle in Settings ("Match System Accent").

---

## 5. Cross-Platform Desktop Maturation (`apps/linux`)

- **GTK4 & Libadwaita Migration**: Modernize desktop chrome from GTK3 to GTK4 with responsive header bars and tab strips matching GNOME/Linux HIG.
- **Unified Rust Database Driver**: Use the same encrypted SQLite schema across desktop and mobile for bookmarks and history, paving the way for P2P End-to-End Encrypted Sync.

---

## 📅 Phased Execution Waves

| Wave | Horizon | Focus Area | Deliverables |
| :---: | :---: | :--- | :--- |
| **Wave 1** | Immediate | Engine & Startup Acceleration | Baseline Profiles, ECH/Kyber768 TLS, Biometric Vault Auth |
| **Wave 2** | Short-Term | Community DX & F-Droid | PR APK Bot, F-Droid metadata, MkDocs Portal |
| **Wave 3** | Medium-Term | UX & Media Polish | Background TTS Audio Service, Predictive Back, Custom Filter URLs |
| **Wave 4** | Long-Term | Desktop & E2EE Sync | GTK4 Libadwaita Desktop App, P2P Sync Protocol |
