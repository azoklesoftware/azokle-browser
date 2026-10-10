# 🔒 Security Policy & Vulnerability Reporting

The Azokle Software team is dedicated to providing an uncompromised, privacy-first web browser built with rigorous memory safety, native engine sandboxing, and zero third-party telemetry.

---

## 🛡️ Supported Versions

Security patches and vulnerability updates are actively maintained for the following versions:

| Platform / Component | Supported Versions | Status |
| :--- | :--- | :--- |
| **Android Application** (`apps/android`) | `v2.0.x` | 🟢 Active Security Support |
| **Rust Core Engine** (`crates/*`) | `v1.0.x` | 🟢 Active Security Support |
| **Linux Desktop** (`apps/linux`) | `main`, `development` | 🟡 Active Development |
| **Prior Releases** | `< v2.0.0` | 🔴 End of Life (Upgrade Required) |

---

## 🚨 Reporting a Vulnerability

If you discover a security vulnerability, privacy leak, or sandbox bypass in Azokle Browser, please report it privately. **Do not create public GitHub issues for sensitive security vulnerabilities.**

### Submission Channels

- **Email**: Send your findings directly to [security@azokle.com](mailto:security@azokle.com).
- **GitHub Security Advisories**: You can submit a private advisory draft via GitHub at [https://github.com/azoklesoftware/azokle-browser/security/advisories](https://github.com/azoklesoftware/azokle-browser/security/advisories).

### What to Include in Your Report

To help us triage and verify the issue efficiently, please include:
1. **Description & Impact**: A concise description of the vulnerability, potential threat vectors, and estimated severity (CVSS if applicable).
2. **Component Affected**: Specify the crate (`crates/privacy`, `crates/crypto`, etc.), engine component (`GeckoPromptDelegateImpl`, `AppLinkHandler`), or platform (`Android`, `Linux`).
3. **Step-by-Step Reproduction**: Detailed reproduction steps, sample HTML/JavaScript payloads, or test cases demonstrating the vulnerability.
4. **Environment**: Azokle Browser version, Android OS version / Linux distro, device architecture (`arm64-v8a`, `x86_64`), and network mode (Clearnet, DoH, or Tor).

---

## ⏱️ Response & Triage SLA

Our security engineering team adheres to the following response timeline:

- **Initial Acknowledgment**: Within **24 to 48 hours**.
- **Triage & Reproduction**: Within **3 business days**.
- **Fix & Patch Validation**: Targeted within **7 to 14 days** depending on vulnerability complexity.
- **Coordinated Public Disclosure**: Within **90 days** or upon general release of the patched version, whichever is sooner.

---

## 🏛️ Security Invariants & Core Guarantees

Azokle Browser enforces strict architectural security invariants:

1. **Zero-Telemetry Guarantee**:
   - The browser contains absolute zero analytics SDKs, device telemetry pings, crash telemetry trackers, or behavioral profiling.
   - Any unauthenticated outbound connection not explicitly triggered by user navigation or configured DNS/Tor circuits is treated as a critical security defect.

2. **Memory Safety by Construction**:
   - Parsers for untrusted network input (AdBlock filter syntax, HTTP headers, CNAME records, URL query strings, and TLS certificates) are written in Rust with `#![forbid(unsafe_code)]`.
   - Cross-language FFI boundaries (`JNI`, `C-ABI`, `UniFFI`) implement strict exception barriers (`catch_unwind`) preventing panics across language boundaries.

3. **Storage & Credential Encryption**:
   - Passwords and sensitive browser state are protected with authenticated AES-256-GCM backed by Android's hardware-backed Keystore (`EncryptedSharedPreferences`).
   - WebAuthn / FIDO2 Passkeys utilize hardware-isolated ECDSA P-256 signatures.

4. **Tor & Private Tab Isolation**:
   - Tor circuits route all traffic through local SOCKS5 proxy endpoints with isolated cookie jars, zero DNS leakage, and complete suppression of login/card autofill prompts.

---

## 🚫 Out of Scope

The following scenarios are considered out of scope unless they chain into a critical exploit:
- Attacks requiring physical access to an unlocked, rooted device.
- Self-XSS using developer tools or manual JavaScript execution in the omnibar.
- Theoretical rate-limiting issues on external upstream DoH/Tor directory servers.
- Denial of Service caused by exhausting local device storage through intentional, massive manual downloads.

---

## 🤝 Responsible Disclosure & Attribution

Security researchers who responsibly disclose verified vulnerabilities will be credited in our release notes and changelog (`.azokle/changelog/`). We greatly appreciate the security and privacy community's dedication to keeping Azokle safe.
