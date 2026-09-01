pub mod query_stripper;
pub mod tracker_db;

pub use query_stripper::QueryStripper;
pub use tracker_db::TrackerDb;

use parking_lot::RwLock;
use std::sync::Arc;

#[derive(Clone)]
pub struct TrackerEngine {
    db: Arc<RwLock<TrackerDb>>,
    stripper: Arc<QueryStripper>,
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
        }
    }

    pub fn load_tracker_list(&self, content: &str) -> usize {
        self.db.write().load_from_text(content)
    }

    pub fn is_tracker(&self, host: &str) -> bool {
        self.db.read().is_tracker_domain(host)
    }

    pub fn strip_tracking_params(&self, url: &str) -> String {
        self.stripper.strip_tracking_params(url)
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

        let dirty_url = "https://example.com/shop?product=laptop&utm_source=newsletter&fbclid=IwAR123&utm_medium=email&gclid=Cj0K123";
        let clean_url = engine.strip_tracking_params(dirty_url);
        assert_eq!(clean_url, "https://example.com/shop?product=laptop");

        let simple_clean = "https://example.com/about?section=team";
        assert_eq!(engine.strip_tracking_params(simple_clean), simple_clean);
    }
}
