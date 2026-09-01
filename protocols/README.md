# Azokle Universal Browser Protocol (AUBP) Specification

**Version**: 1.0.0  
**Status**: Active / Production Standard  
**Maintainer**: Azokle Core Architecture Group  

---

## 1. Overview

The `protocols/` directory contains the authoritative, language-agnostic interface definitions, schemas, and IPC specifications for all Azokle Browser applications across Android, iOS, macOS, Windows, Linux, and WebEngine embeddings.

```
protocols/
├── README.md               # Master Protocol Specification & Architecture
├── schemas/                # Universal Schema Definitions (JSON Schema / IDL)
│   ├── aubp-v1.json        # Master AUBP v1.0.0 Envelope Schema
│   ├── tabs.json           # Tab Lifecycle, Session Restore, & Grouping Schemas
│   ├── permissions.json    # W3C Origin Permissions & Sandboxing Schemas
│   ├── tor.json            # Tor Circuit, SOCKS5, & Exit IP Schemas
│   ├── security.json       # SSRF, DNS Leak Shield, & Redirect Security Schemas
│   └── search.json         # Search API & Omnibar Autosuggestion Schemas
├── ipc/                    # Inter-Process Communication & Messaging Contracts
│   └── ipc_specification.md# Framing, Buffering, Zero-Copy IPC, & Correlation
└── api/                    # Cross-Platform FFI / UniFFI & Extension Contracts
    └── api_specification.md# UniFFI Interface, C-ABI Bindings, & JNI Mappings
```

---

## 2. Protocol Wire Envelope (AUBP v1.0.0)

Every message transmitted over IPC, FFI, or network boundaries follows this canonical schema:

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "ProtocolEnvelope",
  "type": "object",
  "required": [
    "version",
    "message_id",
    "timestamp_ms",
    "source",
    "target",
    "payload"
  ],
  "properties": {
    "version": {
      "type": "object",
      "required": ["major", "minor", "patch"],
      "properties": {
        "major": { "type": "integer", "minimum": 1 },
        "minor": { "type": "integer", "minimum": 0 },
        "patch": { "type": "integer", "minimum": 0 }
      }
    },
    "message_id": { "type": "string" },
    "correlation_id": { "type": ["string", "null"] },
    "timestamp_ms": { "type": "integer" },
    "source": {
      "type": "string",
      "enum": ["RustCore", "GeckoEngine", "AndroidUI", "IosUI", "DesktopUI", "ExtensionBridge"]
    },
    "target": {
      "type": "string",
      "enum": ["RustCore", "GeckoEngine", "AndroidUI", "IosUI", "DesktopUI", "ExtensionBridge"]
    },
    "payload": {
      "type": "object",
      "oneOf": [
        { "required": ["Command"] },
        { "required": ["Request"] },
        { "required": ["Response"] },
        { "required": ["Event"] },
        { "required": ["Error"] }
      ]
    }
  }
}
```

---

## 3. Numeric Error Code Directory

| Code | Name | Description |
|---|---|---|
| `1001` | `IncompatibleVersion` | Client and Server protocol versions have incompatible major numbers |
| `1002` | `InvalidEnvelope` | Missing required headers or corrupted payload framing |
| `1003` | `Timeout` | Operation exceeded deadline |
| `1004` | `Cancelled` | Transaction aborted by caller |
| `1005` | `UnsupportedCommand` | Feature not supported in this runtime |
| `2001` | `TabNotFound` | Tab ID does not exist or was already closed |
| `2002` | `MaxTabsExceeded` | Tab limit reached |
| `3001` | `DnsResolutionFailed` | Hostname lookup failed across all configured DoH providers |
| `3002` | `DnsLeakPrevented` | Direct OS DNS lookup of `.onion` or privacy domain was blocked |
| `3003` | `ProxyUnreachable` | SOCKS5 / Tor tunnel failed to accept connection |
| `4001` | `SsrfBlocked` | Destination IP resolves to private CIDR (`127.0.0.0/8`, `10.0.0.0/8`, etc.) |
| `4002` | `InsecureRedirectBlocked` | Refused insecure downgrade redirect (HTTPS $\rightarrow$ HTTP) |
| `5001` | `VaultLocked` | ChaCha20-Poly1305 master key not provided or hardware Keystore locked |
