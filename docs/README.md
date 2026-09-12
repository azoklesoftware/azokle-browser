# Azokle Sovereign Browser: Engineering Documentation

Welcome to the official technical documentation for the **Azokle Sovereign Browser** project. Azokle is an ultra-private, high-performance, multi-platform web browser engineered with a memory-safe Rust core, a unified C++20 Mozilla Quantum (Gecko / GeckoView / WebRender) rendering layer, universal FFI bindings, and native presentation applications across Linux, Android, Windows, macOS, and iOS.

---

## 📚 Table of Contents

### 1. Architecture & Design
- [System Overview](architecture/system-overview.md): High-level system topology, layering principles, component interactions, and dataflow diagrams.
- [Rust Core Subsystem](architecture/rust-core.md): Domain-Driven Design across the 18 Rust workspace crates, domain services, state machines, and concurrency.
- [Mozilla Quantum Engine Bridge](architecture/engine-quantum.md): Gecko / GeckoView embedding, C++20 engine interfaces, WebRender compositing, and session lifecycles.
- [Universal FFI Bindings](architecture/ffi-bridges.md): C-ABI, Kotlin JNI, and Swift UniFFI bridge implementations with panic boundaries.
- [Multi-Platform Native Shells](architecture/multiplatform-shells.md): Presentation architectures for Android (Compose), Linux (Wayland/X11), Windows (Win32), macOS (Cocoa), and iOS (SwiftUI).
- [Shared Desktop Controller](architecture/desktop-controller.md): The `DesktopBrowserApp` orchestration layer ensuring consistent state across desktop platforms.

### 2. Security & Privacy Engineering
- [Threat Model & Security Invariants](security-privacy/threat-model.md): Threat vectors, security boundaries, attacker profiles, and memory protection.
- [Azokle Shield Architecture](security-privacy/azokle-shield.md): Adblocking, tracker defusal, cosmetic element hiding, scriptlet defusers, bounce tracking mitigation, and query parameter stripping.
- [Tor Onion Routing Subsystem](security-privacy/tor-onion-routing.md): 3-hop circuit isolation, Arti SOCKS5 integration, zero-DNS-leak enforcement, and `.onion` resolution.
- [Cryptographic Vault & Passkeys](security-privacy/cryptography-vault.md): AES-256-GCM, ChaCha20-Poly1305, Argon2id key derivation, encrypted SQLite vault, and FIDO2 WebAuthn passkeys.
- [Network Security & DNS-over-HTTPS](security-privacy/network-defense.md): DoH resolvers, SSRF defense, scheme downgrade prevention, and connection isolation.

### 3. API & Protocol Specifications
- [C++20 Engine API Reference](api/cpp-engine-api.md): Specification for `IBrowserEngine`, `IBrowserSession`, `IEngineDelegate`, and `IWebExtensionBridge`.
- [C-ABI Export Reference](api/c-abi-reference.md): Native C interfaces (`azokle_core.h`), memory ownership rules, and panic isolation contracts.
- [Swift UniFFI API Reference](api/swift-uniffi-api.md): Swift API contracts for Apple platforms (`UniversalBrowserEngine`, `UniversalFilterDecision`, `UniversalShieldStats`).
- [Universal IPC Protocol](api/universal-protocol.md): Cross-layer wire protocol, CBOR/JSON envelopes, message contracts, and correlation handling.

### 4. Technical Deep Dives
- [Mozilla Quantum Internals](deep-dive/gecko-quantum-internals.md): Necko channels, WebRender GPU pipeline, SpiderMonkey JIT realms, and XPCOM messaging.
- [Shield Filter Compilation & Engine](deep-dive/shield-filter-engine.md): N-gram tokenization, two-tier Bloom filters, CNAME uncloaking, and cosmetic selector compilation.
- [Tor Onion Routing Mechanics](deep-dive/tor-onion-routing-mechanics.md): Circuit building, ntor key exchange, 512-byte relay cell multiplexing, and v3 hidden service rendezvous.
- [Sovereign Passkeys & WebAuthn Flow](deep-dive/passkey-webauthn-flow.md): FIDO2/CTAP2 protocols, ECDSA P-256 signatures, CBOR attestation structures, and hardware biometric binding.
- [Storage Encryption Vault & VFS](deep-dive/storage-encryption-vault.md): Page-level SQLite encryption, Argon2id derivation, IV generation without reuse, and ephemeral RAM scrubbing.
- [Multi-Platform Presentation Internals](deep-dive/multiplatform-internals.md): Windowing models, DirectComposition, Wayland subsurfaces, Metal layers, and low-memory tab suspension.

### 5. Development & Operations
- [Getting Started & Prerequisites](development/getting-started.md): Development environment setup, toolchain requirements, and dependencies.
- [Comprehensive Build Guide](development/build-guide.md): Compiling Cargo crates, CMake desktop engines, Android APKs, and Apple Swift packages.
- [Testing & Quality Assurance](development/testing-strategy.md): Unit testing, headless verification (`MockBrowserEngine`), fuzzing, and regression suites.
- [Coding Standards & Guidelines](development/coding-standards.md): C++20 guidelines, Rust safety practices, Kotlin/Compose idioms, and security linting.
- [Filter List Pipeline](operations/filter-pipeline.md): Updating and compiling adblock rulesets, surrogate scriptlets, and query filters.
- [Release Management & CI/CD](operations/release-and-ci.md): Multi-platform packaging matrix, signing configurations, and CI workflows.

---

## 🏛️ High-Level Architectural Snapshot

```
+-----------------------------------------------------------------------------------+
|                            NATIVE CLIENT APPLICATIONS                             |
|  +---------------+  +---------------+  +---------------+  +---------------------+  |
|  | Android App   |  | Linux Desktop |  | macOS Desktop |  | Windows Desktop     |  |
|  | Jetpack Comp. |  | C++20/Wayland |  | Swift/Cocoa   |  | C++20/WinUI3        |  |
|  +---------------+  +---------------+  +---------------+  +---------------------+  |
|                                     |                                             |
|                               +------------------+                                |
|                               | iOS App (SwiftUI)|                                |
|                               +------------------+                                |
+-----------------------------------------------------------------------------------+
       |                       |                    |                   |
       v                       v                    v                   v
+------------------+   +------------------------------------------------------------+
| Kotlin/JNI Glue  |   |           CANONICAL C++20 ENGINE API (azokle::engine)      |
| AzokleShieldJni  |   |   IBrowserEngine | IBrowserSession | IEngineDelegate       |
+------------------+   +------------------------------------------------------------+
       |                         |                  |                   |
       |                         v                  v                   v
       |               +------------------+ +----------------+ +--------------------+
       |               | GeckoDesktop     | | GeckoView      | | MockEngineBackend  |
       |               | (Linux/Mac/Win)  | | (Android JNI)  | | (Automated Tests)  |
       |               +------------------+ +----------------+ +--------------------+
       |                         \                  /                   /
       v                          v                v                   v
+-----------------------------------------------------------------------------------+
|                            UNIVERSAL FFI BRIDGE LAYER                             |
|         bindings/c (Desktop C-ABI)      |     bindings/uniffi (Swift/iOS)         |
+-----------------------------------------------------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|                        AZOKLE SOVEREIGN CORE (RUST WORKSPACE)                     |
|  +-----------------------------------------------------------------------------+  |
|  |                      crates/core (AzokleEngine / ShieldFacade)              |  |
|  +-----------------------------------------------------------------------------+  |
|       |              |               |               |              |             |
|       v              v               v               v              v             |
|  +----------+  +------------+  +------------+  +-----------+  +---------------+   |
|  | privacy/ |  | security/  |  | tor/       |  | storage/  |  | networking/   |   |
|  | AdBlock  |  | MemoryZero |  | Arti SOCKS |  | Encrypted |  | DoH / DNS     |   |
|  | Tracker  |  | Sanitizer  |  | Isolation  |  | SQLite    |  | ConnectionPool|   |
|  +----------+  +------------+  +------------+  +-----------+  +---------------+   |
+-----------------------------------------------------------------------------------+
```
