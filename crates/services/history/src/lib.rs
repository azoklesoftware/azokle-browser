use parking_lot::RwLock;
use serde::{Deserialize, Serialize};
use std::collections::VecDeque;
use std::sync::Arc;
use url::Url;

/// Record representing a visited web page.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct HistoryEntry {
    /// Unique visit entry identifier formatted as `hist_{timestamp}_{index}`.
    pub id: String,
    /// Page title recorded at navigation or updated upon document parse.
    pub title: String,
    /// Canonical page URL.
    pub url: String,
    /// Millisecond Unix timestamp of the most recent visit.
    pub visit_time: u64,
    /// Total cumulative number of visits to this URL.
    pub visit_count: u32,
    /// Hostname / domain component extracted from the URL.
    pub domain: String,
    /// Indicates whether the URL is also marked in the bookmarks collection.
    pub is_bookmarked: bool,
}

/// Thread-safe manager for browsing history entries, query search, and privacy sanitization.
#[derive(Clone)]
pub struct HistoryManager {
    entries: Arc<RwLock<VecDeque<HistoryEntry>>>,
    max_entries: usize,
}

impl Default for HistoryManager {
    fn default() -> Self {
        Self::new()
    }
}

impl HistoryManager {
    /// Creates a history manager with a default capacity of 50,000 entries.
    pub fn new() -> Self {
        Self {
            entries: Arc::new(RwLock::new(VecDeque::new())),
            max_entries: 50_000,
        }
    }

    /// Creates a history manager with a custom maximum entry capacity.
    pub fn with_capacity(max_entries: usize) -> Self {
        Self {
            entries: Arc::new(RwLock::new(VecDeque::new())),
            max_entries,
        }
    }

    /// Records a page visit, automatically deduplicating existing entries by updating visit count.
    ///
    /// # Privacy Guarantees:
    /// - Strictly skips visits occurring in private or Tor tabs (`is_private: true`).
    /// - Ignores pseudo-protocols and internal URLs (`about:`, `javascript:`, `data:`).
    pub fn record_visit(&self, title: &str, url_str: &str, is_private: bool) -> Option<HistoryEntry> {
        if is_private || url_str.trim().is_empty() {
            return None;
        }

        let clean_url = url_str.trim();
        if clean_url.starts_with("about:") || clean_url.starts_with("javascript:") || clean_url.starts_with("data:") {
            return None;
        }

        let now_ms = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .map(|d| d.as_millis() as u64)
            .unwrap_or(0);

        let domain = Url::parse(clean_url)
            .ok()
            .and_then(|u| u.host_str().map(|h| h.to_string()))
            .unwrap_or_default();

        let clean_title = if title.trim().is_empty() {
            clean_url.to_string()
        } else {
            title.trim().to_string()
        };

        let mut list = self.entries.write();
        if let Some(existing) = list.iter_mut().find(|e| e.url == clean_url) {
            existing.visit_count += 1;
            existing.visit_time = now_ms;
            if !title.trim().is_empty() {
                existing.title = clean_title;
            }
            Some(existing.clone())
        } else {
            let id = format!("hist_{}_{}", now_ms, list.len());
            let entry = HistoryEntry {
                id,
                title: clean_title,
                url: clean_url.to_string(),
                visit_time: now_ms,
                visit_count: 1,
                domain,
                is_bookmarked: false,
            };

            list.push_front(entry.clone());
            if list.len() > self.max_entries {
                list.pop_back();
            }
            Some(entry)
        }
    }

    pub fn search(&self, query: &str, limit: usize) -> Vec<HistoryEntry> {
        let q = query.trim().to_lowercase();
        if q.is_empty() {
            return self.get_recent(limit, 0);
        }

        self.entries
            .read()
            .iter()
            .filter(|e| e.title.to_lowercase().contains(&q) || e.url.to_lowercase().contains(&q) || e.domain.to_lowercase().contains(&q))
            .take(limit.max(1))
            .cloned()
            .collect()
    }

    pub fn get_recent(&self, limit: usize, offset: usize) -> Vec<HistoryEntry> {
        self.entries
            .read()
            .iter()
            .skip(offset)
            .take(limit)
            .cloned()
            .collect()
    }

    pub fn delete_entry(&self, id: &str) -> bool {
        let mut list = self.entries.write();
        if let Some(pos) = list.iter().position(|e| e.id == id) {
            list.remove(pos);
            true
        } else {
            false
        }
    }

    pub fn delete_url(&self, url_str: &str) -> bool {
        let mut list = self.entries.write();
        let initial_len = list.len();
        list.retain(|e| e.url != url_str);
        list.len() < initial_len
    }

    pub fn delete_domain(&self, domain: &str) -> usize {
        let d_clean = domain.trim().to_lowercase();
        let mut list = self.entries.write();
        let initial_len = list.len();
        list.retain(|e| e.domain.to_lowercase() != d_clean);
        initial_len - list.len()
    }

    pub fn clear_time_range(&self, from_ms: u64, to_ms: u64) -> usize {
        let mut list = self.entries.write();
        let initial_len = list.len();
        list.retain(|e| e.visit_time < from_ms || e.visit_time > to_ms);
        initial_len - list.len()
    }

    pub fn clear_all(&self) {
        self.entries.write().clear();
    }

    pub fn total_count(&self) -> usize {
        self.entries.read().len()
    }

    pub fn export_json(&self) -> String {
        serde_json::to_string(&*self.entries.read()).unwrap_or_else(|_| "[]".to_string())
    }

    pub fn import_json(&self, json_str: &str) -> usize {
        if json_str.trim().is_empty() {
            return 0;
        }

        let imported: VecDeque<HistoryEntry> = match serde_json::from_str(json_str) {
            Ok(v) => v,
            Err(_) => return 0,
        };

        let count = imported.len();
        let mut list = self.entries.write();
        *list = imported;
        count
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_history_lifecycle() {
        let history = HistoryManager::new();

        // 1. Regular recording
        let entry = history.record_visit("Azokle Search", "https://search.azokle.com", false);
        assert!(entry.is_some());
        assert_eq!(history.total_count(), 1);

        // 2. Private tabs are ignored
        let private_entry = history.record_visit("Secret", "https://secret.com", true);
        assert!(private_entry.is_none());
        assert_eq!(history.total_count(), 1);

        // 3. Increment visit count on revisit
        history.record_visit("Azokle Search", "https://search.azokle.com", false);
        let recent = history.get_recent(10, 0);
        assert_eq!(recent[0].visit_count, 2);

        // 4. Search
        let results = history.search("search", 5);
        assert_eq!(results.len(), 1);

        // 5. Delete by URL
        assert!(history.delete_url("https://search.azokle.com"));
        assert_eq!(history.total_count(), 0);
    }

    #[test]
    fn test_time_range_clearing() {
        let history = HistoryManager::new();
        history.record_visit("Page 1", "https://page1.com", false);
        history.record_visit("Page 2", "https://page2.com", false);

        assert_eq!(history.total_count(), 2);
        let cleared = history.clear_time_range(0, u64::MAX);
        assert_eq!(cleared, 2);
        assert_eq!(history.total_count(), 0);
    }
}
