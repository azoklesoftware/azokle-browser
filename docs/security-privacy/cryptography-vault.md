# Cryptographic Vault & Passkeys Architecture

Azokle Browser secures all user secrets, encrypted databases, and authentication credentials using state-of-the-art cryptographic primitives in `crates/crypto` and `crates/storage`.

---

## 1. Cryptographic Primitive Suite

| Function | Algorithm | Specification / Library | Security Target |
| :--- | :--- | :--- | :--- |
| **Authenticated Encryption** | AES-256-GCM / ChaCha20-Poly1305 | `aes-gcm`, `chacha20poly1305` | 256-bit symmetric confidentiality & integrity |
| **Key Derivation Function (KDF)** | Argon2id | `argon2` | Memory-hard resistance to GPU/ASIC brute forcing |
| **FIDO2 / WebAuthn Signatures** | ECDSA (P-256) / Ed25519 | `p256`, `ed25519-dalek` | Passkey hardware authentication |
| **Secure Random Generation** | ChaCha20-based CSPRNG | `rand_core::OsRng` | Cryptographically secure nonces & IVs |
| **Memory Sanitization** | Memory Zeroization | `zeroize::ZeroizeOnDrop` | Defense against cold-boot & heap-dump exploits |

---

## 2. Encrypted SQLite Storage Vault (`crates/storage`)

User data (bookmarks, history records, saved logins, session snapshots) is encrypted before writing to disk:
1. **Master Key Derivation**: The user passphrase or hardware-backed master secret is derived via Argon2id (parameters: `m=64MB, t=3, p=4`).
2. **Page-Level / Field-Level Encryption**: Every sensitive record is encrypted with a unique 96-bit nonce using AES-256-GCM.
3. **Zeroization on Destruction**: When the database connection drops or a private browsing session ends, all session keys and intermediate plaintext buffers in RAM are overwritten with zeroes.

---

## 3. Sovereign Passkey & WebAuthn Subsystem (`crates/crypto/src/passkey.rs`)

Azokle implements a native sovereign passkey authenticator compliant with W3C WebAuthn Level 3:
- **`SovereignPasskey`**: Generates and manages secp256r1 (P-256) key pairs.
- **Relying Party Scoping**: Private keys are strictly scoped to the relying party origin (eTLD+1), eliminating phishing vectors.
- **Biometric Binding**: On Android, private key usage requires biometric confirmation via Android KeyStore and `BiometricPrompt`. On desktop, system keyring credentials protect the passkey store.
