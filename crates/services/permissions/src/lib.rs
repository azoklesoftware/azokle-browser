use parking_lot::RwLock;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::sync::Arc;
use url::Url;

/// Web capabilities and hardware access permissions controllable per web origin.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum PermissionType {
    Camera,
    Microphone,
    Geolocation,
    Notifications,
    Autoplay,
    PersistentStorage,
    MediaKeySystem,
    Midi,
    Clipboard,
}

impl PermissionType {
    /// Parses a string representation into a [`PermissionType`].
    pub fn from_str_name(name: &str) -> Option<Self> {
        match name.to_uppercase().as_str() {
            "CAMERA" => Some(Self::Camera),
            "MICROPHONE" => Some(Self::Microphone),
            "GEOLOCATION" => Some(Self::Geolocation),
            "NOTIFICATIONS" => Some(Self::Notifications),
            "AUTOPLAY" => Some(Self::Autoplay),
            "PERSISTENT_STORAGE" | "PERSISTENTSTORAGE" => Some(Self::PersistentStorage),
            "MEDIA_KEY_SYSTEM" | "MEDIAKEYSYSTEM" => Some(Self::MediaKeySystem),
            "MIDI" => Some(Self::Midi),
            "CLIPBOARD" => Some(Self::Clipboard),
            _ => None,
        }
    }

    /// Canonical uppercase name representation.
    pub fn to_str_name(&self) -> &'static str {
        match self {
            Self::Camera => "CAMERA",
            Self::Microphone => "MICROPHONE",
            Self::Geolocation => "GEOLOCATION",
            Self::Notifications => "NOTIFICATIONS",
            Self::Autoplay => "AUTOPLAY",
            Self::PersistentStorage => "PERSISTENT_STORAGE",
            Self::MediaKeySystem => "MEDIA_KEY_SYSTEM",
            Self::Midi => "MIDI",
            Self::Clipboard => "CLIPBOARD",
        }
    }
}

/// Authorization state for a granted or denied origin capability.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum PermissionState {
    Prompt,
    Allow,
    Block,
}

impl PermissionState {
    /// Parses string representation into [`PermissionState`].
    pub fn from_str_name(name: &str) -> Self {
        match name.to_uppercase().as_str() {
            "ALLOW" | "GRANTED" => Self::Allow,
            "BLOCK" | "DENIED" => Self::Block,
            _ => Self::Prompt,
        }
    }

    /// Canonical uppercase string representation.
    pub fn to_str_name(&self) -> &'static str {
        match self {
            Self::Prompt => "PROMPT",
            Self::Allow => "ALLOW",
            Self::Block => "BLOCK",
        }
    }
}

/// Dual-partition permission manager for origin capability authorization.
///
/// # Partition Architecture:
/// - `persistent_rules`: Saved to persistent disk storage for standard browsing.
/// - `ephemeral_rules`: Isolated in-memory table for Private and Tor browsing tabs,
///   guaranteed to never touch non-volatile disk.
#[derive(Clone)]
pub struct PermissionsManager {
    persistent_rules: Arc<RwLock<HashMap<String, HashMap<PermissionType, PermissionState>>>>,
    ephemeral_rules: Arc<RwLock<HashMap<String, HashMap<PermissionType, PermissionState>>>>,
}

impl Default for PermissionsManager {
    fn default() -> Self {
        Self::new()
    }
}

impl PermissionsManager {
    /// Creates an empty permission manager with separate persistent and ephemeral rule tables.
    pub fn new() -> Self {
        Self {
            persistent_rules: Arc::new(RwLock::new(HashMap::new())),
            ephemeral_rules: Arc::new(RwLock::new(HashMap::new())),
        }
    }

    /// Canonicalizes a raw URL or domain into a strict W3C RFC 6454 Origin (`scheme://host[:port]`).
    ///
    /// Omits standard default ports (80 for HTTP, 443 for HTTPS) to guarantee uniform lookup keys.
    pub fn canonicalize_origin(raw: &str) -> String {
        let trimmed = raw.trim();
        if trimmed.is_empty() || trimmed.eq_ignore_ascii_case("about:blank") {
            return String::new();
        }

        let to_parse = if !trimmed.contains("://") {
            format!("https://{}", trimmed)
        } else {
            trimmed.to_string()
        };

        if let Ok(parsed) = Url::parse(&to_parse) {
            let scheme = parsed.scheme().to_lowercase();
            if let Some(host) = parsed.host_str() {
                let host_clean = host.trim_matches('/').to_lowercase();
                let port = parsed.port_or_known_default();
                if let Some(p) = port {
                    let is_default = (scheme == "http" && p == 80) || (scheme == "https" && p == 443);
                    if is_default {
                        format!("{}://{}", scheme, host_clean)
                    } else {
                        format!("{}://{}:{}", scheme, host_clean, p)
                    }
                } else {
                    format!("{}://{}", scheme, host_clean)
                }
            } else {
                trimmed.to_lowercase()
            }
        } else {
            trimmed.to_lowercase()
        }
    }

    pub fn set_permission(&self, origin: &str, perm: PermissionType, state: PermissionState, is_private: bool) {
        let canon = Self::canonicalize_origin(origin);
        if canon.is_empty() {
            return;
        }

        if is_private {
            let mut cache = self.ephemeral_rules.write();
            cache.entry(canon).or_default().insert(perm, state);
        } else {
            let mut cache = self.persistent_rules.write();
            cache.entry(canon).or_default().insert(perm, state);
        }
    }

    pub fn get_permission(&self, origin: &str, perm: PermissionType, is_private: bool) -> PermissionState {
        let canon = Self::canonicalize_origin(origin);
        if canon.is_empty() {
            return PermissionState::Prompt;
        }

        if is_private {
            if let Some(map) = self.ephemeral_rules.read().get(&canon) {
                if let Some(&state) = map.get(&perm) {
                    return state;
                }
            }
        }

        self.persistent_rules
            .read()
            .get(&canon)
            .and_then(|map| map.get(&perm).copied())
            .unwrap_or(PermissionState::Prompt)
    }

    pub fn clear_for_origin(&self, origin: &str) {
        let canon = Self::canonicalize_origin(origin);
        if !canon.is_empty() {
            self.persistent_rules.write().remove(&canon);
            self.ephemeral_rules.write().remove(&canon);
        }
    }

    pub fn clear_all(&self) {
        self.persistent_rules.write().clear();
        self.ephemeral_rules.write().clear();
    }

    pub fn clear_ephemeral(&self) {
        self.ephemeral_rules.write().clear();
    }

    pub fn get_all_customized_origins(&self) -> Vec<String> {
        let mut set = std::collections::BTreeSet::new();
        for key in self.persistent_rules.read().keys() {
            set.insert(key.clone());
        }
        for key in self.ephemeral_rules.read().keys() {
            set.insert(key.clone());
        }
        set.into_iter().collect()
    }

    pub fn get_site_permissions_json(&self, origin: &str, is_private: bool) -> String {
        let canon = Self::canonicalize_origin(origin);
        let mut result = serde_json::Map::new();

        let all_types = [
            PermissionType::Camera,
            PermissionType::Microphone,
            PermissionType::Geolocation,
            PermissionType::Notifications,
            PermissionType::Autoplay,
            PermissionType::PersistentStorage,
            PermissionType::MediaKeySystem,
            PermissionType::Midi,
            PermissionType::Clipboard,
        ];

        for &perm in &all_types {
            let state = self.get_permission(&canon, perm, is_private);
            result.insert(
                perm.to_str_name().to_string(),
                serde_json::Value::String(state.to_str_name().to_string()),
            );
        }

        serde_json::to_string(&result).unwrap_or_else(|_| "{}".to_string())
    }

    pub fn export_persistent_json(&self) -> String {
        let mut root = serde_json::Map::new();
        let guard = self.persistent_rules.read();
        for (origin, map) in guard.iter() {
            let mut site_map = serde_json::Map::new();
            for (type_key, state_val) in map {
                site_map.insert(
                    type_key.to_str_name().to_string(),
                    serde_json::Value::String(state_val.to_str_name().to_string()),
                );
            }
            root.insert(origin.clone(), serde_json::Value::Object(site_map));
        }
        serde_json::to_string(&root).unwrap_or_else(|_| "{}".to_string())
    }

    pub fn import_persistent_json(&self, json_str: &str) -> usize {
        if json_str.trim().is_empty() {
            return 0;
        }

        let parsed: serde_json::Value = match serde_json::from_str(json_str) {
            Ok(v) => v,
            Err(_) => return 0,
        };

        let root_obj = match parsed.as_object() {
            Some(obj) => obj,
            None => return 0,
        };

        let mut count = 0;
        let mut guard = self.persistent_rules.write();

        for (origin_key, site_val) in root_obj {
            let canon = Self::canonicalize_origin(origin_key);
            if canon.is_empty() {
                continue;
            }
            if let Some(site_obj) = site_val.as_object() {
                let entry = guard.entry(canon).or_default();
                for (type_name, state_val) in site_obj {
                    if let Some(perm_type) = PermissionType::from_str_name(type_name) {
                        if let Some(state_str) = state_val.as_str() {
                            let perm_state = PermissionState::from_str_name(state_str);
                            entry.insert(perm_type, perm_state);
                            count += 1;
                        }
                    }
                }
            }
        }

        count
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_permissions_canonicalization() {
        assert_eq!(
            PermissionsManager::canonicalize_origin("example.com"),
            "https://example.com"
        );
        assert_eq!(
            PermissionsManager::canonicalize_origin("http://example.com:80"),
            "http://example.com"
        );
        assert_eq!(
            PermissionsManager::canonicalize_origin("https://example.com:8443"),
            "https://example.com:8443"
        );
    }

    #[test]
    fn test_permissions_dual_partition() {
        let manager = PermissionsManager::new();

        // 1. Persistent partition
        manager.set_permission("https://example.com", PermissionType::Camera, PermissionState::Allow, false);
        assert_eq!(
            manager.get_permission("https://example.com", PermissionType::Camera, false),
            PermissionState::Allow
        );

        // 2. Ephemeral Private partition
        manager.set_permission("https://private.com", PermissionType::Microphone, PermissionState::Block, true);
        assert_eq!(
            manager.get_permission("https://private.com", PermissionType::Microphone, true),
            PermissionState::Block
        );
        assert_eq!(
            manager.get_permission("https://private.com", PermissionType::Microphone, false),
            PermissionState::Prompt
        );

        // 3. Clear Ephemeral
        manager.clear_ephemeral();
        assert_eq!(
            manager.get_permission("https://private.com", PermissionType::Microphone, true),
            PermissionState::Prompt
        );
        // Persistent remains
        assert_eq!(
            manager.get_permission("https://example.com", PermissionType::Camera, false),
            PermissionState::Allow
        );
    }

    #[test]
    fn test_export_import_json() {
        let manager = PermissionsManager::new();
        manager.set_permission("https://test.com", PermissionType::Geolocation, PermissionState::Allow, false);

        let json = manager.export_persistent_json();
        assert!(json.contains("GEOLOCATION"));

        let new_manager = PermissionsManager::new();
        let imported = new_manager.import_persistent_json(&json);
        assert_eq!(imported, 1);
        assert_eq!(
            new_manager.get_permission("https://test.com", PermissionType::Geolocation, false),
            PermissionState::Allow
        );
    }
}
