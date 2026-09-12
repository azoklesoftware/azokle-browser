use crate::domain::TabId;

pub trait EnginePort: Send + Sync {
    fn load_uri(&self, tab_id: TabId, uri: &str) -> Result<(), String>;
    fn close_session(&self, tab_id: TabId) -> Result<(), String>;
    fn reload(&self, tab_id: TabId, bypass_cache: bool) -> Result<(), String>;
}
