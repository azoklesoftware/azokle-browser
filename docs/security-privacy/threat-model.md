# Threat Model & Security Invariants

The security model of Azokle Browser adheres to STRIDE threat categorization, memory safety invariants, and strict boundary isolation.

---

## 1. Security Boundaries & Trust Zones

```
+---------------------------------------------------------------------------------+
|                                 UNTRUSTED ZONE                                  |
|  - Malicious Web Content (JavaScript, HTML, WebAssembly, CSS)                   |
|  - Hostile Network Ad & Tracker Scripts                                         |
|  - Third-Party Tracking Pixels & CNAME Spoofs                                   |
+---------------------------------------------------------------------------------+
                                      |
                       [Necko Socket / Interceptor]
                                      v
+---------------------------------------------------------------------------------+
|                             DEFENSE / FILTER ZONE                               |
|  - Azokle Shield (Fast Bitmask Matcher & Trie Index)                            |
|  - Query Parameter Stripper & Bounce Tracker Unwrapper                          |
|  - Content Security Policy (CSP) Evaluator & Scriptlet Defusers                 |
+---------------------------------------------------------------------------------+
                                      |
                     [Panic-Safe FFI Barrier: catch_unwind]
                                      v
+---------------------------------------------------------------------------------+
|                                SECURE CORE ZONE                                 |
|  - Rust Kernel (crates/core, crates/crypto, crates/storage)                     |
|  - Encrypted SQLite Vault (AES-256-GCM / Argon2id)                              |
|  - Tor Controller (Arti Onion SOCKS5 Isolation)                                 |
|  - FIDO2 / WebAuthn Private Key Storage                                         |
+---------------------------------------------------------------------------------+
```

---

## 2. STRIDE Threat Analysis & Mitigations

| Threat | Description | Azokle Mitigation |
| :--- | :--- | :--- |
| **Spoofing** | Adversary attempts to impersonate legitimate domains or CNAME cloaking. | CNAME uncloaking via recursive DNS resolution (`crates/privacy/tracker-block`), strict TLS verification. |
| **Tampering** | In-flight request interception or local profile database tampering. | AES-256-GCM authenticated encryption on local databases; strict TLS 1.3 cipher suite enforcement. |
| **Repudiation** | Malicious websites denying unauthorized resource requests or credential prompts. | WebAuthn passkey assertion signing with hardware-backed user verification (`BiometricPrompt` / Keyring). |
| **Information Disclosure** | Cross-site tracking via cookies, bounce tracking, or URL query parameters. | Total Cookie Protection (First-Party state partitioning per eTLD+1), automated bounce unwrapping, 80+ query stripping rules. |
| **Denial of Service** | Deep regex recursion (ReDoS) or memory exhaustion in filter matching. | Pre-tokenized trie and bitmask matchers without backtrack-vulnerable regexes; strict buffer size limits. |
| **Elevation of Privilege** | Renderer sandbox breakout or malicious intent injection on Android. | Android Intent sanitization (`IntentParser`), scheme allowlists (`http`, `https`, `azokle`), process sandboxing. |

---

## 3. Core Security Invariants

1. **Zero Unwind Panic Invariant**: No Rust thread panic shall ever propagate across FFI into C++, Kotlin, or Swift host runtimes.
2. **Zero Plaintext Private Key Invariant**: Cryptographic keys, passkey credentials, and master vault keys must be zeroized in memory immediately upon dropping (`zeroize::ZeroizeOnDrop`).
3. **Private Mode Disk Exclusion Invariant**: Private browsing sessions must never execute `fsync` or write dirty database pages to non-volatile storage under any circumstances.
4. **No-DNS-Leak Invariant**: Under Tor routing, no UDP port 53 or DNS query may touch the local network adapter; all queries must resolve through Tor SOCKS5 remote proxy commands (`0x04` / `0x05`).
