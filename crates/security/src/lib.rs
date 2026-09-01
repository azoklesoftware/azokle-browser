pub mod cosmetic;
pub mod scriptlets;

pub use cosmetic::CosmeticEngine;
pub use scriptlets::ScriptletEngine;

use parking_lot::RwLock;
use std::sync::Arc;

#[derive(Clone)]
pub struct ShieldEngine {
    cosmetic: Arc<RwLock<CosmeticEngine>>,
}

impl Default for ShieldEngine {
    fn default() -> Self {
        Self::new()
    }
}

impl ShieldEngine {
    pub fn new() -> Self {
        Self {
            cosmetic: Arc::new(RwLock::new(CosmeticEngine::new())),
        }
    }

    pub fn load_cosmetic_rules(&self, content: &str) -> usize {
        self.cosmetic.write().load_from_text(content)
    }

    pub fn get_cosmetic_css(&self, domain: &str) -> String {
        self.cosmetic.read().get_css_for_domain(domain)
    }

    pub fn get_scriptlets(&self, domain: &str) -> Option<&'static str> {
        ScriptletEngine::get_defuser_scriptlet(domain)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_cosmetic_rules_and_scriptlets() {
        let engine = ShieldEngine::new();
        engine.load_cosmetic_rules("##.ad-banner\nnytimes.com##.special-sponsored\nnytimes.com#@#.allowed-promo");

        let general_css = engine.get_cosmetic_css("example.com");
        assert!(general_css.contains(".ad-banner"));
        assert!(!general_css.contains(".special-sponsored"));

        let nyt_css = engine.get_cosmetic_css("nytimes.com");
        assert!(nyt_css.contains(".ad-banner"));
        assert!(nyt_css.contains(".special-sponsored"));
        assert!(!nyt_css.contains(".allowed-promo"));

        assert!(engine.get_scriptlets("m.youtube.com").is_some());
        assert!(engine.get_scriptlets("example.com").is_none());
    }
}
