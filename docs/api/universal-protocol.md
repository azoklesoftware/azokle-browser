# Universal IPC Protocol Specification

Crate: `crates/protocol`
Supported Wire Encodings: Concise Binary Object Representation (CBOR) and JSON.

---

## 1. Envelope Structure (`ProtocolEnvelope`)

All inter-process messages exchanged between UI presentation layers and the core kernel use a standardized envelope schema:

```json
{
  "version": { "major": 1, "minor": 0 },
  "message_id": "9f7b1968-3bb8-4f86-9a2c-29b6bc498bc2",
  "correlation_id": "e44d32aa-9f44-4e20-b0b3-1f1981a3d902",
  "timestamp": 1726131400,
  "source": "WebExtension",
  "target": "CoreKernel",
  "payload": {
    "Command": {
      "TabOpen": {
        "url": "https://search.azokle.com",
        "title": "Azokle Search",
        "is_private": false,
        "is_tor": false
      }
    }
  }
}
```

---

## 2. Command Payloads (`CommandPayload`)

Commands represent state-modifying requests issued to the browser core:
- `TabOpen`: Open a new tab session.
- `TabClose`: Destroy a tab session.
- `TabNavigate`: Request navigation to a target URI.
- `ShieldToggle`: Toggle protection for an origin.
- `TorCircuitRenew`: Force allocation of fresh Tor onion circuits.
- `StoragePurge`: Request full data purge (cache, cookies, history).

---

## 3. Response Payloads (`ResponsePayload`)

Responses map back to original command requests via `correlation_id`:
- `TabSessionJson(String)`: Serialized JSON snapshot of the active tab.
- `ShieldStatsJson(String)`: Telemetry snapshot of blocked requests.
- `StatusOk`: Command execution succeeded without return value.
- `Error`: Failure payload containing error codes and human-readable diagnostic messages.
