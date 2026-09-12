use base64ct::{Base64UrlUnpadded, Encoding};
use p256::ecdsa::{signature::Signer, SigningKey, VerifyingKey};
use p256::elliptic_curve::rand_core::OsRng;
use serde::{Deserialize, Serialize};
use sha2::{Digest, Sha256};
use thiserror::Error;

#[derive(Error, Debug)]
pub enum PasskeyError {
    #[error("Failed to generate P-256 keypair: {0}")]
    KeyGenerationError(String),
    #[error("Signing failed: {0}")]
    SigningError(String),
    #[error("Serialization error: {0}")]
    SerializationError(String),
}

/// In-browser sovereign passkey credential compliant with WebAuthn / FIDO2 standards.
///
/// Encapsulates an elliptic-curve NIST P-256 (secp256r1) private key stored securely within
/// the client vault. The private key material is marked with `#[serde(skip_serializing)]`
/// and automatically wiped upon drop via [`Zeroize`].
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SovereignPasskey {
    /// Base64URL-encoded unique credential identifier.
    pub credential_id_b64: String,
    /// Relying Party Identifier (effective domain / origin).
    pub rp_id: String,
    /// Opaque user handle identifying the account at the relying party.
    pub user_handle_b64: String,
    /// Display username associated with the credential.
    pub user_name: String,
    /// Unix timestamp (seconds) when the passkey was minted.
    pub created_at: u64,
    /// Monotonically increasing signature counter used for replay detection.
    pub sign_count: u32,
    #[serde(skip_serializing)]
    private_key_bytes: Vec<u8>,
    /// Base64URL-encoded uncompressed SEC1 public key point.
    pub public_key_der_b64: String,
}

impl SovereignPasskey {
    /// Generates a new NIST P-256 keypair and registers a sovereign passkey credential.
    ///
    /// Derives a deterministic 20-byte credential ID prefixed with `azokle-passkey-v1:`,
    /// formats the public key as an uncompressed SEC1 point, and initializes the replay counter.
    ///
    /// # Returns
    /// A tuple containing the created [`SovereignPasskey`] and the raw binary credential ID.
    pub fn create(
        rp_id: &str,
        user_name: &str,
        user_handle: &[u8],
    ) -> Result<(Self, Vec<u8>), PasskeyError> {
        let signing_key = SigningKey::random(&mut OsRng);
        let verifying_key = VerifyingKey::from(&signing_key);

        let private_key_bytes = signing_key.to_bytes().to_vec();
        let pubkey_sec1 = verifying_key.to_encoded_point(false);
        let pubkey_bytes = pubkey_sec1.as_bytes();
        let public_key_der_b64 = Base64UrlUnpadded::encode_string(pubkey_bytes);

        // Generate credential ID (20-byte random or hash of pubkey)
        let mut hasher = Sha256::new();
        hasher.update(b"azokle-passkey-v1:");
        hasher.update(pubkey_bytes);
        let credential_id = &hasher.finalize()[..20];
        let credential_id_b64 = Base64UrlUnpadded::encode_string(credential_id);

        let user_handle_b64 = Base64UrlUnpadded::encode_string(user_handle);

        let passkey = Self {
            credential_id_b64,
            rp_id: rp_id.to_lowercase(),
            user_handle_b64,
            user_name: user_name.to_string(),
            created_at: std::time::SystemTime::now()
                .duration_since(std::time::UNIX_EPOCH)
                .map(|d| d.as_secs())
                .unwrap_or(0),
            sign_count: 0,
            private_key_bytes,
            public_key_der_b64,
        };

        Ok((passkey, credential_id.to_vec()))
    }

    /// Signs an authentication challenge using the credential's private key.
    ///
    /// Increments `sign_count` monotonically to detect replay attacks.
    /// Concatentates `authenticator_data` and `client_data_hash` according to WebAuthn Level 3 specs
    /// and returns an ASN.1 DER-encoded ECDSA signature.
    pub fn sign_assertion(
        &mut self,
        authenticator_data: &[u8],
        client_data_hash: &[u8],
    ) -> Result<Vec<u8>, PasskeyError> {
        self.sign_count += 1;

        let signing_key = SigningKey::from_slice(&self.private_key_bytes)
            .map_err(|e| PasskeyError::SigningError(e.to_string()))?;

        // WebAuthn signature data = authenticatorData || clientDataHash
        let mut signed_data = Vec::with_capacity(authenticator_data.len() + client_data_hash.len());
        signed_data.extend_from_slice(authenticator_data);
        signed_data.extend_from_slice(client_data_hash);

        let signature: p256::ecdsa::Signature = signing_key.sign(&signed_data);
        Ok(signature.to_der().as_bytes().to_vec())
    }
}

impl Drop for SovereignPasskey {
    fn drop(&mut self) {
        use zeroize::Zeroize;
        self.private_key_bytes.zeroize();
    }
}
