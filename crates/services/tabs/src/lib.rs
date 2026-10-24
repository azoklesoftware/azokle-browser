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
use std::sync::atomic::{AtomicU64, Ordering};
use std::sync::Arc;
use std::time::{SystemTime, UNIX_EPOCH};
use url::Url;

static TAB_COUNTER: AtomicU64 = AtomicU64::new(1);

/// Unique tab session representation containing navigation state and grouping metadata.
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
pub struct TabSession {
    /// Unique tab identifier formatted as `tab_{timestamp}_{counter}`.
    pub id: String,
    /// Currently loaded or pending navigation URL.
    pub url: String,
    /// Rendered document title or fallback label.
    pub title: String,
    #[serde(default)]
    pub favicon_url: Option<String>,
    /// Indicates whether the tab operates in private browsing mode (ephemeral storage).
    pub is_private: bool,
    /// Indicates whether the tab routes traffic through the Tor onion routing network.
    pub is_tor: bool,
    /// Indicates whether background processes/renderers for this tab are frozen to conserve RAM.
    pub is_sleeping: bool,
    /// Optional identifier grouping related tabs together.
    pub group_id: Option<String>,
    /// Human-readable title for the tab cluster.
    pub group_title: Option<String>,
    #[serde(default)]
    pub group_color: Option<u64>,
    #[serde(default)]
    pub parent_tab_id: Option<String>,
    /// Timestamp of most recent user interaction or focus.
    pub last_accessed_at: u64,
    /// Creation timestamp (milliseconds since epoch).
    pub created_at: u64,
}

impl TabSession {
    /// Sanitizes an untrusted URL intended for a browser tab.
    /// Strictly rejects dangerous and privileged execution schemes (javascript:, file:, content:,
    /// intent:, chrome:, resource:, etc.) and falls back to the default search engine homepage.
    pub fn sanitize_tab_url(url: &str) -> String {
        let trimmed = url.trim();
        if trimmed.is_empty()
            || trimmed.contains('\r')
            || trimmed.contains('\n')
            || trimmed.contains('\0')
        {
            return "https://search.azokle.com".to_string();
        }
        let lower = trimmed.to_lowercase();
        if lower.starts_with("javascript:")
            || lower.starts_with("file:")
            || lower.starts_with("content:")
            || lower.starts_with("intent:")
            || lower.starts_with("chrome:")
            || lower.starts_with("resource:")
            || lower.starts_with("jar:")
            || lower.starts_with("view-source:")
        {
            return "https://search.azokle.com".to_string();
        }
        trimmed.to_string()
    }

    /// Constructs a new tab session initialized with defaults.
    pub fn new(
        url: &str,
        title: &str,
        is_private: bool,
        is_tor: bool,
        group_id: Option<&str>,
        group_title: Option<&str>,
    ) -> Self {
        let now = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .map(|d| d.as_millis() as u64)
            .unwrap_or(0);
        let seq = TAB_COUNTER.fetch_add(1, Ordering::Relaxed);
        let id = format!("tab_{}_{}", now, seq);

        Self {
            id,
            url: if url.is_empty() {
                "https://search.azokle.com".to_string()
            } else {
                Self::sanitize_tab_url(url)
            },
            title: if title.is_empty() {
                "New Tab".to_string()
            } else {
                title.replace('\r', "").replace('\n', "").trim().to_string()
            },
            favicon_url: None,
            is_private,
            is_tor,
            is_sleeping: false,
            group_id: group_id.map(|s| s.to_string()),
            group_title: group_title.map(|s| s.to_string()),
            group_color: None,
            parent_tab_id: None,
            last_accessed_at: now,
            created_at: now,
        }
    }

    /// Securely wipes URL, title, and group metadata from memory for private or Tor tabs.
    ///
    /// Utilizes [`zeroize::Zeroize`] to zero memory pages against forensic RAM extraction.
    pub fn scrub_ephemeral(&mut self) {
        if self.is_private || self.is_tor {
            use zeroize::Zeroize;
            self.url.zeroize();
            self.title.zeroize();
            if let Some(ref mut g) = self.group_id {
                g.zeroize();
            }
            if let Some(ref mut gt) = self.group_title {
                gt.zeroize();
            }
        }
    }
}

/// Thread-safe browser tab manager coordinating tab lifecycle, focus, and session persistence.
pub struct TabManager {
    tabs: Arc<RwLock<Vec<TabSession>>>,
    active_tab_id: Arc<RwLock<Option<String>>>,
    recently_closed: Arc<RwLock<Vec<TabSession>>>,
    max_recently_closed: usize,
}

impl Default for TabManager {
    /// Returns the default instance.
    fn default() -> Self {
        Self::new()
    }
}

impl TabManager {
    /// Initializes a tab manager with a default home tab.
    pub fn new() -> Self {
        let default_tab = TabSession::new(
            "https://search.azokle.com",
            "New Tab",
            false,
            false,
            None,
            None,
        );
        let id = default_tab.id.clone();

        Self {
            tabs: Arc::new(RwLock::new(vec![default_tab])),
            active_tab_id: Arc::new(RwLock::new(Some(id))),
            recently_closed: Arc::new(RwLock::new(Vec::new())),
            max_recently_closed: 15,
        }
    }

    /// Spawns a new tab session and sets it as the active tab.
    pub fn open_tab(
        &self,
        url: &str,
        title: &str,
        is_private: bool,
        is_tor: bool,
        group_id: Option<&str>,
        group_title: Option<&str>,
    ) -> TabSession {
        let session = TabSession::new(url, title, is_private, is_tor, group_id, group_title);
        let id = session.id.clone();

        let mut tabs_guard = self.tabs.write();
        tabs_guard.push(session.clone());

        *self.active_tab_id.write() = Some(id);
        session
    }

    /// Closes a tab session by ID and selects a neighboring tab if the closed tab was focused.
    ///
    /// # Privacy Guarantee:
    /// Private and Tor tabs are NEVER pushed to the `recently_closed` stack.
    pub fn close_tab(&self, id: &str) -> Option<TabSession> {
        let mut tabs_guard = self.tabs.write();
        let index = tabs_guard.iter().position(|t| t.id == id)?;
        let closed = tabs_guard.remove(index);

        // Do not add private or Tor tabs to recently closed (privacy protection)
        if !closed.is_private && !closed.is_tor {
            let mut closed_guard = self.recently_closed.write();
            closed_guard.insert(0, closed.clone());
            if closed_guard.len() > self.max_recently_closed {
                closed_guard.pop();
            }
        }

        // Update active tab if the closed tab was active
        let mut active_guard = self.active_tab_id.write();
        if active_guard.as_deref() == Some(id) {
            *active_guard = if tabs_guard.is_empty() {
                None
            } else {
                let next_idx = if index >= tabs_guard.len() {
                    tabs_guard.len() - 1
                } else {
                    index
                };
                Some(tabs_guard[next_idx].id.clone())
            };
        }

        Some(closed)
    }

    /// Pops the most recently closed non-private tab from the undo stack and restores it.
    pub fn restore_recently_closed(&self) -> Option<TabSession> {
        let mut closed_guard = self.recently_closed.write();
        if closed_guard.is_empty() {
            return None;
        }
        let restored = closed_guard.remove(0);
        let mut tabs_guard = self.tabs.write();
        tabs_guard.push(restored.clone());
        *self.active_tab_id.write() = Some(restored.id.clone());
        Some(restored)
    }

    /// Returns the active tab.
    pub fn get_active_tab(&self) -> Option<TabSession> {
        let active_id = self.active_tab_id.read().clone()?;
        self.tabs.read().iter().find(|t| t.id == active_id).cloned()
    }

    /// Updates the active tab.
    pub fn set_active_tab(&self, id: &str) -> bool {
        let exists = self.tabs.read().iter().any(|t| t.id == id);
        if exists {
            let now = SystemTime::now()
                .duration_since(UNIX_EPOCH)
                .map(|d| d.as_millis() as u64)
                .unwrap_or(0);
            let mut tabs = self.tabs.write();
            if let Some(tab) = tabs.iter_mut().find(|t| t.id == id) {
                tab.last_accessed_at = now;
            }
            *self.active_tab_id.write() = Some(id.to_string());
            true
        } else {
            false
        }
    }

    /// Updates the tab url and title.
    pub fn update_tab_url_and_title(&self, id: &str, url: &str, title: &str) -> bool {
        let mut tabs = self.tabs.write();
        if let Some(tab) = tabs.iter_mut().find(|t| t.id == id) {
            if !url.is_empty() {
                tab.url = TabSession::sanitize_tab_url(url);
            }
            if !title.is_empty() {
                tab.title = title.replace('\r', "").replace('\n', "").trim().to_string();
            }
            true
        } else {
            false
        }
    }

    /// Returns the all tabs.
    pub fn get_all_tabs(&self) -> Vec<TabSession> {
        self.tabs.read().clone()
    }

    /// Performs close all tabs operation.
    pub fn close_all_tabs(&self, include_private: bool) {
        let mut tabs = self.tabs.write();
        if include_private {
            tabs.clear();
            *self.active_tab_id.write() = None;
        } else {
            tabs.retain(|t| t.is_private || t.is_tor);
            *self.active_tab_id.write() = tabs.first().map(|t| t.id.clone());
        }
    }

    /// Closes all active private and Tor tabs, maintaining normal browsing tabs intact.
    pub fn close_private_tabs(&self) {
        let mut tabs = self.tabs.write();
        tabs.retain(|t| !t.is_private && !t.is_tor);
        *self.active_tab_id.write() = tabs.first().map(|t| t.id.clone());
    }

    /// Automatically organizes all open tabs into clusters based on their target registrable domain,
    /// enforcing strict isolation across Standard, Private, and Tor browsing modes.
    pub fn auto_group_by_domain(&self) {
        let mut tabs = self.tabs.write();
        for tab in tabs.iter_mut() {
            if let Ok(parsed) = Url::parse(&tab.url) {
                if let Some(host) = parsed.host_str() {
                    let domain = host.trim_start_matches("www.").to_string();
                    let prefix = if tab.is_tor {
                        "group_tor"
                    } else if tab.is_private {
                        "group_priv"
                    } else {
                        "group_std"
                    };
                    tab.group_id = Some(format!("{}_{}", prefix, domain));
                    tab.group_title = Some(domain);
                }
            }
        }
    }

    /// Exports persistent tabs to JSON format for session restoration across application restarts.
    ///
    /// # Privacy Guarantee:
    /// Strictly filters out all private and Tor tabs before serialization.
    pub fn export_persistent_json(&self) -> String {
        let tabs = self.tabs.read();
        let persistent_tabs: Vec<&TabSession> =
            tabs.iter().filter(|t| !t.is_private && !t.is_tor).collect();
        serde_json::to_string(&persistent_tabs).unwrap_or_else(|_| "[]".to_string())
    }

    /// Imports persistent tabs from JSON.
    pub fn import_persistent_json(&self, json_data: &str) -> usize {
        let imported: Vec<TabSession> = match serde_json::from_str(json_data) {
            Ok(v) => v,
            Err(_) => return 0,
        };

        if imported.is_empty() {
            return 0;
        }

        let mut count = 0;
        let mut tabs = self.tabs.write();
        tabs.retain(|t| t.is_private || t.is_tor); // keep private in memory

        for mut tab in imported {
            let lower_url = tab.url.trim().to_lowercase();
            // AZK-VULN-12: Reject dangerous and privileged schemes from imported tab backups
            if lower_url.starts_with("javascript:")
                || lower_url.starts_with("file:")
                || lower_url.starts_with("content:")
                || lower_url.starts_with("chrome:")
                || lower_url.starts_with("resource:")
                || lower_url.starts_with("view-source:")
                || lower_url.starts_with("data:")
                || lower_url.starts_with("blob:")
                || lower_url.is_empty()
            {
                tab.url = "https://search.azokle.com".to_string();
                tab.title = "New Tab".to_string();
            }

            // Detect .onion URLs and enforce Tor/Private mode isolation
            let is_onion = if let Ok(parsed) = Url::parse(&tab.url) {
                parsed.host_str().map(|h| {
                    let host = h.trim_end_matches('.').to_lowercase();
                    host == "onion" || host.ends_with(".onion")
                }).unwrap_or(false)
            } else {
                false
            };

            if is_onion {
                tab.is_tor = true;
                tab.is_private = true;
            } else {
                tab.is_private = false;
                tab.is_tor = false;
            }

            tabs.push(tab);
            count += 1;
        }

        if self.active_tab_id.read().is_none() {
            *self.active_tab_id.write() = tabs.first().map(|t| t.id.clone());
        }

        count
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_tab_lifecycle() {
        let manager = TabManager::new();
        assert_eq!(manager.get_all_tabs().len(), 1);

        let tab2 = manager.open_tab("https://github.com", "GitHub", false, false, None, None);
        assert_eq!(manager.get_all_tabs().len(), 2);
        assert_eq!(manager.get_active_tab().unwrap().id, tab2.id);

        // Close tab2 and restore
        let closed = manager.close_tab(&tab2.id).unwrap();
        assert_eq!(closed.url, "https://github.com");
        assert_eq!(manager.get_all_tabs().len(), 1);

        let restored = manager.restore_recently_closed().unwrap();
        assert_eq!(restored.id, tab2.id);
        assert_eq!(manager.get_all_tabs().len(), 2);
    }

    #[test]
    fn test_private_tab_disk_exclusion() {
        let manager = TabManager::new();
        manager.open_tab("https://secret.onion", "Secret", true, true, None, None);

        let json = manager.export_persistent_json();
        assert!(!json.contains("secret.onion"));
    }

    #[test]
    fn test_auto_domain_grouping() {
        let manager = TabManager::new();
        manager.open_tab(
            "https://www.youtube.com/watch?v=123",
            "Video",
            false,
            false,
            None,
            None,
        );
        manager.open_tab(
            "https://www.youtube.com/watch?v=456",
            "Private Video",
            true,
            false,
            None,
            None,
        );
        manager.open_tab(
            "https://www.youtube.com/watch?v=789",
            "Tor Video",
            true,
            true,
            None,
            None,
        );
        manager.auto_group_by_domain();

        let tabs = manager.get_all_tabs();
        let yt_std = tabs
            .iter()
            .find(|t| !t.is_private && !t.is_tor && t.url.contains("123"))
            .unwrap();
        let yt_priv = tabs
            .iter()
            .find(|t| t.is_private && !t.is_tor && t.url.contains("456"))
            .unwrap();
        let yt_tor = tabs
            .iter()
            .find(|t| t.is_tor && t.url.contains("789"))
            .unwrap();

        assert_eq!(yt_std.group_id.as_deref(), Some("group_std_youtube.com"));
        assert_eq!(yt_priv.group_id.as_deref(), Some("group_priv_youtube.com"));
        assert_eq!(yt_tor.group_id.as_deref(), Some("group_tor_youtube.com"));
        assert_eq!(yt_std.group_title.as_deref(), Some("youtube.com"));
    }

    #[test]
    fn test_import_persistent_json_sanitization() {
        let manager = TabManager::new();
        let mut t1 = TabSession::new("https://example.com", "Legit", false, false, None, None);
        t1.id = "1".to_string();
        let mut t2 = TabSession::new("javascript:steal()", "Danger JS", false, false, None, None);
        t2.id = "2".to_string();
        let mut t3 = TabSession::new("file:///etc/hosts", "Danger File", false, false, None, None);
        t3.id = "3".to_string();
        let mut t4 = TabSession::new("http://duckduckgogg42xjoc72x3sjasowoarfbgcmvfimaftt6twagswzczad.onion", "Onion Tab", false, false, None, None);
        t4.id = "4".to_string();

        let backup = serde_json::to_string(&vec![t1, t2, t3, t4]).unwrap();
        let count = manager.import_persistent_json(&backup);
        assert_eq!(count, 4);

        let tabs = manager.get_all_tabs();
        let js_tab = tabs.iter().find(|t| t.id == "2").unwrap();
        assert_eq!(js_tab.url, "https://search.azokle.com");

        let file_tab = tabs.iter().find(|t| t.id == "3").unwrap();
        assert_eq!(file_tab.url, "https://search.azokle.com");

        let onion_tab = tabs.iter().find(|t| t.id == "4").unwrap();
        assert!(onion_tab.is_tor);
        assert!(onion_tab.is_private);
    }

    #[test]
    fn test_tab_session_and_update_url_sanitization() {
        let manager = TabManager::new();
        let tab = manager.open_tab(
            "javascript:alert(document.cookie)",
            "Malicious Tab",
            false,
            false,
            None,
            None,
        );
        assert_eq!(tab.url, "https://search.azokle.com");

        let file_tab = manager.open_tab(
            "file:///data/data/com.azokle.search/databases/history.db",
            "File Tab",
            false,
            false,
            None,
            None,
        );
        assert_eq!(file_tab.url, "https://search.azokle.com");

        // Updating with dangerous scheme
        assert!(manager.update_tab_url_and_title(
            &tab.id,
            "chrome://flags",
            "Updated Title\r\nInjected",
        ));
        let updated = manager
            .get_all_tabs()
            .into_iter()
            .find(|t| t.id == tab.id)
            .unwrap();
        assert_eq!(updated.url, "https://search.azokle.com");
        assert_eq!(updated.title, "Updated TitleInjected");
    }
}
