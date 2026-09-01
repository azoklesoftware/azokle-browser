<div align="center">

# 🛡️ Azokle Sovereign Browser

**The Next-Generation Sovereign, Private, and High-Performance Cross-Platform Web Browser.**

[![Rust 1.75+](https://img.shields.io/badge/Rust-1.75%2B-orange.svg?style=flat&logo=rust)](https://www.rust-lang.org)
[![C++20](https://img.shields.io/badge/C%2B%2B-20-blue.svg?style=flat&logo=c%2B%2B)](https://isocpp.org)
[![GTK 3/4](https://img.shields.io/badge/GTK-3%2F4-green.svg?style=flat&logo=gnome)](https://www.gtk.org)
[![Android GeckoView](https://img.shields.io/badge/Android-GeckoView-red.svg?style=flat&logo=android)](https://developer.android.com)
[![License: MIT/Apache-2.0](https://img.shields.io/badge/License-MIT%2FApache--2.0-blue.svg)](LICENSE)

*Built with a shared Rust Core engine, Gecko / WebKit rendering runtime, FIDO2 WebAuthn Passkeys, 3-Hop Tor Onion Routing, and zero third-party telemetry.*

</div>

---

## 📐 System Architecture

```mermaid
graph TD
    subgraph Monorepo Root [azokle-browser Monorepo]
        subgraph Rust Core [14 Sovereign Rust Crates - /crates]
            AD[adblock]
            TB[tracker-block]
            CR[crypto - P-256 Passkeys]
            SEC[security]
            TOR[tor - SOCKS5 / 3-Hop]
            NET[networking]
            STORE[storage / bookmarks / history]
            SYNC[sync]
        end

        subgraph Bindings [/bindings]
            CABI[C-ABI Bridge - libazokle_c.a]
            JNI[Kotlin / JNI Bindings]
        end

        subgraph Engine [/engine]
            GE[IGeckoEngine / IGeckoSession / IGeckoDelegate]
        end

        subgraph Applications [/apps]
            LINUX[Linux Desktop App - GTK / C++20]
            ANDROID[Android App - Material 3 / Kotlin]
        end
    end

    Rust Core --> CABI & JNI
    CABI --> GE
    GE --> LINUX
    JNI --> ANDROID
```

---

## 🌟 Key Features

- **🛡️ Azokle Privacy Shield**: Zero-latency native ad-blocking, tracker defusal, scriptlet injection, and cosmetic filter matching built directly in Rust.
- **🧅 3-Hop Tor Onion Routing**: Built-in SOCKS5 proxy and Tor circuit management for seamless private browsing and `.onion` hidden service resolution.
- **🔑 Sovereign Passkey Vault**: Hardware-backed FIDO2 / WebAuthn cryptographic authentication using ECDSA P-256 and Argon2id.
- **🎨 Brave-Level New Tab Dashboard**: Minimalist geometric dashboard with live privacy counters (`Trackers Blocked`, `Bandwidth Saved`, `Time Saved`), squircle speed dial, and search bangs (`!g`, `!gh`, `!yt`, `!w`).
- **⚙️ Dedicated Internal Pages**:
  - `azokle://newtab`: High-performance local dashboard.
  - `azokle://settings`: Comprehensive Shield, Search, Passkey, and Tor settings.
  - `azokle://history`: Chronological timeline with instant query filtering.
  - `azokle://downloads`: File download manager.
  - `azokle://bookmarks`: Sovereign bookmark manager.
- **🐧 Native Linux Desktop Chrome**: Ultra-clean GTK interface with integrated tab strip, compact circular glyph controls, and solid flat dark palette.

---

## 📁 Repository Structure

```text
azokle-browser/
├── Cargo.toml                     # Cargo workspace definition
├── CMakeLists.txt                 # Monorepo CMake configuration
├── README.md                      # Project documentation
├── apps/
│   ├── android/                   # Native Android application (Material 3 / Kotlin)
│   └── linux/                     # Native Linux desktop application (GTK / C++20)
│       ├── resources/
│       │   ├── newtab/            # Brave-style new tab dashboard (HTML/CSS/JS)
│       │   └── pages/             # Settings, History, Downloads, Bookmarks
│       └── src/                   # Window, Tab Manager, UI, and Widget implementations
├── bindings/
│   ├── c/                         # C-ABI headers and libazokle_c static library
│   └── kotlin/                    # JNI native bindings for Android
├── crates/                        # 14 Modular Rust crates
│   ├── adblock/                   # AdBlock filter matching engine
│   ├── tracker-block/             # Privacy tracker defusal
│   ├── crypto/                    # ECDSA P-256 & ChaCha20-Poly1305 vault
│   ├── security/                  # TLS & URL sanitization
│   ├── storage/                   # Encrypted SQLite storage
│   ├── bookmarks/                 # Bookmark management
│   ├── history/                   # History management
│   ├── tor/                       # Tor SOCKS5 & onion routing
│   ├── permissions/               # Site permission controller
│   ├── search/                    # Private search integration
│   ├── downloads/                 # Download pipeline
│   ├── sync/                      # End-to-end encrypted sync
│   ├── networking/                # HTTP client & DOM parser
│   └── azokle-core/               # Unified core facade
└── engine/                        # Cross-platform abstract C++20 GeckoEngine bridge
```

---

## 🚀 Building & Running

### Prerequisites
- **Rust Toolchain**: `rustc` 1.75+ & `cargo`
- **C++ Compiler**: `g++` or `clang++` supporting C++20
- **Build Systems**: `cmake` (>= 3.20) & `make`
- **Linux Libraries**: `libgtk-3-dev` / `libgtk-4-dev`, `libwebkit2gtk-4.1-dev` / `libwebkitgtk-6.0-dev`
- **Android SDK / NDK**: (For Android builds) NDK 28+

### 1. Build Rust Monorepo Core
```bash
# Run tests across all 14 crates
cargo test --workspace

# Build the C-ABI static bridge
cargo build --package azokle-c-bridge --release
```

### 2. Build & Launch Linux Desktop App
```bash
# Configure and build with CMake
cmake -B build_linux -S .
cmake --build build_linux

# Launch Linux Browser on active display
DISPLAY=:0 ./build_linux/apps/linux/azokle-browser-linux
```

### 3. Build Android App
```bash
cd apps/android
./gradlew assembleDebug
```

---

## 🔒 Security & Privacy Architecture

1. **Memory Safety**: Core filtering, cryptographic key management, and networking routines are written exclusively in 100% memory-safe Rust with `#![forbid(unsafe_code)]` where applicable.
2. **Zero Telemetry**: Azokle does not collect, log, or transmit user activity, IP addresses, browsing histories, or hardware fingerprints.
3. **Tracking Stripper**: URL parameters matching known advertising tracking schemes (`utm_*`, `fbclid`, `gclid`, `_ga`, `msclkid`, etc.) are stripped natively before network requests are dispatched.

---

## 📄 License

This project is licensed under the MIT License and Apache 2.0 License. See [LICENSE](LICENSE) for details.
