use parking_lot::RwLock;
use serde::{Deserialize, Serialize};
use std::sync::atomic::{AtomicU64, Ordering};
use std::sync::Arc;
use std::time::{SystemTime, UNIX_EPOCH};
use url::Url;

static TAB_COUNTER: AtomicU64 = AtomicU64::new(1);

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
pub struct TabSession {
    pub id: String,
    pub url: String,
    pub title: String,
    pub is_private: bool,
    pub is_tor: bool,
    pub is_sleeping: bool,
    pub group_id: Option<String>,
    pub group_title: Option<String>,
    pub last_accessed_at: u64,
    pub created_at: u64,
}

impl TabSession {
    pub fn new(url: &str, title: &str, is_private: bool, is_tor: bool, group_id: Option<&str>, group_title: Option<&str>) -> Self {
        let now = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .map(|d| d.as_millis() as u64)
            .unwrap_or(0);
        let seq = TAB_COUNTER.fetch_add(1, Ordering::Relaxed);
        let id = format!("tab_{}_{}", now, seq);

        Self {
            id,
            url: if url.is_empty() { "https://search.azokle.com".to_string() } else { url.to_string() },
            title: if title.is_empty() { "New Tab".to_string() } else { title.to_string() },
            is_private,
            is_tor,
            is_sleeping: false,
            group_id: group_id.map(|s| s.to_string()),
            group_title: group_title.map(|s| s.to_string()),
            last_accessed_at: now,
            created_at: now,
        }
    }

    /// Securely wipes URL and title memory for private / Tor tabs
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

pub struct TabManager {
    tabs: Arc<RwLock<Vec<TabSession>>>,
    active_tab_id: Arc<RwLock<Option<String>>>,
    recently_closed: Arc<RwLock<Vec<TabSession>>>,
    max_recently_closed: usize,
}

impl Default for TabManager {
    fn default() -> Self {
        Self::new()
    }
}

impl TabManager {
    pub fn new() -> Self {
        let default_tab = TabSession::new("https://search.azokle.com", "New Tab", false, false, None, None);
        let id = default_tab.id.clone();

        Self {
            tabs: Arc::new(RwLock::new(vec![default_tab])),
            active_tab_id: Arc::new(RwLock::new(Some(id))),
            recently_closed: Arc::new(RwLock::new(Vec::new())),
            max_recently_closed: 15,
        }
    }

    pub fn open_tab(&self, url: &str, title: &str, is_private: bool, is_tor: bool, group_id: Option<&str>, group_title: Option<&str>) -> TabSession {
        let session = TabSession::new(url, title, is_private, is_tor, group_id, group_title);
        let id = session.id.clone();

        let mut tabs_guard = self.tabs.write();
        tabs_guard.push(session.clone());

        *self.active_tab_id.write() = Some(id);
        session
    }

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

    pub fn get_active_tab(&self) -> Option<TabSession> {
        let active_id = self.active_tab_id.read().clone()?;
        self.tabs.read().iter().find(|t| t.id == active_id).cloned()
    }

    pub fn set_active_tab(&self, id: &str) -> bool {
        let exists = self.tabs.read().iter().any(|t| t.id == id);
        if exists {
            let now = SystemTime::now().duration_since(UNIX_EPOCH).map(|d| d.as_millis() as u64).unwrap_or(0);
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

    pub fn update_tab_url_and_title(&self, id: &str, url: &str, title: &str) -> bool {
        let mut tabs = self.tabs.write();
        if let Some(tab) = tabs.iter_mut().find(|t| t.id == id) {
            if !url.is_empty() {
                tab.url = url.to_string();
            }
            if !title.is_empty() {
                tab.title = title.to_string();
            }
            true
        } else {
            false
        }
    }

    pub fn get_all_tabs(&self) -> Vec<TabSession> {
        self.tabs.read().clone()
    }

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

    pub fn close_private_tabs(&self) {
        let mut tabs = self.tabs.write();
        tabs.retain(|t| !t.is_private && !t.is_tor);
        *self.active_tab_id.write() = tabs.first().map(|t| t.id.clone());
    }

    pub fn auto_group_by_domain(&self) {
        let mut tabs = self.tabs.write();
        for tab in tabs.iter_mut() {
            if let Ok(parsed) = Url::parse(&tab.url) {
                if let Some(host) = parsed.host_str() {
                    let domain = host.trim_start_matches("www.").to_string();
                    tab.group_id = Some(format!("group_{}", domain));
                    tab.group_title = Some(domain);
                }
            }
        }
    }

    /// Exports persistent tabs to JSON (Strictly excludes private and Tor tabs).
    pub fn export_persistent_json(&self) -> String {
        let tabs = self.tabs.read();
        let persistent_tabs: Vec<&TabSession> = tabs.iter().filter(|t| !t.is_private && !t.is_tor).collect();
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
            tab.is_private = false;
            tab.is_tor = false;
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
        manager.open_tab("https://www.youtube.com/watch?v=123", "Video", false, false, None, None);
        manager.auto_group_by_domain();

        let tabs = manager.get_all_tabs();
        let yt_tab = tabs.iter().find(|t| t.url.contains("youtube")).unwrap();
        assert_eq!(yt_tab.group_title.as_deref(), Some("youtube.com"));
    }
}
