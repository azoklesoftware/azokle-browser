use crate::domain::{Tab, TabId};
use crate::ports::StoragePort;
use std::sync::Arc;

/// Enforces tab lifecycle rules and sovereignty persistence invariants.
///
/// Ensures strict separation between normal browsing sessions (which can be persisted
/// to local storage) and private/Tor browsing tabs (which must remain purely ephemeral in RAM).
pub struct TabUseCase {
    storage_port: Option<Arc<dyn StoragePort>>,
}

impl TabUseCase {
    /// Constructs a TabUseCase optionally linked to a persistent storage port.
    pub fn new(storage_port: Option<Arc<dyn StoragePort>>) -> Self {
        Self { storage_port }
    }

    /// Initializes a new tab session while strictly enforcing disk-isolation guarantees.
    ///
    /// If `is_private` or `is_tor` is true, the tab is kept exclusively in volatile memory
    /// and will never be dispatched to the `StoragePort`.
    pub fn create_tab(
        &self,
        id: TabId,
        url: String,
        is_private: bool,
        is_tor: bool,
        now_ms: u64,
    ) -> Tab {
        let tab = Tab::new(id, url, is_private, is_tor, now_ms);
        // Invariant: Private or Tor tabs are strictly never persisted to storage
        if !is_private && !is_tor {
            if let Some(storage) = &self.storage_port {
                let _ = storage.save_tab(&tab);
            }
        }
        tab
    }

    /// Closes a tab session and cleans up its persistent state if it was a persistent tab.
    pub fn close_tab(&self, tab: &Tab) {
        if !tab.is_private && !tab.is_tor {
            if let Some(storage) = &self.storage_port {
                let _ = storage.delete_tab(tab.id.0);
            }
        }
    }

    /// Transitions an active tab into suspended state to reclaim memory while preserving its session URL.
    pub fn suspend_tab(&self, tab: &mut Tab) {
        tab.suspend();
    }
}
