# System Architecture Overview

The Azokle Browser architecture is designed around four strict foundational principles:
1. **Core Domain Isolation**: All security logic, cryptography, ad/tracker blocking, network proxies, and protocol rules are strictly encapsulated within a pure Rust workspace (`crates/`), completely isolated from UI code.
2. **Engine Abstraction Layer**: The rendering pipeline is decoupled from platform frameworks via a modern C++20 engine contract (`azokle::engine`), enabling Mozilla Quantum (Gecko/GeckoView) embedding across Linux, Android, Windows, macOS, and iOS without leaking engine-specific quirks into the UI.
3. **Panic-Proof FFI Boundaries**: All foreign function interactions (C-ABI, JNI, UniFFI) enforce unconditional panic-isolation barriers (`std::panic::catch_unwind`), zero-cost bitmask status flags, and deterministic memory ownership.
4. **Thin Native Presentation**: Client applications in `apps/` maintain zero business or privacy logic; they act strictly as presentation shells rendering the browser surface and forwarding user interactions to the core.

---

## Component Topology

```mermaid
graph TD
    subgraph UI_Layer [Native Presentation Tier]
        AND[Android - Kotlin / Jetpack Compose]
        LNX[Linux - C++20 / Wayland / GTK]
        WIN[Windows - C++20 / Win32 / WinUI]
        MAC[macOS - Swift / SwiftUI / Cocoa]
        IOS[iOS - Swift / SwiftUI]
    end

    subgraph Controller_Tier [Desktop & Mobile Controllers]
        DAPP[DesktopBrowserApp - Shared C++20 Controller]
        AND_MGR[GeckoEngineManager - Android Orchestrator]
        IOS_VM[BrowserViewModel - iOS Swift Orchestrator]
    end

    subgraph Engine_Tier [C++20 Engine Abstraction Layer]
        CANONICAL[Canonical API: IBrowserEngine / IBrowserSession / IEngineDelegate]
        GECKO_DESKTOP[GeckoEngineBridge - Mozilla Quantum Desktop]
        GECKO_VIEW[GeckoView - Android Native Integration]
        MOCK_ENGINE[MockBrowserEngine - Headless Test Harness]
    end

    subgraph FFI_Tier [Foreign Function Interface Boundaries]
        CABI[bindings/c - C-ABI with catch_unwind]
        JNI[bindings/kotlin - JNI fast-path bitmask]
        UNIFFI[bindings/uniffi - Swift / Apple FFI]
    end

    subgraph Rust_Kernel [Azokle Core Rust Workspace]
        CORE[crates/core - AzokleCoreEngine & ShieldFacade]
        PRIVACY[crates/privacy - adblock / tracker-block]
        SECURITY[crates/security - CSP / URL sanitizer / memory zeroize]
        TOR[crates/tor - Arti SOCKS5 onion circuits]
        CRYPTO[crates/crypto - AES-256-GCM / WebAuthn Passkeys]
        STORAGE[crates/storage - Encrypted SQLite Vault]
        NETWORKING[crates/networking - DoH / Connection Pool]
    end

    LNX & WIN --> DAPP
    MAC --> DAPP
    DAPP --> CANONICAL
    AND --> AND_MGR --> GECKO_VIEW
    IOS --> IOS_VM

    CANONICAL --> GECKO_DESKTOP & MOCK_ENGINE
    DAPP --> CABI
    AND_MGR --> JNI
    IOS_VM --> UNIFFI

    CABI & JNI & UNIFFI --> CORE
    CORE --> PRIVACY & SECURITY & TOR & CRYPTO & STORAGE & NETWORKING
```

---

## Dataflow Pipeline

### 1. URL Navigation & Interception Flow
```mermaid
sequenceDiagram
    participant User as User / Address Bar
    participant Shell as Platform Native Shell
    participant Controller as DesktopBrowserApp / Engine Manager
    participant FFI as FFI Layer (C-ABI / JNI)
    participant Core as Rust Core (AzokleShield)
    participant Engine as Mozilla Quantum (Gecko / GeckoView)

    User->>Shell: Enter "https://example.com"
    Shell->>Controller: Navigate(url)
    Controller->>FFI: check_url(url, origin, is_third_party)
    FFI->>Core: AdBlockEngine + TrackerEngine match
    Core-->>FFI: FilterDecision (Block / Allow / Redirect)
    alt Request Blocked
        FFI-->>Controller: FilterDecision { should_block: true }
        Controller->>Shell: DispatchBlockedEvent(stats)
        Controller-->>Engine: Abort Navigation
    else Request Allowed
        FFI-->>Controller: FilterDecision { should_block: false }
        Controller->>Engine: session->LoadUri(url)
        Engine->>Shell: Paint WebRender Frame
    end
```

### 2. Ephemeral vs Normal Storage Segregation
The browser maintains two distinct operational profiles:
- **Normal Profile**: Persistent cookies, cache, local storage, encrypted history, and bookmark databases stored in AES-256-GCM encrypted vaults on disk.
- **Private / Ephemeral Profile**: Strictly memory-backed (RAM disk), zero write activity to persistent storage. Upon tab or window closure, all cryptographic keys, cache blocks, and session histories are zeroized using `zeroize::Zeroize`.
