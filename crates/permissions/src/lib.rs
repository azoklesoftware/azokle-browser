use parking_lot::RwLock;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::sync::Arc;

#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum PermissionType {
    Camera,
    Microphone,
    Geolocation,
    Notifications,
    Autoplay,
    PersistentStorage,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionState {
    Prompt,
    Allow,
    Block,
}

#[derive(Clone, Default)]
pub struct PermissionsManager {
    rules: Arc<RwLock<HashMap<(String, PermissionType), PermissionState>>>,
}

impl PermissionsManager {
    pub fn new() -> Self {
        Self {
            rules: Arc::new(RwLock::new(HashMap::new())),
        }
    }

    pub fn set_permission(&self, origin: &str, perm: PermissionType, state: PermissionState) {
        self.rules.write().insert((origin.to_lowercase(), perm), state);
    }

    pub fn get_permission(&self, origin: &str, perm: PermissionType) -> PermissionState {
        self.rules
            .read()
            .get(&(origin.to_lowercase(), perm))
            .copied()
            .unwrap_or(PermissionState::Prompt)
    }

    pub fn clear_for_origin(&self, origin: &str) {
        let origin_clean = origin.to_lowercase();
        self.rules.write().retain(|(o, _), _| o != &origin_clean);
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_permissions() {
        let manager = PermissionsManager::new();
        assert_eq!(manager.get_permission("https://example.com", PermissionType::Camera), PermissionState::Prompt);
        manager.set_permission("https://example.com", PermissionType::Camera, PermissionState::Allow);
        assert_eq!(manager.get_permission("https://example.com", PermissionType::Camera), PermissionState::Allow);
        manager.clear_for_origin("https://example.com");
        assert_eq!(manager.get_permission("https://example.com", PermissionType::Camera), PermissionState::Prompt);
    }
}
