pub mod category;
pub mod matcher;
pub mod rules;

pub use category::BlockCategory;
pub use matcher::{FilterDecision, RuleMatcher};
pub use rules::NetworkRule;

use parking_lot::RwLock;
use std::sync::Arc;

#[derive(Clone)]
pub struct AdBlockEngine {
    matcher: Arc<RwLock<RuleMatcher>>,
}

impl Default for AdBlockEngine {
    fn default() -> Self {
        Self::new()
    }
}

impl AdBlockEngine {
    pub fn new() -> Self {
        Self {
            matcher: Arc::new(RwLock::new(RuleMatcher::new())),
        }
    }

    pub fn load_rules_from_text(&self, content: &str, category: BlockCategory) -> usize {
        let mut matcher = self.matcher.write();
        let mut count = 0;
        for line in content.lines() {
            if let Some(rule) = NetworkRule::parse(line, category) {
                matcher.add_rule(rule);
                count += 1;
            }
        }
        matcher.rebuild_indexes();
        count
    }

    pub fn check_url(&self, url: &str, source_origin: Option<&str>, is_third_party: bool) -> FilterDecision {
        self.matcher.read().check_url(url, source_origin, is_third_party)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_adblock_domain_suffix_and_exceptions() {
        let engine = AdBlockEngine::new();
        let rules = r#"
            ||doubleclick.net^$third-party
            ||googleadservices.com^
            @@||exception.googleadservices.com^
            @@||googlevideo.com^
            /pagead/
        "#;
        engine.load_rules_from_text(rules, BlockCategory::Ad);

        // 1. Should block ad domain (including trailing dot FQDN) when third-party
        let res1 = engine.check_url("https://ad.doubleclick.net/ad.js", Some("https://example.com"), true);
        assert!(res1.should_block);
        assert_eq!(res1.category, BlockCategory::Ad);

        let res1_dot = engine.check_url("https://ad.doubleclick.net./ad.js", Some("https://example.com"), true);
        assert!(res1_dot.should_block);

        // 1b. Third-party rule should NOT block first-party requests
        let res1_first_party = engine.check_url("https://ad.doubleclick.net/ad.js", Some("https://doubleclick.net"), false);
        assert!(!res1_first_party.should_block);

        // 2. Should block googleadservices
        let res2 = engine.check_url("https://googleadservices.com/pagead/123", None, true);
        assert!(res2.should_block);

        // 3. Should allow exception domain
        let res3 = engine.check_url("https://exception.googleadservices.com/ok", None, true);
        assert!(!res3.should_block);
        assert!(res3.is_exception);

        // 4. Sovereign Azokle domain protection
        let res4 = engine.check_url("https://search.azokle.com/pagead/test", None, false);
        assert!(!res4.should_block);
        assert!(res4.is_exception);

        // 5. Declarative video streaming CDN rule exemption
        let res5 = engine.check_url("https://rr1---sn-4g5ednle.googlevideo.com/videoplayback?id=123", Some("https://youtube.com"), true);
        assert!(!res5.should_block);
        assert!(res5.is_exception);
    }
}
