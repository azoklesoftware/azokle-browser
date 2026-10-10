use parking_lot::RwLock;
use serde::{Deserialize, Serialize};
use std::sync::Arc;

/// Persistent browser preferences and shield operational settings.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct BrowserSettings {
    /// Active default search engine provider identifier.
    pub search_engine: String,
    /// Safe search filtering level (`strict`, `moderate`, `off`).
    pub safe_search: String,
    /// Color scheme mode (`system`, `dark`, `light`).
    pub theme_mode: String,
    /// Recent search queries sliding window.
    pub recent_searches: Vec<String>,
    /// Global toggle for network ad blocking.
    pub is_adblock_enabled: bool,
    /// Global toggle for tracking script and CNAME uncloaking protection.
    pub is_tracker_blocking_enabled: bool,
    /// Toggle allowing audio/video playback to persist when tabs or application are backgrounded.
    pub is_background_audio_enabled: bool,
    /// Active DNS-over-HTTPS provider identifier.
    pub doh_provider: String,
    /// Optional custom DoH endpoint URL.
    pub custom_doh_url: Option<String>,
}

impl Default for BrowserSettings {
    fn default() -> Self {
        Self {
            search_engine: "duckduckgo".to_string(),
            safe_search: "moderate".to_string(),
            theme_mode: "system".to_string(),
            recent_searches: Vec::new(),
            is_adblock_enabled: true,
            is_tracker_blocking_enabled: true,
            is_background_audio_enabled: true,
            doh_provider: "cloudflare".to_string(),
            custom_doh_url: None,
        }
    }
}

/// Thread-safe controller for browser settings and recent search history.
#[derive(Clone)]
pub struct SettingsManager {
    settings: Arc<RwLock<BrowserSettings>>,
    max_recent_searches: usize,
}

impl Default for SettingsManager {
    fn default() -> Self {
        Self::new()
    }
}

impl SettingsManager {
    /// Initializes settings with defaults and an 8-item recent search history limit.
    pub fn new() -> Self {
        Self {
            settings: Arc::new(RwLock::new(BrowserSettings::default())),
            max_recent_searches: 8,
        }
    }

    pub fn get_search_engine(&self) -> String {
        self.settings.read().search_engine.clone()
    }

    pub fn set_search_engine(&self, engine: &str) {
        self.settings.write().search_engine = engine.trim().to_lowercase();
    }

    pub fn get_theme_mode(&self) -> String {
        self.settings.read().theme_mode.clone()
    }

    pub fn set_theme_mode(&self, theme: &str) {
        self.settings.write().theme_mode = theme.trim().to_lowercase();
    }

    pub fn is_adblock_enabled(&self) -> bool {
        self.settings.read().is_adblock_enabled
    }

    pub fn set_adblock_enabled(&self, enabled: bool) {
        self.settings.write().is_adblock_enabled = enabled;
    }

    pub fn is_background_audio_enabled(&self) -> bool {
        self.settings.read().is_background_audio_enabled
    }

    pub fn set_background_audio_enabled(&self, enabled: bool) {
        self.settings.write().is_background_audio_enabled = enabled;
    }

    pub fn get_doh_provider(&self) -> String {
        self.settings.read().doh_provider.clone()
    }

    pub fn set_doh_provider(&self, provider: &str) {
        self.settings.write().doh_provider = provider.trim().to_lowercase();
    }

    pub fn get_recent_searches(&self) -> Vec<String> {
        self.settings.read().recent_searches.clone()
    }

    pub fn add_recent_search(&self, query: &str) {
        let clean = query.trim();
        if clean.is_empty() {
            return;
        }

        let mut guard = self.settings.write();
        guard
            .recent_searches
            .retain(|q| !q.eq_ignore_ascii_case(clean));
        guard.recent_searches.insert(0, clean.to_string());
        if guard.recent_searches.len() > self.max_recent_searches {
            guard.recent_searches.truncate(self.max_recent_searches);
        }
    }

    pub fn remove_recent_search(&self, query: &str) {
        let clean = query.trim();
        self.settings
            .write()
            .recent_searches
            .retain(|q| !q.eq_ignore_ascii_case(clean));
    }

    pub fn clear_recent_searches(&self) {
        self.settings.write().recent_searches.clear();
    }

    pub fn export_json(&self) -> String {
        serde_json::to_string(&*self.settings.read()).unwrap_or_else(|_| "{}".to_string())
    }

    pub fn import_json(&self, json_str: &str) -> bool {
        if json_str.trim().is_empty() {
            return false;
        }

        if let Ok(imported) = serde_json::from_str::<BrowserSettings>(json_str) {
            *self.settings.write() = imported;
            true
        } else {
            false
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_settings_recent_searches() {
        let manager = SettingsManager::new();
        manager.add_recent_search("Rust");
        manager.add_recent_search("Azokle");
        manager.add_recent_search("Rust"); // Re-add moves to front

        let list = manager.get_recent_searches();
        assert_eq!(list.len(), 2);
        assert_eq!(list[0], "Rust");
        assert_eq!(list[1], "Azokle");

        manager.remove_recent_search("Azokle");
        assert_eq!(manager.get_recent_searches().len(), 1);
    }

    #[test]
    fn test_settings_export_import() {
        let manager = SettingsManager::new();
        manager.set_search_engine("searx");
        manager.set_theme_mode("dark");

        let json = manager.export_json();
        assert!(json.contains("searx"));

        let new_manager = SettingsManager::new();
        assert!(new_manager.import_json(&json));
        assert_eq!(new_manager.get_search_engine(), "searx");
        assert_eq!(new_manager.get_theme_mode(), "dark");
    }
}
