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

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SovereignPasskey {
    pub credential_id_b64: String,
    pub rp_id: String,
    pub user_handle_b64: String,
    pub user_name: String,
    pub created_at: u64,
    pub sign_count: u32,
    #[serde(skip_serializing)]
    private_key_bytes: Vec<u8>,
    pub public_key_der_b64: String,
}

impl SovereignPasskey {
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
