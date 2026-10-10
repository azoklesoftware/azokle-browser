# 🛠️ Contributing to Azokle Browser

Thank you for your interest in contributing to **Azokle Browser**! We are building an uncompromising, high-performance, privacy-first web browser designed to give users control over their data, memory, and browsing freedom.

---

## 📜 Code of Conduct

All contributors, maintainers, and community members are expected to uphold a respectful, inclusive, and professional environment. Harassment, derogatory comments, and exclusionary behavior will not be tolerated.

---

## 🌿 Branching Strategy & Git Workflow

We maintain a structured Git branching workflow:

- **`main`**: Production-ready, stable releases only. Tags and release builds are cut from here.
- **`development`**: Active integration branch. **All Pull Requests and everyday feature contributions must target `development`.**
- **Feature Branches**: Branch off `development` using descriptive naming:
  - `feature/<feature-name>` (e.g. `feature/reader-tts`)
  - `fix/<bug-description>` (e.g. `fix/app-link-loop`)
  - `sec/<security-enhancement>` (e.g. `sec/doh-leak-guard`)

---

## 💻 Development Environment Setup

### 1. Prerequisites
- **Android SDK & NDK**: API 35 (Android 15), Min SDK 26 (Android 8.0), NDK `26.1.10909125+`.
- **Java Development Kit**: OpenJDK 17 or higher.
- **Rust Toolchain**: Rust 1.75+ stable (`rustup update stable`).
  - Add Android cross-compile targets if building native JNI:
    ```bash
    rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android
    ```
- **Build Tools**: CMake 3.22+, Gradle 8.5+, Ninja, `cargo`.

### 2. Workspace Initialization
```bash
git clone https://github.com/azoklesoftware/azokle-browser.git
cd azokle-browser
git checkout development
```

### 3. Verification Builds
- **Rust Core**:
  ```bash
  cargo check --workspace
  cargo test --workspace
  ```
- **Android Application**:
  ```bash
  cd apps/android
  ./gradlew testDebugUnitTest
  ./gradlew generateSbom
  ```

---

## 🎨 Code Style & Quality Standards

### Kotlin & Android (`apps/android`)
- **Jetpack Compose First**: All new UI components must be built with declarative Jetpack Compose and Material 3 design tokens.
- **Theme Consistency**: Use color tokens from `AzokleTheme.colors` (`cardBg`, `surface`, `border`, `actionPrimaryBg`, etc.) rather than hardcoded hex colors to maintain seamless Light/Dark/AMOLED support.
- **Engine Delegate Boundaries**: Follow the established Mozilla GeckoView architecture. Keep UI logic in Compose state hosts and delegate web engine interactions through `AzokleContentDelegate`, `AzokleNavigationDelegate`, or `GeckoPromptDelegateImpl`.
- **Zero-Leak Lifecycle**: Ensure open prompts, dialogs, and coroutines cleanly dismiss when tabs change or tabs close.

### Rust Core (`crates/*`)
- **Formatting**: Code must pass `cargo fmt --check`.
- **Lints**: Code must compile cleanly under `cargo clippy --workspace --all-targets -- -D warnings`.
- **Memory Safety**: Parsers, network filters, and URL handlers must declare `#![forbid(unsafe_code)]`.
- **Panic Boundaries**: FFI functions exposed to JNI or C must wrap logic with `std::panic::catch_unwind`.

---

## 🛡️ Privacy & Zero-Telemetry Invariants

Azokle Browser enforces non-negotiable privacy constraints:
1. **Never introduce third-party analytics, crash beacons, or tracking dependencies.**
2. **Never log sensitive data** (URLs, passwords, form entries, or search queries) in production logs or uncaught exception handlers.
3. **Tor & Private Mode Isolation**: Ensure all new features respect private browsing and Tor tabs (no persistence of credentials, history, or cookies).

---

## 🚀 Submitting a Pull Request (PR)

1. **Fork & Branch**: Create your branch off `development`.
2. **Make Atomic Commits**: Write clear, descriptive commit messages conforming to conventional commits (`feat:`, `fix:`, `refactor:`, `test:`, `docs:`).
3. **Run All Quality Gates**:
   ```bash
   # In apps/android
   ./gradlew testDebugUnitTest generateSbom
   
   # In workspace root
   cargo test --workspace
   ```
4. **Update Changelog**: Add notes under `.azokle/changelog/android/` for any user-facing or architectural changes.
5. **Open Pull Request**: Open your PR against the `development` branch of `https://github.com/azoklesoftware/azokle-browser`. Include:
   - Clear description of the problem solved.
   - Screenshots or recordings for visual UI changes.
   - Reference to associated issues.

---

## 💬 Community & Questions

For questions or discussions regarding browser architecture, please open a GitHub Discussion or join our developer channels. Thank you for making the web safer and faster with Azokle!
