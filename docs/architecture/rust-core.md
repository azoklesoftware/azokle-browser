# Rust Core Subsystem Architecture

The Azokle Rust Core (`crates/`) is structured as a Cargo virtual workspace containing 18 focused, decoupled micro-crates. It serves as the single source of truth for browser domain rules, state machines, privacy heuristics, and cryptography.

---

## Workspace Layout & Dependency Matrix

| Crate | Responsibility | Key Structs / Traits | Concurrency Model |
| :--- | :--- | :--- | :--- |
| `crates/core` | Application orchestrator, TabManager, ShieldFacade | `AzokleCoreEngine`, `TabManager`, `ShieldStatsSummary` | `Arc<RwLock<T>>`, Atomic counters |
| `crates/privacy/adblock` | High-performance network request filter matcher | `AdBlockEngine`, `RuleMatcher`, `FilterDecision` | Read-heavy, lock-free lookups |
| `crates/privacy/tracker-block` | Tracker database, bounce unwrap, query parameter stripper | `TrackerEngine`, `QueryStripper`, `BounceUnwrapper` | Immutable trie structures |
| `crates/security` | CSP enforcement, memory zeroization, threat assessment | `SecurityEngine`, `ThreatDetector`, `CspEvaluator` | Zero-allocation sanitization |
| `crates/tor` | Arti-backed Tor client, SOCKS5 listener, `.onion` resolution | `TorController`, `TorCircuitManager`, `OnionResolver` | Async Tokio event loop |
| `crates/crypto` | AES-256-GCM, ChaCha20, Argon2id, FIDO2 Passkeys | `PasskeyManager`, `CryptoVault`, `KeyDerivation` | `zeroize::ZeroizeOnDrop` |
| `crates/storage` | Encrypted SQLite storage for history, bookmarks, settings | `StorageVault`, `ProfileManager`, `EncryptedDb` | Thread-safe connection pools |
| `crates/networking` | DoH resolver, SSRF firewall, connection pools | `NetworkingEngine`, `DohClient`, `SsrfFilter` | Asynchronous Hyper / Reqwest |
| `crates/protocol` | Universal cross-platform IPC envelopes (CBOR & JSON) | `ProtocolEnvelope`, `CommandPayload`, `EventPayload` | Zero-copy deserialization |
| `crates/services/*` | Modular services: tabs, history, bookmarks, downloads, search | `HistoryManager`, `BookmarkManager`, `DownloadManager` | Event-driven notification dispatch |
| `crates/common` | Common types, error definitions, telemetry blocking | `AzokleError`, `Result<T>`, `TelemetryBlocker` | Pure functional primitives |

---

## Domain-Driven Design in `crates/core`

`crates/core` implements an aggregate root orchestrator pattern via `AzokleCoreEngine`:

```rust
pub struct AzokleCoreEngine {
    pub adblock: Arc<AdBlockEngine>,
    pub trackers: Arc<TrackerEngine>,
    pub shield: Arc<ShieldEngine>,
    pub tor: Arc<TorController>,
    pub permissions: Arc<PermissionsManager>,
    pub history: Arc<HistoryManager>,
    pub bookmarks: Arc<BookmarkManager>,
    pub downloads: Arc<DownloadManager>,
    pub settings: Arc<SettingsManager>,
    pub networking: Arc<NetworkingEngine>,
    pub tabs: Arc<TabManager>,
    pub disabled_hosts: Arc<RwLock<HashSet<String>>>,
    pub stats_total: AtomicU64,
    pub stats_ads: AtomicU64,
    pub stats_trackers: AtomicU64,
    pub stats_telemetry: AtomicU64,
    pub stats_annoyances: AtomicU64,
    pub stats_fingerprinting: AtomicU64,
    pub stats_cryptominers: AtomicU64,
}
```

### Key Invariants
1. **Zero Dynamic Memory Leaks**: Core handles are shared using atomic reference counting (`Arc<T>`). Internal mutability is guarded by `parking_lot::RwLock`, preventing POSIX thread deadlocks.
2. **Stateless Request Interception**: Request checks execute in microseconds. Rules are pre-compiled into fast bitmask indices and hash sets.
3. **Relaxed Atomic Snapshots**: Shield metrics (`stats_*`) use `std::sync::atomic::Ordering::Relaxed` reads for UI counters to guarantee that reporting threads never stall network pipeline worker threads.
