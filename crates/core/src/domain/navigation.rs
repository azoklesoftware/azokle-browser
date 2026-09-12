use super::tab::TabId;
use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct NavigationRequest {
    pub tab_id: TabId,
    pub target_url: String,
    pub source_origin: Option<String>,
    pub request_type: String,
    pub is_user_initiated: bool,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum NavigationOutcome {
    Proceed { final_url: String },
    Blocked { reason: String },
    BypassedBounce { direct_destination: String },
}
