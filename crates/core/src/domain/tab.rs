use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub struct TabId(pub u64);

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum TabState {
    Active,
    Suspended,
    Closed,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Tab {
    pub id: TabId,
    pub url: String,
    pub title: String,
    pub is_private: bool,
    pub is_tor: bool,
    pub state: TabState,
    pub created_at_ms: u64,
    pub last_accessed_ms: u64,
}

impl Tab {
    pub fn new(id: TabId, url: String, is_private: bool, is_tor: bool, now_ms: u64) -> Self {
        Self {
            id,
            url: url.clone(),
            title: String::new(),
            is_private,
            is_tor,
            state: TabState::Active,
            created_at_ms: now_ms,
            last_accessed_ms: now_ms,
        }
    }

    pub fn suspend(&mut self) {
        if self.state == TabState::Active {
            self.state = TabState::Suspended;
        }
    }

    pub fn activate(&mut self, now_ms: u64) {
        self.state = TabState::Active;
        self.last_accessed_ms = now_ms;
    }
}
