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

use parking_lot::RwLock;
use serde::{Deserialize, Serialize};
use std::collections::{HashMap, VecDeque};
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

/// Timestamped audit record capturing an origin permission authorization event.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PermissionAuditRecord {
    pub id: String,
    pub origin: String,
    pub permission: PermissionType,
    pub state: PermissionState,
    pub timestamp: u64,
    pub is_private: bool,
}

/// Dual-partition permission manager for origin capability authorization & security auditing.
///
/// # Partition Architecture:
/// - `persistent_rules`: Saved to persistent disk storage for standard browsing.
/// - `ephemeral_rules`: Isolated in-memory table for Private and Tor browsing tabs.
/// - `audit_history`: Real-time audit trail of all permission evaluations and access requests.
#[derive(Clone)]
pub struct PermissionsManager {
    persistent_rules: Arc<RwLock<HashMap<String, HashMap<PermissionType, PermissionState>>>>,
    ephemeral_rules: Arc<RwLock<HashMap<String, HashMap<PermissionType, PermissionState>>>>,
    audit_history: Arc<RwLock<VecDeque<PermissionAuditRecord>>>,
    max_audit_records: usize,
}

impl Default for PermissionsManager {
    /// Returns the default instance.
    fn default() -> Self {
        Self::new()
    }
}

impl PermissionsManager {
    /// Creates an empty permission manager with separate persistent and ephemeral rule tables and audit logging.
    pub fn new() -> Self {
        Self {
            persistent_rules: Arc::new(RwLock::new(HashMap::new())),
            ephemeral_rules: Arc::new(RwLock::new(HashMap::new())),
            audit_history: Arc::new(RwLock::new(VecDeque::new())),
            max_audit_records: 5000,
        }
    }

    /// Canonicalizes a raw URL or domain into a strict W3C RFC 6454 Origin (`scheme://host[:port]`).
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
                    let is_default =
                        (scheme == "http" && p == 80) || (scheme == "https" && p == 443);
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

    /// Updates the permission.
    pub fn set_permission(
        &self,
        origin: &str,
        perm: PermissionType,
        state: PermissionState,
        is_private: bool,
    ) {
        let canon = Self::canonicalize_origin(origin);
        if canon.is_empty() {
            return;
        }

        if is_private {
            let mut cache = self.ephemeral_rules.write();
            cache.entry(canon.clone()).or_default().insert(perm, state);
        } else {
            let mut cache = self.persistent_rules.write();
            cache.entry(canon.clone()).or_default().insert(perm, state);
        }

        self.record_audit(&canon, perm, state, is_private);
    }

    /// Returns the permission.
    pub fn get_permission(
        &self,
        origin: &str,
        perm: PermissionType,
        is_private: bool,
    ) -> PermissionState {
        let canon = Self::canonicalize_origin(origin);
        if canon.is_empty() {
            return PermissionState::Prompt;
        }

        if is_private {
            let cache = self.ephemeral_rules.read();
            if let Some(rules) = cache.get(&canon) {
                if let Some(&state) = rules.get(&perm) {
                    return state;
                }
            }
        }

        let cache = self.persistent_rules.read();
        if let Some(rules) = cache.get(&canon) {
            if let Some(&state) = rules.get(&perm) {
                return state;
            }
        }

        PermissionState::Prompt
    }

    /// Performs clear for origin operation.
    pub fn clear_for_origin(&self, origin: &str) {
        let canon = Self::canonicalize_origin(origin);
        if canon.is_empty() {
            return;
        }
        self.persistent_rules.write().remove(&canon);
        self.ephemeral_rules.write().remove(&canon);
    }

    /// Performs clear all operation.
    pub fn clear_all(&self) {
        self.persistent_rules.write().clear();
        self.ephemeral_rules.write().clear();
        self.audit_history.write().clear();
    }

    /// Performs clear ephemeral operation.
    pub fn clear_ephemeral(&self) {
        self.ephemeral_rules.write().clear();
    }

    // -------------------------------------------------------------------------
    // Audit History & Security Logging
    // -------------------------------------------------------------------------

    pub fn record_audit(
        &self,
        origin: &str,
        perm: PermissionType,
        state: PermissionState,
        is_private: bool,
    ) -> PermissionAuditRecord {
        let now_ms = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .map(|d| d.as_millis() as u64)
            .unwrap_or(0);

        let mut list = self.audit_history.write();
        let id = format!("perm_audit_{}_{}", now_ms, list.len());
        let record = PermissionAuditRecord {
            id,
            origin: origin.to_string(),
            permission: perm,
            state,
            timestamp: now_ms,
            is_private,
        };

        list.push_front(record.clone());
        if list.len() > self.max_audit_records {
            list.pop_back();
        }
        record
    }

    /// Returns the audit history.
    pub fn get_audit_history(&self, limit: usize, offset: usize) -> Vec<PermissionAuditRecord> {
        self.audit_history
            .read()
            .iter()
            .skip(offset)
            .take(limit)
            .cloned()
            .collect()
    }

    /// Returns the audit history for origin.
    pub fn get_audit_history_for_origin(&self, origin: &str) -> Vec<PermissionAuditRecord> {
        let canon = Self::canonicalize_origin(origin);
        self.audit_history
            .read()
            .iter()
            .filter(|r| r.origin == canon || canon.contains(&r.origin))
            .cloned()
            .collect()
    }

    /// Performs clear audit history operation.
    pub fn clear_audit_history(&self) {
        self.audit_history.write().clear();
    }

    /// Performs clear audit history range operation.
    pub fn clear_audit_history_range(&self, from_ms: u64, to_ms: u64) -> usize {
        let mut list = self.audit_history.write();
        let initial_len = list.len();
        list.retain(|r| r.timestamp < from_ms || r.timestamp > to_ms);
        initial_len - list.len()
    }

    /// Returns the site permissions json.
    pub fn get_site_permissions_json(&self, origin: &str, is_private: bool) -> String {
        let canon = Self::canonicalize_origin(origin);
        let mut map = HashMap::new();

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

        for perm in all_types {
            let state = self.get_permission(&canon, perm, is_private);
            map.insert(perm.to_str_name(), state.to_str_name());
        }

        serde_json::to_string(&map).unwrap_or_else(|_| "{}".to_string())
    }

    /// Returns the all customized origins.
    pub fn get_all_customized_origins(&self) -> Vec<String> {
        let mut origins: Vec<String> = self.persistent_rules.read().keys().cloned().collect();
        origins.sort();
        origins
    }

    /// Performs export persistent json operation.
    pub fn export_persistent_json(&self) -> String {
        serde_json::to_string(&*self.persistent_rules.read()).unwrap_or_else(|_| "{}".to_string())
    }

    /// Performs import persistent json operation.
    pub fn import_persistent_json(&self, json_str: &str) -> usize {
        if json_str.trim().is_empty() {
            return 0;
        }

        let imported: HashMap<String, HashMap<PermissionType, PermissionState>> =
            match serde_json::from_str(json_str) {
                Ok(v) => v,
                Err(_) => return 0,
            };

        let count = imported.len();
        let mut guard = self.persistent_rules.write();
        for (origin, rules) in imported {
            guard.insert(origin, rules);
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
        manager.set_permission(
            "https://example.com",
            PermissionType::Camera,
            PermissionState::Allow,
            false,
        );
        assert_eq!(
            manager.get_permission("https://example.com", PermissionType::Camera, false),
            PermissionState::Allow
        );

        // 2. Ephemeral Private partition
        manager.set_permission(
            "https://private.com",
            PermissionType::Microphone,
            PermissionState::Block,
            true,
        );
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
    fn test_permission_audit_history() {
        let manager = PermissionsManager::new();
        manager.set_permission(
            "https://github.com",
            PermissionType::Geolocation,
            PermissionState::Allow,
            false,
        );
        manager.set_permission(
            "https://tracker.com",
            PermissionType::Camera,
            PermissionState::Block,
            false,
        );

        let audit = manager.get_audit_history(10, 0);
        assert_eq!(audit.len(), 2);
        assert_eq!(audit[0].origin, "https://tracker.com");
        assert_eq!(audit[0].state, PermissionState::Block);

        let github_audit = manager.get_audit_history_for_origin("github.com");
        assert_eq!(github_audit.len(), 1);
        assert_eq!(github_audit[0].permission, PermissionType::Geolocation);
    }

    #[test]
    fn test_export_import_json() {
        let manager = PermissionsManager::new();
        manager.set_permission(
            "https://test.com",
            PermissionType::Geolocation,
            PermissionState::Allow,
            false,
        );

        let json = manager.export_persistent_json();
        assert!(json.contains("Geolocation"));

        let new_manager = PermissionsManager::new();
        let imported = new_manager.import_persistent_json(&json);
        assert_eq!(imported, 1);
        assert_eq!(
            new_manager.get_permission("https://test.com", PermissionType::Geolocation, false),
            PermissionState::Allow
        );
    }
}
