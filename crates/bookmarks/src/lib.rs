use parking_lot::RwLock;
use serde::{Deserialize, Serialize};
use std::sync::Arc;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct BookmarkItem {
    pub id: String,
    pub title: String,
    pub url: String,
    pub parent_folder: Option<String>,
    pub date_added: u64,
    pub favicon_url: Option<String>,
}

#[derive(Clone, Default)]
pub struct BookmarkManager {
    items: Arc<RwLock<Vec<BookmarkItem>>>,
}

impl BookmarkManager {
    pub fn new() -> Self {
        Self {
            items: Arc::new(RwLock::new(Vec::new())),
        }
    }

    pub fn add(&self, title: &str, url: &str, parent_folder: Option<&str>) -> BookmarkItem {
        let now = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .map(|d| d.as_secs())
            .unwrap_or(0);
        let id = format!("bm_{}_{}", now, self.items.read().len());

        let item = BookmarkItem {
            id,
            title: title.to_string(),
            url: url.to_string(),
            parent_folder: parent_folder.map(|s| s.to_string()),
            date_added: now,
            favicon_url: None,
        };

        self.items.write().push(item.clone());
        item
    }

    pub fn remove(&self, id: &str) -> bool {
        let mut list = self.items.write();
        if let Some(pos) = list.iter().position(|b| b.id == id) {
            list.remove(pos);
            true
        } else {
            false
        }
    }

    pub fn search(&self, query: &str) -> Vec<BookmarkItem> {
        let q = query.to_lowercase();
        self.items
            .read()
            .iter()
            .filter(|b| b.title.to_lowercase().contains(&q) || b.url.to_lowercase().contains(&q))
            .cloned()
            .collect()
    }

    pub fn get_all(&self) -> Vec<BookmarkItem> {
        self.items.read().clone()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_bookmarks() {
        let manager = BookmarkManager::new();
        let b1 = manager.add("Azokle Search", "https://search.azokle.com", None);
        let b2 = manager.add("Rust Lang", "https://rust-lang.org", None);

        assert_eq!(manager.get_all().len(), 2);
        let results = manager.search("Azokle");
        assert_eq!(results.len(), 1);
        assert_eq!(results[0].id, b1.id);

        assert!(manager.remove(&b2.id));
        assert_eq!(manager.get_all().len(), 1);
    }
}
