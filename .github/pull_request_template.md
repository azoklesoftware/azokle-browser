## 📝 Summary of Changes

A concise description of what this PR introduces, fixes, or refactors.

Fixes #(issue)

---

## 🔍 Type of Change

- [ ] 🐛 Bug fix (non-breaking change which fixes an issue)
- [ ] ✨ New feature (non-breaking change which adds functionality)
- [ ] 🛡️ Security / Privacy hardening
- [ ] ⚡ Performance optimization
- [ ] 🧹 Refactoring / Code cleanup
- [ ] 📄 Documentation update

---

## 🧪 Quality Gate Verification

Before submitting this PR, please check that you have completed the following:

- [ ] **Target Branch**: This PR targets the `development` branch (not `main`).
- [ ] **Unit Tests**: Ran `./gradlew testDebugUnitTest` and/or `cargo test --workspace` with 100% passing tests.
- [ ] **SBOM Verification**: Ran `./gradlew generateSbom` without configuration cache errors.
- [ ] **Linter & Formatting**: Passed code formatting and lint checks (`cargo clippy` / Compose guidelines).
- [ ] **Zero-Telemetry Invariant**: Verified no third-party tracking, analytics, or unauthenticated outbound network pings were added.
- [ ] **Changelog**: Updated `.azokle/changelog/` for user-facing changes if applicable.
