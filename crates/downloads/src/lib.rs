use parking_lot::RwLock;
use serde::{Deserialize, Serialize};
use std::sync::Arc;

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum DownloadState {
    Pending,
    Downloading,
    Paused,
    Completed,
    Cancelled,
    Failed,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DownloadItem {
    pub id: String,
    pub url: String,
    pub filename: String,
    pub destination_path: String,
    pub total_bytes: u64,
    pub downloaded_bytes: u64,
    pub state: DownloadState,
}

#[derive(Clone, Default)]
pub struct DownloadManager {
    items: Arc<RwLock<Vec<DownloadItem>>>,
}

impl DownloadManager {
    pub fn new() -> Self {
        Self {
            items: Arc::new(RwLock::new(Vec::new())),
        }
    }

    pub fn start_download(&self, url: &str, filename: &str, destination_path: &str, total_bytes: u64) -> DownloadItem {
        let now = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .map(|d| d.as_secs())
            .unwrap_or(0);
        let id = format!("dl_{}_{}", now, self.items.read().len());

        let item = DownloadItem {
            id,
            url: url.to_string(),
            filename: filename.to_string(),
            destination_path: destination_path.to_string(),
            total_bytes,
            downloaded_bytes: 0,
            state: DownloadState::Downloading,
        };

        self.items.write().push(item.clone());
        item
    }

    pub fn update_progress(&self, id: &str, downloaded_bytes: u64, state: DownloadState) {
        if let Some(item) = self.items.write().iter_mut().find(|d| d.id == id) {
            item.downloaded_bytes = downloaded_bytes;
            item.state = state;
        }
    }

    pub fn get_all(&self) -> Vec<DownloadItem> {
        self.items.read().clone()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_downloads() {
        let manager = DownloadManager::new();
        let item = manager.start_download("https://example.com/file.zip", "file.zip", "/downloads/file.zip", 1024);
        assert_eq!(item.state, DownloadState::Downloading);

        manager.update_progress(&item.id, 1024, DownloadState::Completed);
        let list = manager.get_all();
        assert_eq!(list[0].state, DownloadState::Completed);
        assert_eq!(list[0].downloaded_bytes, 1024);
    }
}
