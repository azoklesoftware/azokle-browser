# Deep Dive: Storage Encryption & Database Vault

This document explains the page-level encryption, key derivation, and memory zeroization mechanics used in `crates/storage` and `crates/crypto`.

---

## 1. Multi-Tier Key Hierarchy

```
[User Passphrase / Device Secret]
               |
        [Argon2id KDF]  (Memory: 64 MB, Iterations: 3, Parallelism: 4)
               |
               v
     +-------------------+
     | Master Key (256b) |
     +-------------------+
               |
     +---------+---------+
     |                   |
     v                   v
[History KEK]      [Vault KEK] (Key Encryption Keys via HKDF-SHA256)
     |                   |
     v                   v
[History DB]       [Bookmarks / Logins DB] (Encrypted via AES-256-GCM)
```

---

## 2. Page-Level SQLite Encryption Engine

Rather than encrypting individual columns or entire database files at rest (which leaks page size access patterns or requires complete re-encryption on update), Azokle uses an in-process page-level VFS (Virtual File System):

### Page Structure
- Standard SQLite page size: **4,096 bytes**.
- Reserved page header: **16 bytes Authentication Tag (GCM)** + **12 bytes Nonce (IV)**.
- Ciphertext payload: **4,068 bytes**.

### Nonce Generation Invariant
```
Nonce = HMAC-SHA256(PageNumber || GlobalSalt, PageWriteCounter)[0..12]
```
- **Zero Nonce Reuse**: Because nonces are derived using a monotonically increasing 64-bit page write counter, nonce collisions under AES-GCM are cryptographically impossible.
- **Bit-Rot & Tamper Detection**: If an unauthorized party alters a single byte of a database file on disk, GCM authentication verification fails with `AzokleError::IntegrityCheckFailed`, immediately locking the vault.

---

## 3. Ephemeral RAM-Disk Isolation for Private Browsing

Private tabs run with an in-memory VFS:
1. SQLite is configured with `:memory:` or `PRAGMA temp_store = MEMORY`.
2. Temporary database pages are stored in heap-allocated chunks.
3. Upon tab closure, the memory pages are explicitly scrubbed:
   ```rust
   unsafe {
       std::ptr::write_bytes(buffer.as_mut_ptr(), 0, buffer.len());
   }
   ```
4. This guarantees that forensics tools inspecting flash storage or swap space recover zero residual web artifacts.
