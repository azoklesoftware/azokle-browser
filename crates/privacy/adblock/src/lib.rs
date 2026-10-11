pub mod category;

pub use azokle_common::{FilterDecision, SovereignWhitelist};
pub use category::BlockCategory;

use adblock::lists::{FilterSet, ParseOptions, RuleTypes};
use adblock::resources::Resource;
use parking_lot::RwLock;
use std::sync::Arc;

/// Builds an optimized Brave adblock Engine from filter lists and surrogate resources.
fn rebuild_brave_engine(lists: &[String], resources: &[Resource]) -> adblock::Engine {
    let mut filter_set = FilterSet::new(true);
    for list in lists {
        filter_set.add_filter_list(list.clone(), ParseOptions::default());
    }
    let mut engine = adblock::Engine::new_with_filter_set(filter_set);
    if !resources.is_empty() {
        engine.use_resources(resources.iter().cloned());
    }
    engine
}

/// Infers the Chromium/Gecko request type based on URL path and extension.
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
    } else if path.ends_with(".woff")
        || path.ends_with(".woff2")
        || path.ends_with(".ttf")
        || path.ends_with(".otf")
    {
        "font"
    } else if path.ends_with(".mp4")
        || path.ends_with(".webm")
        || path.ends_with(".m3u8")
        || path.ends_with(".ts")
    {
        "media"
    } else if lower.contains("/beacon") || lower.contains("/ping") || lower.contains("/collect") {
        "ping"
    } else {
        "other"
    }
}

/// Enterprise-grade, FlatBuffers-backed AdBlock & Privacy Engine.
/// Mirrors Brave's mature AdBlockEngineWrapper architecture:
/// - `primary_engine`: Default core rules (EasyList, EasyPrivacy, uBlock, unbreak).
/// - `custom_engine`: User-defined custom rules and dynamic exceptions.
/// - `resources`: uBlock/Brave neutral surrogate scripts and redirects.
#[derive(Clone)]
pub struct AdBlockEngine {
    primary_engine: Arc<RwLock<adblock::Engine>>,
    custom_engine: Arc<RwLock<adblock::Engine>>,
    primary_lists: Arc<RwLock<Vec<String>>>,
    custom_lists: Arc<RwLock<Vec<String>>>,
    resources: Arc<RwLock<Vec<Resource>>>,
}

impl Default for AdBlockEngine {
    fn default() -> Self {
        Self::new()
    }
}

impl AdBlockEngine {
    pub fn new() -> Self {
        Self {
            primary_engine: Arc::new(RwLock::new(adblock::Engine::default())),
            custom_engine: Arc::new(RwLock::new(adblock::Engine::default())),
            primary_lists: Arc::new(RwLock::new(Vec::new())),
            custom_lists: Arc::new(RwLock::new(Vec::new())),
            resources: Arc::new(RwLock::new(Vec::new())),
        }
    }

    /// Loads filter rules from text lines into the primary engine.
    pub fn load_rules_from_text(&self, content: &str, _category: BlockCategory) -> usize {
        let mut lists = self.primary_lists.write();
        lists.push(content.to_string());
        let res = self.resources.read();
        let new_engine = rebuild_brave_engine(&lists, &res);
        *self.primary_engine.write() = new_engine;
        content
            .lines()
            .filter(|l| !l.trim().is_empty() && !l.starts_with('!') && !l.starts_with('#'))
            .count()
    }

    /// Loads multiple filter lists in bulk for maximum initialization efficiency.
    pub fn load_filter_lists(&self, list_contents: Vec<String>) -> usize {
        let mut count = 0;
        let mut lists = self.primary_lists.write();
        for content in list_contents {
            count += content
                .lines()
                .filter(|l| !l.trim().is_empty() && !l.starts_with('!') && !l.starts_with('#'))
                .count();
            lists.push(content);
        }
        let res = self.resources.read();
        let new_engine = rebuild_brave_engine(&lists, &res);
        *self.primary_engine.write() = new_engine;
        count
    }

    /// Loads surrogate resources and scriptlets (from uBlock / Brave resources JSON format).
    pub fn load_resources_json(&self, json_str: &str) -> Result<usize, String> {
        let parsed_resources: Vec<Resource> = serde_json::from_str(json_str)
            .map_err(|e| format!("Failed to parse resources JSON: {e}"))?;
        let count = parsed_resources.len();

        {
            let mut res_lock = self.resources.write();
            *res_lock = parsed_resources;
            let lists = self.primary_lists.read();
            if !lists.is_empty() {
                let new_engine = rebuild_brave_engine(&lists, &res_lock);
                *self.primary_engine.write() = new_engine;
            } else {
                self.primary_engine
                    .write()
                    .use_resources(res_lock.iter().cloned());
            }
        }

        Ok(count)
    }

    /// Loads multiple filter lists and surrogate resources in a single pass.
    pub fn load_filter_lists_bulk(
        &self,
        lists: Vec<(String, BlockCategory)>,
        resources_json: Option<&str>,
    ) -> usize {
        let mut count = 0;
        let parsed_resources: Vec<Resource> = if let Some(res_str) = resources_json {
            serde_json::from_str(res_str).unwrap_or_default()
        } else {
            Vec::new()
        };

        {
            let mut stored_lists = self.primary_lists.write();
            let mut stored_resources = self.resources.write();
            for (content, _) in &lists {
                count += content
                    .lines()
                    .filter(|l| !l.trim().is_empty() && !l.starts_with('!') && !l.starts_with('#'))
                    .count();
                stored_lists.push(content.clone());
            }
            if !parsed_resources.is_empty() {
                *stored_resources = parsed_resources;
            }
            let new_engine = rebuild_brave_engine(&stored_lists, &stored_resources);
            *self.primary_engine.write() = new_engine;
        }

        count
    }

    /// Adds a custom user rule into the isolated custom engine.
    pub fn add_custom_rule(&self, rule: &str) -> bool {
        if rule.trim().is_empty() {
            return false;
        }
        let mut custom_lists = self.custom_lists.write();
        custom_lists.push(rule.to_string());
        let mut filter_set = FilterSet::new(true);
        for list in custom_lists.iter() {
            filter_set.add_filter_list(list.clone(), ParseOptions::default());
        }
        *self.custom_engine.write() = adblock::Engine::new_with_filter_set(filter_set);
        true
    }

    /// Clears all user custom rules.
    pub fn clear_custom_rules(&self) {
        self.custom_lists.write().clear();
        *self.custom_engine.write() = adblock::Engine::default();
    }

    /// High-performance network request classification with resource type awareness and surrogate support.
    pub fn check_network_request(
        &self,
        url: &str,
        source_origin: Option<&str>,
        request_type: Option<&str>,
        is_third_party: bool,
    ) -> FilterDecision {
        // 1. URL normalization and sovereign domain check (Azokle, Cloudflare, Captchas are protected)
        let clean_url = if let Ok(mut parsed) = url::Url::parse(url) {
            let (is_sovereign, needs_trim) = if let Some(host) = parsed.host_str() {
                (
                    SovereignWhitelist::is_sovereign_domain(host),
                    host.ends_with('.'),
                )
            } else {
                (false, false)
            };
            if is_sovereign {
                return FilterDecision {
                    should_block: false,
                    category: BlockCategory::None,
                    matched_rule: None,
                    is_exception: true,
                    redirect_url: None,
                    rewritten_url: None,
                };
            }
            if needs_trim {
                let trimmed = parsed.host_str().unwrap().trim_end_matches('.').to_string();
                let _ = parsed.set_host(Some(&trimmed));
            }
            parsed.to_string()
        } else {
            url.to_string()
        };

        // 2. Query Brave adblock-rust primary Engine
        let src = source_origin.unwrap_or("");
        let rtype = request_type.unwrap_or_else(|| infer_request_type(&clean_url));
        if let Ok(mut req) = adblock::request::Request::new(&clean_url, src, rtype, "get") {
            req.is_third_party = is_third_party;

            // Check primary engine first with force_check_exceptions = true
            let res = self
                .primary_engine
                .read()
                .check_network_request_subset(&req, false, true);

            if res.should_block() {
                return FilterDecision {
                    should_block: true,
                    category: BlockCategory::Ad,
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
                    should_block: true,
                    category: BlockCategory::Ad,
                    matched_rule: res.filter.as_ref().and_then(|f| f.raw_line.clone()),
                    is_exception: false,
                    redirect_url: res.redirect,
                    rewritten_url: res.rewritten_url,
                };
            }

            // Check custom engine if not blocked by primary
            let custom_res = self
                .custom_engine
                .read()
                .check_network_request_subset(&req, false, true);
            if custom_res.should_block() {
                return FilterDecision {
                    should_block: true,
                    category: BlockCategory::Ad,
                    matched_rule: custom_res.filter.as_ref().and_then(|f| f.raw_line.clone()),
                    is_exception: false,
                    redirect_url: custom_res.redirect,
                    rewritten_url: custom_res.rewritten_url,
                };
            } else if custom_res.exception.is_some() {
                return FilterDecision {
                    should_block: false,
                    category: BlockCategory::None,
                    matched_rule: custom_res
                        .exception
                        .as_ref()
                        .and_then(|f| f.raw_line.clone()),
                    is_exception: true,
                    redirect_url: None,
                    rewritten_url: custom_res.rewritten_url,
                };
            } else if custom_res.redirect.is_some() || custom_res.rewritten_url.is_some() {
                return FilterDecision {
                    should_block: true,
                    category: BlockCategory::Ad,
                    matched_rule: custom_res.filter.as_ref().and_then(|f| f.raw_line.clone()),
                    is_exception: false,
                    redirect_url: custom_res.redirect,
                    rewritten_url: custom_res.rewritten_url,
                };
            }
        }

        FilterDecision::allow()
    }

    /// Fast-path network request classification using default request type inference.
    pub fn check_url(
        &self,
        url: &str,
        source_origin: Option<&str>,
        is_third_party: bool,
    ) -> FilterDecision {
        self.check_network_request(url, source_origin, None, is_third_party)
    }

    /// Queries cosmetic resources (hide selectors, style selectors, injected scriptlets)
    /// for the given URL/origin across both primary and custom engines.
    pub fn url_cosmetic_resources(
        &self,
        url: &str,
    ) -> adblock::cosmetic_filter_cache::UrlSpecificResources {
        let full_url = if url.starts_with("http://") || url.starts_with("https://") {
            url.to_string()
        } else {
            format!("https://{url}")
        };

        let mut res = self.primary_engine.read().url_cosmetic_resources(&full_url);
        let custom_res = self.custom_engine.read().url_cosmetic_resources(&full_url);

        // Merge custom cosmetic resources
        res.hide_selectors.extend(custom_res.hide_selectors);
        if !custom_res.injected_script.is_empty() {
            if res.injected_script.is_empty() {
                res.injected_script = custom_res.injected_script;
            } else {
                res.injected_script.push('\n');
                res.injected_script.push_str(&custom_res.injected_script);
            }
        }

        res
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

    /// Serializes the compiled Engine into FlatBuffers binary (.dat) format for instant cold-starts.
    pub fn serialize_to_dat(&self) -> Vec<u8> {
        self.primary_engine.read().serialize()
    }

    /// Deserializes and restores the Engine from a precompiled FlatBuffers binary (.dat) cache (<15ms).
    pub fn deserialize_from_dat(&self, data: &[u8]) -> Result<(), String> {
        let mut new_engine = adblock::Engine::default();
        new_engine.deserialize(data).map_err(|e| format!("{e:?}"))?;
        *self.primary_engine.write() = new_engine;
        Ok(())
    }

    /// Converts standard AdBlock Plus syntax into native Apple WebKit Content Blocking JSON rules.
    /// Used directly on iOS (WKContentRuleList) for hardware-accelerated blocking in WebKit.
    pub fn convert_to_content_blocking(&self, rules: &str) -> Result<String, String> {
        const MAX_CB_LIST_SIZE: usize = 150_000;

        let mut filter_set = FilterSet::new(true);
        filter_set.add_filter_list(
            rules.to_string(),
            ParseOptions {
                rule_types: RuleTypes::NetworkOnly,
                ..Default::default()
            },
        );

        let (mut cb_rules, _) = filter_set
            .into_content_blocking()
            .map_err(|e| format!("{e:?}"))?;
        let rules_len = cb_rules.len();
        if rules_len > MAX_CB_LIST_SIZE {
            cb_rules.swap(rules_len - 1, MAX_CB_LIST_SIZE - 1);
            cb_rules.truncate(MAX_CB_LIST_SIZE);
        }

        serde_json::to_string(&cb_rules)
            .map_err(|e| format!("Failed to serialize content blocking JSON: {e}"))
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

        // 1. Should block ad domain when third-party
        let res1 = engine.check_url(
            "https://ad.doubleclick.net/ad.js",
            Some("https://example.com"),
            true,
        );
        assert!(res1.should_block);
        assert_eq!(res1.category, BlockCategory::Ad);

        // 1b. Third-party rule should NOT block first-party requests
        let res1_first_party = engine.check_url(
            "https://ad.doubleclick.net/ad.js",
            Some("https://doubleclick.net"),
            false,
        );
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
        let res5 = engine.check_url(
            "https://rr1---sn-4g5ednle.googlevideo.com/videoplayback?id=123",
            Some("https://youtube.com"),
            true,
        );
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
        assert!(
            css.contains(".ad-banner"),
            "Cosmetic CSS should contain .ad-banner: {css}"
        );

        let res = engine.check_url(
            "https://analytics.example.com/collect",
            Some("https://example.com"),
            true,
        );
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
        assert!(
            !serialized.is_empty(),
            "Serialized dat file should not be empty"
        );

        let restored_engine = AdBlockEngine::new();
        let res = restored_engine.deserialize_from_dat(&serialized);
        assert!(res.is_ok(), "Deserialization should succeed: {:?}", res);

        let check = restored_engine.check_url(
            "https://ad.doubleclick.net/ad.js",
            Some("https://test.com"),
            true,
        );
        assert!(
            check.should_block,
            "Restored engine should block doubleclick"
        );

        // Test attaching resources to deserialized engine without wiping rules
        let resource_json = r#"[{"name":"noopjs","aliases":[],"kind":{"mime":"application/javascript"},"content":"Ly8gbm9vcA=="}]"#;
        let res_attach = restored_engine.load_resources_json(resource_json);
        assert!(
            res_attach.is_ok(),
            "Attaching resources to restored engine should succeed"
        );
        let check_after_res = restored_engine.check_url(
            "https://ad.doubleclick.net/ad.js",
            Some("https://test.com"),
            true,
        );
        assert!(
            check_after_res.should_block,
            "Engine should STILL block doubleclick after attaching resources"
        );
    }

    #[test]
    fn test_ios_content_blocking_conversion() {
        let engine = AdBlockEngine::new();
        let rules = r#"
            ||doubleclick.net^
            @@||allowed.com^
        "#;
        let result = engine.convert_to_content_blocking(rules);
        assert!(
            result.is_ok(),
            "Content blocking conversion failed: {:?}",
            result
        );
        let json = result.unwrap();
        assert!(
            json.contains("url-filter"),
            "JSON should contain url-filter: {json}"
        );
    }
}
