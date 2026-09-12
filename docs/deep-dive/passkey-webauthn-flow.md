# Deep Dive: Sovereign Passkey & WebAuthn Implementation

This document details the cryptographic flow, CBOR encoding, and hardware binding of the **Sovereign Passkey** subsystem in `crates/crypto/src/passkey.rs`.

---

## 1. FIDO2 / CTAP2 Architecture

Azokle acts as an in-process **FIDO2 Authenticator** compliant with the W3C WebAuthentication Level 3 specification:

```
[Relying Party (Website)] <--- HTTPS ---> [Gecko Engine]
                                               |
                                     (WebAuthn JS API)
                                               v
                                    [Azokle Native Passkey]
                                               |
                              +----------------+----------------+
                              |                                 |
                     [Software Vault]                 [Hardware Keystore]
                     (Argon2id / AES)                 (Secure Enclave / TPM)
```

---

## 2. Passkey Creation (`navigator.credentials.create`)

```mermaid
sequenceDiagram
    participant RP as Relying Party (Website)
    participant Core as Azokle Passkey Vault
    participant HW as Hardware Keystore / Biometrics

    RP->>Core: PublicKeyCredentialCreationOptions { challenge, rp: { id: "github.com" }, user }
    Core->>HW: Prompt Biometric Authorization (Fingerprint / Face Unlock)
    HW-->>Core: Biometric Verified OK
    Core->>Core: Generate secp256r1 Key Pair (sk, pk)
    Core->>Core: Store sk in Encrypted Vault bound to "github.com"
    Core->>Core: Build Attestation Object (CBOR encoded):
    Note over Core: authData: rpIdHash (32B) || flags (1B) || signCount (4B) || aaguid (16B) || pk
    Core-->>RP: AttestationResponse { id: credentialId, rawId, response: { clientDataJSON, attestationObject } }
```

### CBOR Attestation Structure
The attestation object is serialized using binary CBOR with the following dictionary keys:
- `fmt`: `"none"` (sovereign privacy format; prevents device serial tracking).
- `authData`: Concatenation of:
  - `rpIdHash` (32 bytes): SHA-256 hash of the relying party domain.
  - `flags` (1 byte): User Present (UP=1), User Verified (UV=1), Attested Credential Data (AT=1).
  - `signCount` (4 bytes, big-endian): Monotonically increasing signature counter.
  - `attestedCredentialData`: AAGUID (16 bytes) + Credential ID Length (2 bytes) + Credential ID + COSE-encoded Public Key.

---

## 3. Passkey Assertion (`navigator.credentials.get`)

```mermaid
sequenceDiagram
    participant RP as Relying Party (Website)
    participant Core as Azokle Passkey Vault
    participant HW as Hardware Keystore / Biometrics

    RP->>Core: PublicKeyCredentialRequestOptions { challenge, rpId: "github.com", allowCredentials }
    Core->>Core: Lookup sk for "github.com" in Encrypted Vault
    Core->>HW: Prompt User Verification
    HW-->>Core: User Verified
    Core->>Core: Compute SHA-256 hash of clientDataJSON
    Core->>Core: Sign (authData || clientDataHash) using ECDSA P-256 private key
    Core-->>RP: AssertionResponse { id, authenticatorData, signature (DER format), userHandle }
    RP->>RP: Verify ECDSA Signature with stored public key -> Authenticated!
```
