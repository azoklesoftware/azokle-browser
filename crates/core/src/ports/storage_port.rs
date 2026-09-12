use crate::domain::Tab;

pub trait StoragePort: Send + Sync {
    fn save_tab(&self, tab: &Tab) -> Result<(), String>;
    fn delete_tab(&self, tab_id: u64) -> Result<(), String>;
    fn load_all_tabs(&self) -> Result<Vec<Tab>, String>;
}
