# Deep Dive: Tor Onion Routing Mechanics

This document explains the cryptographic mechanics, circuit building protocol, and stream multiplexing implemented in `crates/tor`.

---

## 1. Circuit Construction & Onion Layers

Azokle builds 3-hop circuits consisting of:
1. **Entry / Guard Node**: Selected from a persistent consensus directory; guards against Sybil attacks.
2. **Middle Relay**: Provides intermediate routing; hides the guard from the exit node.
3. **Exit Relay**: Connects to destination clearnet servers (or rendezvous point for `.onion` services).

```
[Azokle Client] --- (K1, K2, K3) ---> [Guard] --- (K2, K3) ---> [Middle] --- (K3) ---> [Exit] ---> [Target]
```

### Key Exchange: ntor Handshake
Circuit extension uses the **ntor handshake protocol** (Curve25519, Ed25519, and SHA-256):
1. **`CREATE2` Cell**: Client sends ephemeral Curve25519 public key $X = g^x$ encrypted to the relay's public identity.
2. **`CREATED2` Cell**: Relay replies with ephemeral key $Y = g^y$ and authentication hash.
3. **Shared Secret Derivation**: Both compute $K = H(Y^x \parallel X^y)$, deriving forward/backward symmetric AES-CTR or ChaCha20-Poly1305 stream keys.

---

## 2. Relay Cells & Cell Multiplexing

All data transmitted across a Tor circuit is packetized into fixed-size **512-byte cells**:

```
+-----------+--------+----------+----------+-------------+---------+
| CircID    | Command| StreamID | Digest   | Length      | Payload |
| (4 bytes) | (1 B)  | (2 B)    | (4 bytes)| (2 bytes)   | (499 B) |
+-----------+--------+----------+----------+-------------+---------+
```

### Multi-Stream Multiplexing
- A single 3-hop circuit can multiplex hundreds of independent browser requests simultaneously.
- Each HTTP transaction is allocated an isolated `StreamID`.
- If one TCP stream experiences packet loss, other streams sharing the circuit continue transmitting without head-of-line blocking at the application level.

---

## 3. Hidden Service v3 (`.onion`) Resolution

To connect to a version 3 onion service:
1. **Address Parsing**: The 56-character Base32 address encodes a 32-byte Ed25519 public key and a 2-byte checksum.
2. **Descriptor Lookup**:
   - Client queries the distributed hash table (HSDir) for the onion service's signed descriptor.
   - Descriptor contains the service's current introduction points.
3. **Rendezvous Exchange**:
   - Azokle establishes a circuit to an arbitrary relay chosen as the **Rendezvous Point** (RP) and generates a one-time cookie.
   - Client sends an `INTRODUCE1` cell to one of the service's introduction points containing the RP identity and cookie.
   - The hidden service connects to the RP via a separate circuit, completing an end-to-end 6-hop encrypted tunnel.
