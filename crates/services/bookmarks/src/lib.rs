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
    /// Returns the default instance.
    fn default() -> Self {
        Self::new()
    }
}

/// Sanitizes bookmark URLs against dangerous schemes, non-standard schemes, and control characters.
/// Strictly permits only http, https, and internal safe schemes (azokle, about:blank, about:tor).
fn sanitize_bookmark_url(url: &str) -> String {
    let clean: String = url
        .chars()
        .filter(|c| !c.is_control() && *c != '\u{FEFF}' && *c != '\u{200B}')
        .collect();
    let trimmed = clean.trim();
    if trimmed.is_empty() {
        return "about:blank".to_string();
    }

    let lower = trimmed.to_ascii_lowercase();
    if lower == "about:blank" || lower == "about:tor" {
        return lower;
    }

    if lower.starts_with("javascript:")
        || lower.starts_with("file:")
        || lower.starts_with("content:")
        || lower.starts_with("chrome:")
        || lower.starts_with("resource:")
        || lower.starts_with("view-source:")
        || lower.starts_with("data:")
        || lower.starts_with("blob:")
        || lower.starts_with("jar:")
        || lower.starts_with("intent:")
        || lower.starts_with("about:")
    {
        return "about:blank".to_string();
    }

    if lower.starts_with("http://") || lower.starts_with("https://") || lower.starts_with("azokle://") {
        trimmed.to_string()
    } else {
        format!("https://{}", trimmed)
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
        let safe_url = sanitize_bookmark_url(url_str);
        let safe_title: String = if title.trim().is_empty() {
            safe_url.clone()
        } else {
            let t: String = title
                .trim()
                .chars()
                .filter(|c| !c.is_control() || *c == ' ')
                .take(256)
                .collect();
            if t.is_empty() {
                safe_url.clone()
            } else {
                t
            }
        };
        let item = BookmarkItem {
            id,
            title: safe_title,
            url: safe_url,
            parent_folder_id: parent_folder_id.map(|s| s.to_string()),
            date_added: now_ms,
            favicon_url: favicon_url.map(|s| s.to_string()),
            is_folder: false,
        };

        self.items.write().push(item.clone());
        item
    }

    /// Performs create folder operation.
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

    /// Removes the specified item.
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

    /// Updates the item.
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
                let clean_t: String = t
                    .trim()
                    .chars()
                    .filter(|c| !c.is_control() || *c == ' ')
                    .take(256)
                    .collect();
                if !clean_t.is_empty() {
                    item.title = clean_t;
                }
            }
            if let Some(u) = url_str {
                item.url = if item.is_folder {
                    String::new()
                } else {
                    sanitize_bookmark_url(u)
                };
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

    /// Performs search operation.
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

    /// Checks whether bookmarked is satisfied.
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

    /// Returns the all.
    pub fn get_all(&self) -> Vec<BookmarkItem> {
        self.items.read().clone()
    }

    /// Returns the folder items.
    pub fn get_folder_items(&self, folder_id: Option<&str>) -> Vec<BookmarkItem> {
        self.items
            .read()
            .iter()
            .filter(|b| b.parent_folder_id.as_deref() == folder_id)
            .cloned()
            .collect()
    }

    /// Performs clear all operation.
    pub fn clear_all(&self) {
        self.items.write().clear();
    }

    /// Performs export json operation.
    pub fn export_json(&self) -> String {
        serde_json::to_string(&*self.items.read()).unwrap_or_else(|_| "[]".to_string())
    }

    /// Performs import json operation.
    pub fn import_json(&self, json_str: &str) -> usize {
        if json_str.trim().is_empty() {
            return 0;
        }

        let imported: Vec<BookmarkItem> = match serde_json::from_str(json_str) {
            Ok(v) => v,
            Err(_) => return 0,
        };

        let valid_entries: Vec<BookmarkItem> = imported
            .into_iter()
            .map(|mut b| {
                if !b.is_folder {
                    b.url = sanitize_bookmark_url(&b.url);
                }
                let clean_t: String = b
                    .title
                    .trim()
                    .chars()
                    .filter(|c| !c.is_control() || *c == ' ')
                    .take(256)
                    .collect();
                b.title = if clean_t.is_empty() {
                    if b.is_folder { "Folder".to_string() } else { b.url.clone() }
                } else {
                    clean_t
                };
                b
            })
            .collect();

        let count = valid_entries.len();
        let mut list = self.items.write();
        *list = valid_entries;
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

    #[test]
    fn test_bookmark_scheme_sanitization() {
        let manager = BookmarkManager::new();

        // Dangerous schemes must be sanitized to about:blank
        let b_js = manager.add_bookmark("XSS", "javascript:alert(1)", None, None);
        assert_eq!(b_js.url, "about:blank");

        let b_file = manager.add_bookmark("File", "file:///etc/passwd", None, None);
        assert_eq!(b_file.url, "about:blank");

        let b_content = manager.add_bookmark("Content", "content://media/external", None, None);
        assert_eq!(b_content.url, "about:blank");

        let b_source = manager.add_bookmark("Source", "view-source:https://azokle.com", None, None);
        assert_eq!(b_source.url, "about:blank");

        // Legitimate schemes are preserved
        let b_valid = manager.add_bookmark("Azokle", "https://azokle.com", None, None);
        assert_eq!(b_valid.url, "https://azokle.com");

        // Updating with dangerous scheme must also sanitize
        assert!(manager.update_item(&b_valid.id, None, Some("javascript:alert(2)"), None, None));
        let updated = manager.get_all();
        let target = updated.iter().find(|b| b.id == b_valid.id).unwrap();
        assert_eq!(target.url, "about:blank");
    }
}
