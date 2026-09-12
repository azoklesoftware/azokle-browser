pub mod category;
pub mod matcher;
pub mod rules;

pub use category::BlockCategory;
pub use matcher::{FilterDecision, RuleMatcher};
pub use rules::NetworkRule;

use parking_lot::RwLock;
use std::sync::Arc;
use azokle_common::SovereignWhitelist;

fn rebuild_brave_engine(
    lists: &[String],
    resources: &[adblock::resources::Resource],
) -> adblock::Engine {
    let mut filter_set = adblock::lists::FilterSet::new(true);
    for list in lists {
        filter_set.add_filter_list(list.clone(), adblock::lists::ParseOptions::default());
    }
    let mut engine = adblock::Engine::new_with_filter_set(filter_set);
    if !resources.is_empty() {
        engine.use_resources(resources.iter().cloned());
    }
    engine
}

fn infer_request_type(url: &str) -> &'static str {
    let lower = url.to_ascii_lowercase();
    let path = lower.split('?').next().unwrap_or(&lower);
    if path.ends_with(".js") || path.ends_with(".mjs") {
        "script"
    } else if path.ends_with(".png")
        || path.ends_with(".jpg")
        || path.ends_with(".jpeg")
        || path.ends_with(".gif")
        || path.ends_with(".webp")
        || path.ends_with(".svg")
        || path.ends_with(".ico")
    {
        "image"
    } else if path.ends_with(".css") {
        "stylesheet"
    } else if path.ends_with(".woff") || path.ends_with(".woff2") || path.ends_with(".ttf") || path.ends_with(".otf") {
        "font"
    } else if path.ends_with(".mp4") || path.ends_with(".webm") || path.ends_with(".m3u8") || path.ends_with(".ts") {
        "media"
    } else if lower.contains("/beacon") || lower.contains("/ping") || lower.contains("/collect") {
        "ping"
    } else {
        "other"
    }
}

#[derive(Clone)]
pub struct AdBlockEngine {
    engine: Arc<RwLock<adblock::Engine>>,
    lists: Arc<RwLock<Vec<String>>>,
    resources: Arc<RwLock<Vec<adblock::resources::Resource>>>,
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
            engine: Arc::new(RwLock::new(adblock::Engine::default())),
            lists: Arc::new(RwLock::new(Vec::new())),
            resources: Arc::new(RwLock::new(Vec::new())),
            matcher: Arc::new(RwLock::new(RuleMatcher::new())),
        }
    }

    /// Loads filter rules from text lines, updating both Brave's native Flatbuffer engine
    /// and the category-aware fallback matcher.
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

        let mut lists = self.lists.write();
        lists.push(content.to_string());
        let res = self.resources.read();
        let new_engine = rebuild_brave_engine(&lists, &res);
        *self.engine.write() = new_engine;

        count
    }

    /// Loads multiple filter lists in bulk for maximum initialization efficiency.
    pub fn load_filter_lists(&self, list_contents: Vec<String>) -> usize {
        let mut count = 0;
        {
            let mut matcher = self.matcher.write();
            for content in &list_contents {
                for line in content.lines() {
                    if let Some(rule) = NetworkRule::parse(line, BlockCategory::Ad) {
                        matcher.add_rule(rule);
                        count += 1;
                    }
                }
            }
            matcher.rebuild_indexes();
        }

        {
            let mut lists = self.lists.write();
            for content in list_contents {
                lists.push(content);
            }
            let res = self.resources.read();
            let new_engine = rebuild_brave_engine(&lists, &res);
            *self.engine.write() = new_engine;
        }

        count
    }

    /// Loads surrogate resources and scriptlets (from adblock-resources JSON format).
    pub fn load_resources_json(&self, json_str: &str) -> Result<usize, String> {
        let parsed_resources: Vec<adblock::resources::Resource> =
            serde_json::from_str(json_str).map_err(|e| format!("Failed to parse resources JSON: {e}"))?;
        let count = parsed_resources.len();

        {
            let mut res_lock = self.resources.write();
            *res_lock = parsed_resources;
            let lists = self.lists.read();
            let new_engine = rebuild_brave_engine(&lists, &res_lock);
            *self.engine.write() = new_engine;
        }

        Ok(count)
    }

    /// Loads multiple filter lists and surrogate resources in a single optimized pass.
    pub fn load_filter_lists_bulk(&self, lists: Vec<(String, BlockCategory)>, resources_json: Option<&str>) -> usize {
        let mut count = 0;
        {
            let mut matcher = self.matcher.write();
            for (content, category) in &lists {
                for line in content.lines() {
                    if let Some(rule) = NetworkRule::parse(line, *category) {
                        matcher.add_rule(rule);
                        count += 1;
                    }
                }
            }
            matcher.rebuild_indexes();
        }

        let parsed_resources: Vec<adblock::resources::Resource> = if let Some(res_str) = resources_json {
            serde_json::from_str(res_str).unwrap_or_default()
        } else {
            Vec::new()
        };

        {
            let mut stored_lists = self.lists.write();
            let mut stored_resources = self.resources.write();
            for (content, _) in &lists {
                stored_lists.push(content.clone());
            }
            if !parsed_resources.is_empty() {
                *stored_resources = parsed_resources;
            }
            let new_engine = rebuild_brave_engine(&stored_lists, &stored_resources);
            *self.engine.write() = new_engine;
        }

        count
    }

    /// High-performance network request classification with resource type awareness and category preservation.
    pub fn check_network_request(
        &self,
        url: &str,
        source_origin: Option<&str>,
        request_type: Option<&str>,
        is_third_party: bool,
    ) -> FilterDecision {
        // 1. Sovereign domain check (Azokle, Cloudflare, Captcha services are protected)
        if let Ok(parsed) = url::Url::parse(url) {
            if let Some(host) = parsed.host_str() {
                if SovereignWhitelist::is_sovereign_domain(host) {
                    return FilterDecision {
                        should_block: false,
                        category: BlockCategory::None,
                        matched_rule: None,
                        is_exception: true,
                        redirect_url: None,
                        rewritten_url: None,
                    };
                }
            }
        }

        // Category-aware fallback matcher check (preserves Tracker, Telemetry, Fingerprinting categories)
        let fallback = self.matcher.read().check_url(url, source_origin, is_third_party);

        // 2. Query Brave adblock-rust Engine
        let src = source_origin.unwrap_or("");
        let rtype = request_type.unwrap_or_else(|| infer_request_type(url));
        if let Ok(mut req) = adblock::request::Request::new(url, src, rtype, "get") {
            req.is_third_party = is_third_party;
            let res = self.engine.read().check_network_request(&req);

            if res.should_block() {
                let category = if fallback.category != BlockCategory::None {
                    fallback.category
                } else {
                    BlockCategory::Ad
                };

                return FilterDecision {
                    should_block: true,
                    category,
                    matched_rule: res.filter.as_ref().and_then(|f| f.raw_line.clone()),
                    is_exception: false,
                    redirect_url: res.redirect,
                    rewritten_url: res.rewritten_url,
                };
            } else if res.exception.is_some() {
                return FilterDecision {
                    should_block: false,
                    category: BlockCategory::None,
                    matched_rule: res.exception.as_ref().and_then(|f| f.raw_line.clone()),
                    is_exception: true,
                    redirect_url: None,
                    rewritten_url: res.rewritten_url,
                };
            } else if res.redirect.is_some() || res.rewritten_url.is_some() {
                return FilterDecision {
                    should_block: false,
                    category: BlockCategory::None,
                    matched_rule: res.filter.as_ref().and_then(|f| f.raw_line.clone()),
                    is_exception: false,
                    redirect_url: res.redirect,
                    rewritten_url: res.rewritten_url,
                };
            }
        }

        // 3. Fallback category-specific matcher check
        fallback
    }

    /// High-performance network request classification.
    pub fn check_url(&self, url: &str, source_origin: Option<&str>, is_third_party: bool) -> FilterDecision {
        self.check_network_request(url, source_origin, None, is_third_party)
    }

    /// Queries cosmetic resources (hide selectors, style selectors, injected scriptlets)
    /// for the given URL/origin.
    pub fn url_cosmetic_resources(&self, url: &str) -> adblock::cosmetic_filter_cache::UrlSpecificResources {
        let full_url = if url.starts_with("http://") || url.starts_with("https://") {
            url.to_string()
        } else {
            format!("https://{url}")
        };
        self.engine.read().url_cosmetic_resources(&full_url)
    }

    /// Produces pruned cosmetic CSS stylesheets for the given domain.
    pub fn get_cosmetic_css(&self, domain_or_url: &str) -> String {
        let res = self.url_cosmetic_resources(domain_or_url);
        if res.hide_selectors.is_empty() {
            String::new()
        } else {
            let mut selectors: Vec<_> = res.hide_selectors.into_iter().collect();
            selectors.sort();
            format!("{} {{ display: none !important; }}", selectors.join(", "))
        }
    }

    /// Returns compiled defuser scriptlet JavaScript for the given domain.
    pub fn get_scriptlets(&self, domain_or_url: &str) -> String {
        let res = self.url_cosmetic_resources(domain_or_url);
        res.injected_script
    }

    /// Serializes the compiled Engine into a Flatbuffer binary (.dat) format.
    pub fn serialize_to_dat(&self) -> Vec<u8> {
        self.engine.read().serialize()
    }

    /// Deserializes and restores the Engine from a precompiled Flatbuffer binary (.dat) cache.
    pub fn deserialize_from_dat(&self, data: &[u8]) -> Result<(), String> {
        let mut new_engine = adblock::Engine::default();
        new_engine.deserialize(data).map_err(|e| format!("{e:?}"))?;
        *self.engine.write() = new_engine;
        Ok(())
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

    #[test]
    fn test_adblock_resources_and_cosmetics() {
        let engine = AdBlockEngine::new();
        let rules = r#"
            example.com##.ad-banner
            example.com##+js(set, testAdVar, true)
            ||analytics.example.com^$redirect=noopjs
        "#;
        engine.load_rules_from_text(rules, BlockCategory::Ad);

        let css = engine.get_cosmetic_css("example.com");
        assert!(css.contains(".ad-banner"), "Cosmetic CSS should contain .ad-banner: {css}");

        let res = engine.check_url("https://analytics.example.com/collect", Some("https://example.com"), true);
        assert!(res.should_block || res.redirect_url.is_some());
    }

    #[test]
    fn test_flatbuffer_serialization_cycle() {
        let engine = AdBlockEngine::new();
        let rules = r#"
            ||doubleclick.net^$third-party
            @@||allowed.com^
        "#;
        engine.load_rules_from_text(rules, BlockCategory::Ad);

        let serialized = engine.serialize_to_dat();
        assert!(!serialized.is_empty(), "Serialized dat file should not be empty");

        let restored_engine = AdBlockEngine::new();
        let res = restored_engine.deserialize_from_dat(&serialized);
        assert!(res.is_ok(), "Deserialization should succeed: {:?}", res);

        let check = restored_engine.check_url("https://ad.doubleclick.net/ad.js", Some("https://test.com"), true);
        assert!(check.should_block, "Restored engine should block doubleclick");
    }
}
