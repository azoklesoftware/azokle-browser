// Copyright (c) 2026 The Azokle Authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

use base64ct::{Base64UrlUnpadded, Encoding};
use p256::ecdsa::{signature::Signer, SigningKey, VerifyingKey};
use p256::elliptic_curve::Generate;
use serde::{Deserialize, Serialize};
use sha2::{Digest, Sha256};
use thiserror::Error;

use parking_lot::RwLock;
use std::collections::HashMap;
use std::sync::OnceLock;

static PASSKEY_VAULT: OnceLock<RwLock<HashMap<String, SovereignPasskey>>> = OnceLock::new();

fn get_passkey_vault() -> &'static RwLock<HashMap<String, SovereignPasskey>> {
    PASSKEY_VAULT.get_or_init(|| RwLock::new(HashMap::new()))
}

#[derive(Error, Debug)]
pub enum PasskeyError {
    #[error("Failed to generate P-256 keypair: {0}")]
    KeyGenerationError(String),
    #[error("Signing failed: {0}")]
    SigningError(String),
    #[error("Serialization error: {0}")]
    SerializationError(String),
    #[error("Passkey not found in vault: {0}")]
    NotFound(String),
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
        let signing_key = SigningKey::generate();
        let verifying_key = VerifyingKey::from(&signing_key);

        let private_key_bytes = signing_key.to_bytes().to_vec();
        let pubkey_sec1 = verifying_key.to_sec1_point(false);
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
        // AZK-VULN-15: Enforce WebAuthn Level 3 authenticatorData validation
        // Structure: rpIdHash (32 bytes) || flags (1 byte) || signCount (4 bytes)
        if authenticator_data.len() < 37 {
            return Err(PasskeyError::SigningError(
                "Authenticator data too short (must be at least 37 bytes)".to_string(),
            ));
        }

        // Verify that the rpIdHash matches sha256(self.rp_id) to prevent cross-origin assertion forging
        let expected_rp_id_hash = Sha256::digest(self.rp_id.as_bytes());
        if authenticator_data[..32] != expected_rp_id_hash[..] {
            return Err(PasskeyError::SigningError(
                "RP ID hash mismatch: authenticator data does not match passkey relying party".to_string(),
            ));
        }

        // AZK-VULN-30: Enforce strict WebAuthn Level 3 clientDataHash length (SHA-256 digest = 32 bytes)
        if client_data_hash.len() != 32 {
            return Err(PasskeyError::SigningError(
                "Client data hash must be exactly 32 bytes (SHA-256 digest)".to_string(),
            ));
        }

        self.sign_count += 1;

        let signing_key = SigningKey::from_slice(&self.private_key_bytes)
            .map_err(|e| PasskeyError::SigningError(e.to_string()))?;

        // WebAuthn signature data = authenticatorData || clientDataHash
        let mut signed_data = Vec::with_capacity(authenticator_data.len() + client_data_hash.len());
        signed_data.extend_from_slice(authenticator_data);
        signed_data.extend_from_slice(client_data_hash);

        let signature: p256::ecdsa::Signature = signing_key.sign(&signed_data);
        use zeroize::Zeroize;
        signed_data.zeroize();
        Ok(signature.to_der().as_bytes().to_vec())
    }

    /// Stores this passkey in the secure in-memory vault keyed by its credential ID.
    pub fn store_in_vault(self) -> String {
        let id = self.credential_id_b64.clone();
        get_passkey_vault().write().insert(id.clone(), self);
        id
    }

    /// Retrieves a cloned passkey credential from the in-memory vault by credential ID.
    pub fn get_from_vault(credential_id_b64: &str) -> Option<Self> {
        get_passkey_vault().read().get(credential_id_b64).cloned()
    }

    /// Signs an authentication challenge using a passkey stored in the vault.
    pub fn sign_with_stored(
        credential_id_b64: &str,
        authenticator_data: &[u8],
        client_data_hash: &[u8],
    ) -> Result<Vec<u8>, PasskeyError> {
        let mut guard = get_passkey_vault().write();
        let passkey = guard
            .get_mut(credential_id_b64)
            .ok_or_else(|| PasskeyError::NotFound(credential_id_b64.to_string()))?;
        passkey.sign_assertion(authenticator_data, client_data_hash)
    }

    /// Purges all stored passkeys from memory (e.g. during incineration).
    pub fn clear_vault() {
        get_passkey_vault().write().clear();
    }
}

/// Purges all stored passkeys from the global in-memory vault.
pub fn clear_global_passkey_vault() {
    SovereignPasskey::clear_vault();
}


impl Drop for SovereignPasskey {
    fn drop(&mut self) {
        use zeroize::Zeroize;
        self.private_key_bytes.zeroize();
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_passkey_creation_and_assertion_verification() {
        let (mut passkey, _cred_id) = SovereignPasskey::create("example.com", "alice", b"user_123")
            .expect("Passkey creation failed");

        assert_eq!(passkey.rp_id, "example.com");
        assert_eq!(passkey.user_name, "alice");

        // Construct valid authenticator_data (37 bytes: 32 bytes rpIdHash + 1 byte flags + 4 bytes counter)
        let rp_id_hash = Sha256::digest(b"example.com");
        let mut valid_auth_data = Vec::new();
        valid_auth_data.extend_from_slice(&rp_id_hash);
        valid_auth_data.push(0x05); // UP + UV flags
        valid_auth_data.extend_from_slice(&[0, 0, 0, 1]); // signCount = 1

        let client_hash = [0x42u8; 32];

        // 1. Valid signing must succeed
        let sig = passkey
            .sign_assertion(&valid_auth_data, &client_hash)
            .expect("Assertion signing failed");
        assert!(!sig.is_empty());
        assert_eq!(passkey.sign_count, 1);

        // 2. Reject if authenticator_data is too short (< 37 bytes)
        let short_auth_data = vec![0u8; 20];
        let err_short = passkey.sign_assertion(&short_auth_data, &client_hash);
        assert!(err_short.is_err());

        // 3. Reject if rpIdHash does not match (cross-origin attack attempt)
        let evil_rp_hash = Sha256::digest(b"attacker.com");
        let mut forged_auth_data = Vec::new();
        forged_auth_data.extend_from_slice(&evil_rp_hash);
        forged_auth_data.push(0x05);
        forged_auth_data.extend_from_slice(&[0, 0, 0, 2]);

        let err_mismatch = passkey.sign_assertion(&forged_auth_data, &client_hash);
        assert!(err_mismatch.is_err());

        // 4. AZK-VULN-30: Reject if client_data_hash is not exactly 32 bytes
        let invalid_client_hash = vec![0x42u8; 16];
        let err_bad_hash = passkey.sign_assertion(&valid_auth_data, &invalid_client_hash);
        assert!(err_bad_hash.is_err());
    }

    #[test]
    fn test_passkey_vault_clear() {
        let (passkey, _) = SovereignPasskey::create("vault.com", "bob", b"user_456")
            .expect("Passkey creation failed");
        let cred_id = passkey.store_in_vault();

        assert!(SovereignPasskey::get_from_vault(&cred_id).is_some());
        SovereignPasskey::clear_vault();
        assert!(SovereignPasskey::get_from_vault(&cred_id).is_none());
    }
}
