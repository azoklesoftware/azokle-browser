# Tor Onion Routing Subsystem

Azokle Browser includes native onion routing capabilities implemented in `crates/tor`. It enables 3-hop circuit anonymity, zero-DNS-leak SOCKS5 proxying, and direct resolution of `.onion` v3 hidden services.

---

## Architectural Workflow

```mermaid
sequenceDiagram
    participant App as Browser Session / Gecko Necko
    participant Tor as TorController (crates/tor)
    participant Guard as Guard Node (Hop 1)
    participant Middle as Middle Node (Hop 2)
    participant Exit as Exit Node / Onion Service (Hop 3)
    participant Web as Destination Web Server

    App->>Tor: SOCKS5 Connect (Target: example.onion or https://site.com)
    Tor->>Guard: Encrypted Onion Layer 1
    Guard->>Middle: Encrypted Onion Layer 2
    Middle->>Exit: Encrypted Onion Layer 3
    alt Clearnet Site
        Exit->>Web: TLS 1.3 Request
        Web-->>Exit: Response
    else .onion Hidden Service
        Exit-->>Tor: Rendezvous Point End-to-End Encryption
    end
    Exit-->>Middle-->>Guard-->>Tor-->>App: Anonymized Response Payload
```

---

## 1. Leakproof SOCKS5 Integration
- **Local Listener**: Spawns a hardened in-process or local SOCKS5 proxy server (default port `127.0.0.1:9050`).
- **Remote Hostname Resolution**: Enforces the SOCKS5 `0x03` (domain name) address type. Gecko delegates all DNS resolution over the encrypted Tor circuit rather than attempting local host lookups, eliminating DNS leakage.
- **Circuit Isolation**: Sessions running in Tor mode use isolated circuit tags, ensuring that different browser tabs do not share exit circuits, defeating cross-site linkability.

---

## 2. `.onion` v3 Domain Handling
When a user navigates to an onion address (e.g. `http://duckduckgogg42xjoc72x3sjasowoarfbgcmvfimaftt6twagswzczad.onion`):
1. `crates/networking` recognizes the `.onion` TLD.
2. The request is routed strictly through the Tor controller.
3. TLS certificate validation checks are adjusted to verify the onion v3 public key rather than relying on centralized Certificate Authorities (CAs).
