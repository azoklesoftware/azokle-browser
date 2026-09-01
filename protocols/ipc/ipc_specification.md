# Azokle Inter-Process Communication (IPC) Protocol

**Specification**: AUBP-IPC-1.0  
**Transport**: Cross-Platform Local Socket / Pipe / Shared Memory & JNI/C-ABI Frame  

---

## 1. Frame Wire Format

All IPC byte streams are framed with a 4-byte length prefix (Little-Endian) followed by the binary CBOR or UTF-8 JSON payload:

```
+-------------------+---------------------------------------------------+
| Length (4 Bytes)  | ProtocolEnvelope (CBOR Binary or UTF-8 JSON Payload) |
+-------------------+---------------------------------------------------+
```

- **Maximum Frame Size**: 16 MB (16,777,216 bytes).
- **Heartbeat Interval**: 30 seconds (`HeartbeatEvent`).
- **Cancellation**: If a caller drops a correlation ID, the receiver terminates async futures.
