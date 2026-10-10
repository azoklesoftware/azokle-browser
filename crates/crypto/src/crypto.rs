use argon2::{password_hash::rand_core::OsRng, Algorithm, Argon2, Params, Version};
use chacha20poly1305::{
    aead::{Aead, KeyInit},
    ChaCha20Poly1305, Nonce,
};
use rand::RngCore;
use thiserror::Error;
use zeroize::{Zeroize, ZeroizeOnDrop};

/// Errors occurring during cryptographic operations in the Azokle Vault.
#[derive(Error, Debug)]
pub enum VaultCryptoError {
    #[error("Encryption failed")]
    EncryptionError,
    #[error("Decryption failed / invalid authentication tag")]
    DecryptionError,
    #[error("Key derivation failed: {0}")]
    KdfError(String),
}

/// Secure container for 256-bit symmetric encryption keys.
///
/// Implements [`Zeroize`] and [`ZeroizeOnDrop`] to guarantee memory is overwritten
/// with zeros when dropped, mitigating key leakage via memory dumps or cold-boot attacks.
#[derive(Zeroize, ZeroizeOnDrop)]
pub struct SecretKey([u8; 32]);

impl SecretKey {
    /// Wraps a raw 32-byte key array.
    pub fn new(key: [u8; 32]) -> Self {
        Self(key)
    }

    /// Accesses the underlying 32-byte key slice.
    pub fn as_bytes(&self) -> &[u8; 32] {
        &self.0
    }
}

/// Core cryptographic operations provider for the secure storage vault.
pub struct VaultCrypto;

impl VaultCrypto {
    /// Derives a 256-bit [`SecretKey`] from a user passphrase and cryptographic salt.
    ///
    /// # Cryptographic Parameters:
    /// - Algorithm: Argon2id (v0x13), combining data-dependent and data-independent memory passes.
    /// - Memory Cost: 64 MiB (65,536 KiB) to defeat GPU/ASIC parallel brute-force crackers.
    /// - Iterations: 3 time passes.
    /// - Parallelism: 4 threads.
    /// - Salt: Enforces a strict minimum of 16 cryptographically random bytes.
    pub fn derive_key(passphrase: &str, salt: &[u8]) -> Result<SecretKey, VaultCryptoError> {
        if salt.len() < 16 {
            return Err(VaultCryptoError::KdfError(
                "Salt must be at least 16 bytes for sovereign security.".to_string(),
            ));
        }

        // Pinned Parameters: 64 MiB memory cost, 3 time iterations, 4 parallelism threads
        let params = Params::new(65536, 3, 4, Some(32))
            .map_err(|e| VaultCryptoError::KdfError(e.to_string()))?;
        let argon2 = Argon2::new(Algorithm::Argon2id, Version::V0x13, params);

        let mut key = [0u8; 32];
        argon2
            .hash_password_into(passphrase.as_bytes(), salt, &mut key)
            .map_err(|e| VaultCryptoError::KdfError(e.to_string()))?;
        Ok(SecretKey::new(key))
    }

    /// Encrypts plaintext bytes using ChaCha20-Poly1305 authenticated encryption (AEAD).
    ///
    /// Generates a fresh 96-bit (12-byte) CSPRNG nonce for every invocation.
    ///
    /// # Wire Format:
    /// `[12-byte Nonce] || [Ciphertext] || [16-byte Poly1305 MAC Tag]`
    pub fn encrypt(key: &SecretKey, plaintext: &[u8]) -> Result<Vec<u8>, VaultCryptoError> {
        let cipher = ChaCha20Poly1305::new(key.as_bytes().into());
        let mut nonce_bytes = [0u8; 12];
        OsRng.fill_bytes(&mut nonce_bytes);
        let nonce = Nonce::from_slice(&nonce_bytes);

        let mut ciphertext = cipher
            .encrypt(nonce, plaintext)
            .map_err(|_| VaultCryptoError::EncryptionError)?;

        // Output format: [12 bytes Nonce] + [Ciphertext + 16 bytes Poly1305 Tag]
        let mut result = Vec::with_capacity(12 + ciphertext.len());
        result.extend_from_slice(&nonce_bytes);
        result.append(&mut ciphertext);
        Ok(result)
    }

    /// Decrypts a ChaCha20-Poly1305 payload and verifies the Poly1305 authentication tag.
    ///
    /// Ensures ciphertext integrity before yielding plaintext. Rejects truncated payloads
    /// lacking the 12-byte nonce or 16-byte Poly1305 authentication tag.
    pub fn decrypt(key: &SecretKey, payload: &[u8]) -> Result<Vec<u8>, VaultCryptoError> {
        if payload.len() < 12 + 16 {
            return Err(VaultCryptoError::DecryptionError);
        }

        let (nonce_bytes, ciphertext) = payload.split_at(12);
        let cipher = ChaCha20Poly1305::new(key.as_bytes().into());
        let nonce = Nonce::from_slice(nonce_bytes);

        cipher
            .decrypt(nonce, ciphertext)
            .map_err(|_| VaultCryptoError::DecryptionError)
    }
}
