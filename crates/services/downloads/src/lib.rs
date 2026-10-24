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
use url::Url;

/// Operational lifecycle state of an active or historical file download.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum DownloadState {
    Pending,
    Downloading,
    Paused,
    Completed,
    Cancelled,
    Failed,
}

impl DownloadState {
    /// Canonical uppercase status string representation.
    pub fn to_str(&self) -> &'static str {
        match self {
            Self::Pending => "PENDING",
            Self::Downloading => "DOWNLOADING",
            Self::Paused => "PAUSED",
            Self::Completed => "COMPLETED",
            Self::Cancelled => "CANCELLED",
            Self::Failed => "FAILED",
        }
    }

    /// Parses a status string name into a [`DownloadState`].
    pub fn from_str_name(name: &str) -> Self {
        match name.to_uppercase().as_str() {
            "DOWNLOADING" | "IN_PROGRESS" => Self::Downloading,
            "PAUSED" => Self::Paused,
            "COMPLETED" | "SUCCESS" => Self::Completed,
            "CANCELLED" | "CANCELED" => Self::Cancelled,
            "FAILED" | "ERROR" => Self::Failed,
            _ => Self::Pending,
        }
    }
}

/// Metadata describing a file download transfer.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DownloadItem {
    /// Unique download task identifier formatted as `dl_{timestamp}_{index}`.
    pub id: String,
    /// Source URL being downloaded.
    pub url: String,
    /// Sanitized target file name.
    pub filename: String,
    /// Target filesystem destination directory or file path.
    pub destination_path: String,
    /// Expected total content size in bytes (`0` if chunked/unknown).
    pub total_bytes: u64,
    /// Number of bytes successfully received and flushed to disk.
    pub downloaded_bytes: u64,
    /// Current transfer state.
    pub state: DownloadState,
    /// Inferred or server-declared MIME content-type.
    pub mime_type: Option<String>,
    /// Millisecond Unix timestamp when the download was triggered.
    pub started_at: u64,
    /// Millisecond Unix timestamp when transfer reached a terminal state.
    pub completed_at: Option<u64>,
}

/// Thread-safe manager coordinating background file downloads and progress tracking.
#[derive(Clone)]
pub struct DownloadManager {
    items: Arc<RwLock<Vec<DownloadItem>>>,
}

impl Default for DownloadManager {
    /// Returns the default instance.
    fn default() -> Self {
        Self::new()
    }
}

impl DownloadManager {
    /// Creates an empty download manager.
    pub fn new() -> Self {
        Self {
            items: Arc::new(RwLock::new(Vec::new())),
        }
    }

    /// Sanitizes an untrusted filename to prevent path traversal (`../`) and illegal character injection.
    ///
    /// Replaces `..` with `_`, limits characters to alphanumeric and safe punctuation (`.`, `-`, `_`),
    /// strips leading periods to prevent hidden file creation, and constrains length to 128 characters.
    pub fn sanitize_filename(name: &str) -> String {
        let clean = name.trim().replace("..", "_");
        let sanitized: String = clean
            .chars()
            .map(|c| {
                if c.is_alphanumeric() || c == '.' || c == '-' || c == '_' {
                    c
                } else {
                    '_'
                }
            })
            .collect();
        let result = sanitized.trim_start_matches('.').trim();
        if result.is_empty() {
            format!(
                "download_{}",
                std::time::SystemTime::now()
                    .duration_since(std::time::UNIX_EPOCH)
                    .map(|d| d.as_secs())
                    .unwrap_or(0)
            )
        } else {
            result.chars().take(128).collect()
        }
    }

    /// Extracts or infers a filename from URL path, Content-Disposition, or MIME type.
    pub fn extract_filename(
        url_str: &str,
        content_disposition: Option<&str>,
        mime_type: Option<&str>,
    ) -> String {
        if let Some(cd) = content_disposition {
            if let Some(pos) = cd.to_lowercase().find("filename=") {
                let name_part = &cd[pos + 9..];
                let clean_name =
                    name_part.trim_matches(|c| c == '"' || c == '\'' || c == ' ' || c == ';');
                if !clean_name.is_empty() {
                    return Self::sanitize_filename(clean_name);
                }
            }
        }

        if let Ok(parsed) = Url::parse(url_str) {
            if let Some(mut segments) = parsed.path_segments() {
                if let Some(last) = segments.next_back() {
                    let clean = last.trim();
                    if clean.contains('.') && !clean.ends_with('.') {
                        return Self::sanitize_filename(clean);
                    }
                }
            }
        }

        let ext = match mime_type.unwrap_or("").to_lowercase().as_str() {
            "image/jpeg" | "image/jpg" => "jpg",
            "image/png" => "png",
            "image/webp" => "webp",
            "image/gif" => "gif",
            "video/mp4" => "mp4",
            "video/webm" => "webm",
            "application/pdf" => "pdf",
            "application/zip" => "zip",
            "application/json" => "json",
            "text/html" => "html",
            "text/plain" => "txt",
            _ => "bin",
        };

        let now_sec = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .map(|d| d.as_secs())
            .unwrap_or(0);

        format!("download_{}.{}", now_sec, ext)
    }

    /// Checks whether a URL is safe for download creation and storage.
    /// Strictly rejects dangerous URI schemes (javascript:, file:, intent:, content:, chrome:, etc.)
    /// and control characters.
    pub fn is_safe_download_url(url_str: &str) -> bool {
        let trimmed = url_str.trim();
        if trimmed.is_empty()
            || trimmed.contains('\r')
            || trimmed.contains('\n')
            || trimmed.contains('\0')
        {
            return false;
        }
        let lower = trimmed.to_lowercase();
        if lower.starts_with("javascript:")
            || lower.starts_with("file:")
            || lower.starts_with("intent:")
            || lower.starts_with("content:")
            || lower.starts_with("about:")
            || lower.starts_with("chrome:")
            || lower.starts_with("resource:")
            || lower.starts_with("jar:")
        {
            return false;
        }
        lower.starts_with("http://")
            || lower.starts_with("https://")
            || lower.starts_with("blob:")
            || lower.starts_with("data:")
    }

    /// Performs create download operation.
    pub fn create_download(
        &self,
        url_str: &str,
        filename: Option<&str>,
        destination_path: &str,
        total_bytes: u64,
        mime_type: Option<&str>,
    ) -> Option<DownloadItem> {
        if !Self::is_safe_download_url(url_str) {
            return None;
        }

        let clean_dest = destination_path.trim();
        if clean_dest.is_empty()
            || clean_dest.contains('\0')
            || clean_dest.contains("../")
            || clean_dest.contains("..\\")
        {
            return None;
        }

        let now_ms = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .map(|d| d.as_millis() as u64)
            .unwrap_or(0);

        let safe_name = match filename {
            Some(name) if !name.trim().is_empty() => Self::sanitize_filename(name),
            _ => Self::extract_filename(url_str, None, mime_type),
        };

        let id = format!("dl_{}_{}", now_ms, self.items.read().len());
        let item = DownloadItem {
            id,
            url: url_str.to_string(),
            filename: safe_name,
            destination_path: clean_dest.to_string(),
            total_bytes,
            downloaded_bytes: 0,
            state: DownloadState::Downloading,
            mime_type: mime_type.map(|s| s.to_string()),
            started_at: now_ms,
            completed_at: None,
        };

        self.items.write().insert(0, item.clone());
        Some(item)
    }

    /// Updates the progress.
    pub fn update_progress(
        &self,
        id: &str,
        downloaded_bytes: u64,
        total_bytes: u64,
        state: DownloadState,
    ) -> bool {
        let mut list = self.items.write();
        if let Some(item) = list.iter_mut().find(|d| d.id == id) {
            item.downloaded_bytes = downloaded_bytes;
            if total_bytes > 0 {
                item.total_bytes = total_bytes;
            }
            item.state = state;
            if state == DownloadState::Completed && item.completed_at.is_none() {
                item.completed_at = Some(
                    std::time::SystemTime::now()
                        .duration_since(std::time::UNIX_EPOCH)
                        .map(|d| d.as_millis() as u64)
                        .unwrap_or(0),
                );
            }
            true
        } else {
            false
        }
    }

    /// Returns the item.
    pub fn get_item(&self, id: &str) -> Option<DownloadItem> {
        self.items.read().iter().find(|d| d.id == id).cloned()
    }

    /// Returns the all.
    pub fn get_all(&self) -> Vec<DownloadItem> {
        self.items.read().clone()
    }

    /// Removes the specified item.
    pub fn delete_item(&self, id: &str) -> bool {
        let mut list = self.items.write();
        if let Some(pos) = list.iter().position(|d| d.id == id) {
            list.remove(pos);
            true
        } else {
            false
        }
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

        let imported: Vec<DownloadItem> = match serde_json::from_str(json_str) {
            Ok(v) => v,
            Err(_) => return 0,
        };

        let mut sanitized = Vec::with_capacity(imported.len());
        for mut item in imported {
            if !Self::is_safe_download_url(&item.url) {
                continue;
            }
            item.filename = Self::sanitize_filename(&item.filename);
            let dest = item.destination_path.trim();
            if dest.is_empty()
                || dest.contains('\0')
                || dest.contains("../")
                || dest.contains("..\\")
            {
                continue;
            }
            item.destination_path = dest.to_string();
            sanitized.push(item);
        }

        let count = sanitized.len();
        let mut list = self.items.write();
        *list = sanitized;
        count
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_download_sanitization_and_extraction() {
        assert_eq!(
            DownloadManager::sanitize_filename("image ../test.png"),
            "image___test.png"
        );

        assert_eq!(
            DownloadManager::extract_filename("https://example.com/files/document.pdf", None, None),
            "document.pdf"
        );

        assert_eq!(
            DownloadManager::extract_filename(
                "https://example.com/api/download",
                Some("attachment; filename=\"report.csv\""),
                None
            ),
            "report.csv"
        );
    }

    #[test]
    fn test_download_lifecycle() {
        let manager = DownloadManager::new();
        let item = manager
            .create_download(
                "https://example.com/file.zip",
                Some("file.zip"),
                "/downloads/file.zip",
                2048,
                Some("application/zip"),
            )
            .expect("Valid download should be created");
        assert_eq!(item.state, DownloadState::Downloading);

        // Progress update
        assert!(manager.update_progress(&item.id, 1024, 2048, DownloadState::Downloading));
        let active = manager.get_item(&item.id).unwrap();
        assert_eq!(active.downloaded_bytes, 1024);

        // Completion
        assert!(manager.update_progress(&item.id, 2048, 2048, DownloadState::Completed));
        let finished = manager.get_item(&item.id).unwrap();
        assert_eq!(finished.state, DownloadState::Completed);
        assert!(finished.completed_at.is_some());

        // Delete
        assert!(manager.delete_item(&item.id));
        assert_eq!(manager.get_all().len(), 0);
    }

    #[test]
    fn test_download_security_url_and_path_sanitization() {
        let manager = DownloadManager::new();

        // Reject dangerous schemes
        assert!(!DownloadManager::is_safe_download_url(
            "javascript:alert(1)"
        ));
        assert!(!DownloadManager::is_safe_download_url("file:///etc/passwd"));
        assert!(!DownloadManager::is_safe_download_url(
            "intent://scan/#Intent;scheme=zxing;end"
        ));
        assert!(!DownloadManager::is_safe_download_url(
            "content://media/external/images"
        ));
        assert!(!DownloadManager::is_safe_download_url("chrome://settings"));
        assert!(!DownloadManager::is_safe_download_url("about:config"));

        // Accept safe schemes
        assert!(DownloadManager::is_safe_download_url(
            "https://example.com/file.zip"
        ));
        assert!(DownloadManager::is_safe_download_url(
            "http://example.com/image.png"
        ));
        assert!(DownloadManager::is_safe_download_url(
            "blob:https://example.com/uuid"
        ));
        assert!(DownloadManager::is_safe_download_url(
            "data:image/png;base64,iVBORw0KGgo="
        ));

        // Rejections in create_download
        assert!(manager
            .create_download("javascript:alert(1)", None, "/downloads/a.txt", 100, None)
            .is_none());
        assert!(manager
            .create_download(
                "https://example.com/a.txt",
                None,
                "/downloads/../../etc/passwd",
                100,
                None
            )
            .is_none());

        // Import JSON sanitization
        let malicious_json = r#"[
            {"id":"1","url":"javascript:malicious()","filename":"mal.js","destination_path":"/safe/path","total_bytes":10,"downloaded_bytes":0,"state":"Completed","started_at":0},
            {"id":"2","url":"https://example.com/safe.pdf","filename":"../../traversal.pdf","destination_path":"/safe/path/safe.pdf","total_bytes":100,"downloaded_bytes":100,"state":"Completed","started_at":0},
            {"id":"3","url":"https://example.com/evil.sh","filename":"evil.sh","destination_path":"/downloads/../../bin/evil.sh","total_bytes":50,"downloaded_bytes":50,"state":"Completed","started_at":0}
        ]"#;

        let imported_count = manager.import_json(malicious_json);
        assert_eq!(
            imported_count, 1,
            "Only safe entries without path traversal should be imported"
        );
        let all = manager.get_all();
        assert_eq!(all.len(), 1);
        assert_eq!(all[0].url, "https://example.com/safe.pdf");
        assert_eq!(all[0].filename, "____traversal.pdf");
    }
}
