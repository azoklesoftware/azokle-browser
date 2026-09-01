use parking_lot::RwLock;
use std::collections::HashMap;
use std::sync::Arc;
use thiserror::Error;

#[derive(Error, Debug)]
pub enum StorageError {
    #[error("Key not found")]
    NotFound,
    #[error("Serialization error: {0}")]
    SerializationError(String),
}

#[derive(Clone, Default)]
pub struct EncryptedStorage {
    data: Arc<RwLock<HashMap<String, Vec<u8>>>>,
}

impl EncryptedStorage {
    pub fn new() -> Self {
        Self {
            data: Arc::new(RwLock::new(HashMap::new())),
        }
    }

    pub fn set(&self, key: &str, value: &[u8]) {
        self.data.write().insert(key.to_string(), value.to_vec());
    }

    pub fn get(&self, key: &str) -> Option<Vec<u8>> {
        self.data.read().get(key).cloned()
    }

    pub fn remove(&self, key: &str) -> bool {
        self.data.write().remove(key).is_some()
    }

    pub fn clear(&self) {
        self.data.write().clear();
    }

    pub fn contains(&self, key: &str) -> bool {
        self.data.read().contains_key(key)
    }

    pub fn keys(&self) -> Vec<String> {
        self.data.read().keys().cloned().collect()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_storage_crud() {
        let storage = EncryptedStorage::new();
        storage.set("user_pref", b"dark_mode=true");
        assert_eq!(storage.get("user_pref").unwrap(), b"dark_mode=true");
        assert!(storage.contains("user_pref"));
        assert!(storage.remove("user_pref"));
        assert!(!storage.contains("user_pref"));
    }
}
