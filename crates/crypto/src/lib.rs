pub mod crypto;
pub mod passkey;

pub use crypto::{SecretKey, VaultCrypto, VaultCryptoError};
pub use passkey::{PasskeyError, SovereignPasskey};

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_vault_encryption_decryption() {
        // Short salt (<16 bytes) must fail
        let short_salt = b"short_salt";
        assert!(VaultCrypto::derive_key("password123", short_salt).is_err());

        // Valid salt (>=16 bytes) succeeds
        let salt = b"azokle_test_salt_123";
        let key = VaultCrypto::derive_key("SuperSecretMasterPassword123!", salt).unwrap();
        let message = b"Azokle Sovereign Vault Sensitive Data 2026";

        let encrypted = VaultCrypto::encrypt(&key, message).unwrap();
        assert_ne!(&encrypted[..], message);

        let decrypted = VaultCrypto::decrypt(&key, &encrypted).unwrap();
        assert_eq!(&decrypted[..], message);
    }

    #[test]
    fn test_sovereign_passkey_creation_and_signing() {
        let (mut passkey, cred_id) =
            SovereignPasskey::create("github.com", "azokledev", b"user_12345").unwrap();
        assert_eq!(passkey.rp_id, "github.com");
        assert_eq!(passkey.user_name, "azokledev");
        assert!(!passkey.public_key_der_b64.is_empty());
        assert_eq!(cred_id.len(), 20);

        let auth_data = b"sample_authenticator_data_32_bytes";
        let client_hash = [0xabu8; 32];
        let sig = passkey.sign_assertion(auth_data, &client_hash).unwrap();
        assert!(!sig.is_empty());
        assert_eq!(passkey.sign_count, 1);
    }
}
