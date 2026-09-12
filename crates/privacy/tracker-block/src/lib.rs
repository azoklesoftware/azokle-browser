pub mod debounce;
pub mod query_stripper;
pub mod tracker_db;

pub use debounce::DebounceEngine;
pub use query_stripper::QueryStripper;
pub use tracker_db::TrackerDb;

use parking_lot::RwLock;
use std::sync::Arc;

#[derive(Clone)]
pub struct TrackerEngine {
    db: Arc<RwLock<TrackerDb>>,
    stripper: Arc<QueryStripper>,
    debounce: Arc<RwLock<DebounceEngine>>,
}

impl Default for TrackerEngine {
    fn default() -> Self {
        Self::new()
    }
}

impl TrackerEngine {
    pub fn new() -> Self {
        Self {
            db: Arc::new(RwLock::new(TrackerDb::new())),
            stripper: Arc::new(QueryStripper::new()),
            debounce: Arc::new(RwLock::new(DebounceEngine::new())),
        }
    }

    pub fn load_tracker_list(&self, content: &str) -> usize {
        self.db.write().load_from_text(content)
    }

    pub fn load_debounce_rules(&self, json_str: &str) -> Result<usize, String> {
        self.debounce.write().load_from_json(json_str)
    }

    pub fn is_tracker(&self, host: &str) -> bool {
        self.db.read().is_tracker_domain(host)
    }

    pub fn uncloak_host(&self, host: &str) -> String {
        self.db.read().uncloak_host(host).to_string()
    }

    pub fn add_cname_mapping(&self, cloaked_host: &str, uncloaked_host: &str) {
        self.db.write().add_cname_mapping(cloaked_host, uncloaked_host);
    }

    pub fn strip_tracking_params(&self, url: &str) -> String {
        self.stripper.strip_tracking_params(url)
    }

    pub fn unwrap_bounce_url(&self, url: &str) -> Option<String> {
        self.debounce.read().unwrap_bounce_url(url)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_tracker_detection_and_query_stripping() {
        let engine = TrackerEngine::new();
        engine.load_tracker_list("||segment.io^\n||analytics.tiktok.com^");

        assert!(engine.is_tracker("cdn.segment.io"));
        assert!(engine.is_tracker("analytics.tiktok.com"));
        assert!(!engine.is_tracker("news.ycombinator.com"));

        // CNAME uncloaking detection
        assert_eq!(engine.uncloak_host("cname.segment.com"), "segment.com");
        assert!(engine.is_tracker("cname.segment.com"));

        engine.add_cname_mapping("metrics.example.com", "segment.io");
        assert_eq!(engine.uncloak_host("metrics.example.com"), "segment.io");
        assert!(engine.is_tracker("metrics.example.com"));

        let dirty_url = "https://example.com/shop?product=laptop&utm_source=newsletter&fbclid=IwAR123&utm_medium=email&gclid=Cj0K123";
        let clean_url = engine.strip_tracking_params(dirty_url);
        assert_eq!(clean_url, "https://example.com/shop?product=laptop");

        let simple_clean = "https://example.com/about?section=team";
        assert_eq!(engine.strip_tracking_params(simple_clean), simple_clean);

        // Test debounce bounce tracking bypass
        let debounce_json = r#"[
            {
                "include": ["*://cc.zdnet.com/v1/otc/*"],
                "action": "redirect",
                "param": "url"
            }
        ]"#;
        engine.load_debounce_rules(debounce_json).unwrap();
        let bounce = "https://cc.zdnet.com/v1/otc/click?url=https%3A%2F%2Fstore.com%2Fitem";
        assert_eq!(engine.unwrap_bounce_url(bounce), Some("https://store.com/item".to_string()));
    }
}
