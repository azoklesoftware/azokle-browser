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

pub mod crypto;
pub mod passkey;

pub use crypto::{SecretKey, VaultCrypto, VaultCryptoError};
pub use passkey::{clear_global_passkey_vault, PasskeyError, SovereignPasskey};

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_vault_encryption_decryption() {
        // Short salt (<16 bytes) must fail
        let short_salt = rand::random::<[u8; 8]>();
        assert!(VaultCrypto::derive_key("password123", &short_salt).is_err());

        // Valid salt (>=16 bytes) succeeds
        let salt = rand::random::<[u8; 24]>();
        let key = VaultCrypto::derive_key("SuperSecretMasterPassword123!", &salt).unwrap();
        let message = b"Azokle Sovereign Vault Sensitive Data 2026";

        let encrypted = VaultCrypto::encrypt(&key, message).unwrap();
        assert_ne!(&encrypted[..], message);

        let decrypted = VaultCrypto::decrypt(&key, &encrypted).unwrap();
        assert_eq!(&decrypted[..], message);
    }

    #[test]
    fn test_vault_encryption_with_aad() {
        let salt = rand::random::<[u8; 24]>();
        let key = VaultCrypto::derive_key("SuperSecretMasterPassword123!", &salt).unwrap();
        let message = b"Confidential Password Payload";
        let aad = b"account:user@example.com:domain:example.com";

        let encrypted = VaultCrypto::encrypt_with_aad(&key, message, aad).unwrap();
        let decrypted = VaultCrypto::decrypt_with_aad(&key, &encrypted, aad).unwrap();
        assert_eq!(&decrypted[..], message);

        // Mismatched AAD must fail closed (prevents record-swapping)
        let wrong_aad = b"account:attacker@evil.com:domain:example.com";
        assert!(VaultCrypto::decrypt_with_aad(&key, &encrypted, wrong_aad).is_err());
    }

    #[test]
    fn test_sovereign_passkey_creation_and_signing() {
        use sha2::{Digest, Sha256};
        let (mut passkey, cred_id) =
            SovereignPasskey::create("github.com", "azokledev", b"user_12345").unwrap();
        assert_eq!(passkey.rp_id, "github.com");
        assert_eq!(passkey.user_name, "azokledev");
        assert!(!passkey.public_key_der_b64.is_empty());
        assert_eq!(cred_id.len(), 20);

        let mut auth_data = Vec::new();
        auth_data.extend_from_slice(&Sha256::digest(b"github.com"));
        auth_data.push(0x05); // flags
        auth_data.extend_from_slice(&[0, 0, 0, 1]); // counter
        let client_hash = [0xabu8; 32];
        let sig = passkey.sign_assertion(&auth_data, &client_hash).unwrap();
        assert!(!sig.is_empty());
        assert_eq!(passkey.sign_count, 1);
    }

    #[test]
    fn test_passkey_vault_retention_and_sign_with_stored() {
        use sha2::{Digest, Sha256};
        let (passkey, _cred_id) =
            SovereignPasskey::create("gitlab.com", "devuser", b"user_999").unwrap();
        let cred_id_b64 = passkey.credential_id_b64.clone();
        passkey.store_in_vault();

        let mut auth_data = Vec::new();
        auth_data.extend_from_slice(&Sha256::digest(b"gitlab.com"));
        auth_data.push(0x05);
        auth_data.extend_from_slice(&[0, 0, 0, 1]);
        let client_hash = [0x55u8; 32];
        let sig =
            SovereignPasskey::sign_with_stored(&cred_id_b64, &auth_data, &client_hash).unwrap();
        assert!(!sig.is_empty());

        assert!(
            SovereignPasskey::sign_with_stored("non-existent-id", &auth_data, &client_hash).is_err()
        );
    }
}
