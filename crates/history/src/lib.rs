use parking_lot::RwLock;
use serde::{Deserialize, Serialize};
use std::sync::Arc;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct HistoryEntry {
    pub id: String,
    pub title: String,
    pub url: String,
    pub visit_time: u64,
    pub visit_count: u32,
    pub domain: String,
}

#[derive(Clone, Default)]
pub struct HistoryManager {
    entries: Arc<RwLock<Vec<HistoryEntry>>>,
}

impl HistoryManager {
    pub fn new() -> Self {
        Self {
            entries: Arc::new(RwLock::new(Vec::new())),
        }
    }

    pub fn record_visit(&self, title: &str, url: &str) {
        if url.is_empty() || url.starts_with("about:") {
            return;
        }

        let now = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .map(|d| d.as_secs())
            .unwrap_or(0);

        let domain = url::Url::parse(url)
            .ok()
            .and_then(|u| u.host_str().map(|h| h.to_string()))
            .unwrap_or_default();

        let mut list = self.entries.write();
        if let Some(existing) = list.iter_mut().find(|e| e.url == url) {
            existing.visit_count += 1;
            existing.visit_time = now;
            if !title.is_empty() {
                existing.title = title.to_string();
            }
        } else {
            let id = format!("h_{}_{}", now, list.len());
            list.push(HistoryEntry {
                id,
                title: if title.is_empty() { url.to_string() } else { title.to_string() },
                url: url.to_string(),
                visit_time: now,
                visit_count: 1,
                domain,
            });
        }
    }

    pub fn search(&self, query: &str) -> Vec<HistoryEntry> {
        let q = query.to_lowercase();
        self.entries
            .read()
            .iter()
            .filter(|e| e.title.to_lowercase().contains(&q) || e.url.to_lowercase().contains(&q))
            .cloned()
            .collect()
    }

    pub fn clear(&self) {
        self.entries.write().clear();
    }

    pub fn get_recent(&self, limit: usize) -> Vec<HistoryEntry> {
        let mut list = self.entries.read().clone();
        list.sort_by(|a, b| b.visit_time.cmp(&a.visit_time));
        list.truncate(limit);
        list
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_history() {
        let history = HistoryManager::new();
        history.record_visit("Azokle", "https://search.azokle.com");
        history.record_visit("Azokle", "https://search.azokle.com");
        history.record_visit("GitHub", "https://github.com");

        let recent = history.get_recent(10);
        assert_eq!(recent.len(), 2);
        let azokle = recent.iter().find(|e| e.url == "https://search.azokle.com").unwrap();
        assert_eq!(azokle.visit_count, 2);

        let search = history.search("github");
        assert_eq!(search.len(), 1);

        history.clear();
        assert_eq!(history.get_recent(10).len(), 0);
    }
}
