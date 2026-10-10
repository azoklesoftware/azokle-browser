use parking_lot::RwLock;
use serde::{Deserialize, Serialize};
use std::sync::Arc;

/// Bookmark leaf or container folder item in the user's bookmarks tree.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct BookmarkItem {
    /// Unique bookmark identifier formatted as `bm_{timestamp}_{index}` or `folder_{timestamp}_{index}`.
    pub id: String,
    /// User-visible title label.
    pub title: String,
    /// Destination web address (empty for folders).
    pub url: String,
    /// Optional parent folder ID establishing the tree hierarchy.
    pub parent_folder_id: Option<String>,
    /// Creation timestamp (milliseconds since epoch).
    pub date_added: u64,
    /// Optional cached favicon URL.
    pub favicon_url: Option<String>,
    /// True if the item is a folder container that can hold children.
    pub is_folder: bool,
}

/// Thread-safe manager for browser bookmarks and bookmark folder trees.
#[derive(Clone)]
pub struct BookmarkManager {
    items: Arc<RwLock<Vec<BookmarkItem>>>,
}

impl Default for BookmarkManager {
    fn default() -> Self {
        Self::new()
    }
}

impl BookmarkManager {
    /// Creates an empty bookmarks manager.
    pub fn new() -> Self {
        Self {
            items: Arc::new(RwLock::new(Vec::new())),
        }
    }

    /// Appends a new web bookmark item into the specified folder.
    pub fn add_bookmark(
        &self,
        title: &str,
        url_str: &str,
        parent_folder_id: Option<&str>,
        favicon_url: Option<&str>,
    ) -> BookmarkItem {
        let now_ms = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .map(|d| d.as_millis() as u64)
            .unwrap_or(0);

        let id = format!("bm_{}_{}", now_ms, self.items.read().len());
        let item = BookmarkItem {
            id,
            title: if title.trim().is_empty() {
                url_str.to_string()
            } else {
                title.trim().to_string()
            },
            url: url_str.trim().to_string(),
            parent_folder_id: parent_folder_id.map(|s| s.to_string()),
            date_added: now_ms,
            favicon_url: favicon_url.map(|s| s.to_string()),
            is_folder: false,
        };

        self.items.write().push(item.clone());
        item
    }

    pub fn create_folder(&self, title: &str, parent_folder_id: Option<&str>) -> BookmarkItem {
        let now_ms = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .map(|d| d.as_millis() as u64)
            .unwrap_or(0);

        let id = format!("folder_{}_{}", now_ms, self.items.read().len());
        let item = BookmarkItem {
            id,
            title: title.trim().to_string(),
            url: String::new(),
            parent_folder_id: parent_folder_id.map(|s| s.to_string()),
            date_added: now_ms,
            favicon_url: None,
            is_folder: true,
        };

        self.items.write().push(item.clone());
        item
    }

    pub fn remove_item(&self, id: &str) -> bool {
        let mut list = self.items.write();
        if let Some(pos) = list.iter().position(|b| b.id == id) {
            let is_folder = list[pos].is_folder;
            list.remove(pos);
            if is_folder {
                // Remove recursive children
                list.retain(|b| b.parent_folder_id.as_deref() != Some(id));
            }
            true
        } else {
            false
        }
    }

    pub fn update_item(
        &self,
        id: &str,
        title: Option<&str>,
        url_str: Option<&str>,
        parent_folder_id: Option<Option<&str>>,
        favicon_url: Option<Option<&str>>,
    ) -> bool {
        let mut list = self.items.write();
        if let Some(item) = list.iter_mut().find(|b| b.id == id) {
            if let Some(t) = title {
                item.title = t.trim().to_string();
            }
            if let Some(u) = url_str {
                item.url = u.trim().to_string();
            }
            if let Some(parent) = parent_folder_id {
                item.parent_folder_id = parent.map(|s| s.to_string());
            }
            if let Some(fav) = favicon_url {
                item.favicon_url = fav.map(|s| s.to_string());
            }
            true
        } else {
            false
        }
    }

    pub fn search(&self, query: &str) -> Vec<BookmarkItem> {
        let q = query.trim().to_lowercase();
        if q.is_empty() {
            return self.get_all();
        }

        self.items
            .read()
            .iter()
            .filter(|b| b.title.to_lowercase().contains(&q) || b.url.to_lowercase().contains(&q))
            .cloned()
            .collect()
    }

    pub fn is_bookmarked(&self, url_str: &str) -> bool {
        let clean = url_str.trim().to_lowercase();
        if clean.is_empty() {
            return false;
        }
        self.items
            .read()
            .iter()
            .any(|b| !b.is_folder && b.url.to_lowercase() == clean)
    }

    pub fn get_all(&self) -> Vec<BookmarkItem> {
        self.items.read().clone()
    }

    pub fn get_folder_items(&self, folder_id: Option<&str>) -> Vec<BookmarkItem> {
        self.items
            .read()
            .iter()
            .filter(|b| b.parent_folder_id.as_deref() == folder_id)
            .cloned()
            .collect()
    }

    pub fn clear_all(&self) {
        self.items.write().clear();
    }

    pub fn export_json(&self) -> String {
        serde_json::to_string(&*self.items.read()).unwrap_or_else(|_| "[]".to_string())
    }

    pub fn import_json(&self, json_str: &str) -> usize {
        if json_str.trim().is_empty() {
            return 0;
        }

        let imported: Vec<BookmarkItem> = match serde_json::from_str(json_str) {
            Ok(v) => v,
            Err(_) => return 0,
        };

        let count = imported.len();
        let mut list = self.items.write();
        *list = imported;
        count
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_bookmark_manager() {
        let manager = BookmarkManager::new();

        // 1. Create folder
        let folder = manager.create_folder("Tech", None);
        assert!(folder.is_folder);

        // 2. Add bookmarks
        let b1 = manager.add_bookmark("Azokle", "https://azokle.com", Some(&folder.id), None);
        let b2 = manager.add_bookmark("Rust", "https://rust-lang.org", None, None);

        assert_eq!(manager.get_all().len(), 3);
        assert!(manager.is_bookmarked("https://azokle.com"));
        assert!(!manager.is_bookmarked("https://google.com"));

        // 3. Search
        let results = manager.search("rust");
        assert_eq!(results.len(), 1);
        assert_eq!(results[0].id, b2.id);

        // 4. Update
        assert!(manager.update_item(&b1.id, Some("Azokle Sovereign"), None, None, None));
        let updated = manager.get_all();
        assert_eq!(
            updated.iter().find(|b| b.id == b1.id).unwrap().title,
            "Azokle Sovereign"
        );

        // 5. Remove folder deletes children
        assert!(manager.remove_item(&folder.id));
        assert!(!manager.is_bookmarked("https://azokle.com"));
        assert_eq!(manager.get_all().len(), 1);
    }
}
