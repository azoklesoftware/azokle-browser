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
            .map(|c| if c.is_alphanumeric() || c == '.' || c == '-' || c == '_' { c } else { '_' })
            .collect();
        let result = sanitized.trim_start_matches('.').trim();
        if result.is_empty() {
            format!("download_{}", std::time::SystemTime::now().duration_since(std::time::UNIX_EPOCH).map(|d| d.as_secs()).unwrap_or(0))
        } else {
            result.chars().take(128).collect()
        }
    }

    /// Extracts or infers a filename from URL path, Content-Disposition, or MIME type.
    pub fn extract_filename(url_str: &str, content_disposition: Option<&str>, mime_type: Option<&str>) -> String {
        if let Some(cd) = content_disposition {
            if let Some(pos) = cd.to_lowercase().find("filename=") {
                let name_part = &cd[pos + 9..];
                let clean_name = name_part.trim_matches(|c| c == '"' || c == '\'' || c == ' ' || c == ';');
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

    pub fn create_download(
        &self,
        url_str: &str,
        filename: Option<&str>,
        destination_path: &str,
        total_bytes: u64,
        mime_type: Option<&str>,
    ) -> DownloadItem {
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
            destination_path: destination_path.to_string(),
            total_bytes,
            downloaded_bytes: 0,
            state: DownloadState::Downloading,
            mime_type: mime_type.map(|s| s.to_string()),
            started_at: now_ms,
            completed_at: None,
        };

        self.items.write().insert(0, item.clone());
        item
    }

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

    pub fn get_item(&self, id: &str) -> Option<DownloadItem> {
        self.items.read().iter().find(|d| d.id == id).cloned()
    }

    pub fn get_all(&self) -> Vec<DownloadItem> {
        self.items.read().clone()
    }

    pub fn delete_item(&self, id: &str) -> bool {
        let mut list = self.items.write();
        if let Some(pos) = list.iter().position(|d| d.id == id) {
            list.remove(pos);
            true
        } else {
            false
        }
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

        let imported: Vec<DownloadItem> = match serde_json::from_str(json_str) {
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
            DownloadManager::extract_filename("https://example.com/api/download", Some("attachment; filename=\"report.csv\""), None),
            "report.csv"
        );
    }

    #[test]
    fn test_download_lifecycle() {
        let manager = DownloadManager::new();
        let item = manager.create_download("https://example.com/file.zip", Some("file.zip"), "/downloads/file.zip", 2048, Some("application/zip"));
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
}
