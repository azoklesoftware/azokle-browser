use crate::category::BlockCategory;
use crate::rules::{NetworkRule, RulePattern};
use aho_corasick::{AhoCorasick, AhoCorasickBuilder};
use serde::{Deserialize, Serialize};
use std::collections::{HashMap, HashSet};
use url::Url;

pub use azokle_common::FilterDecision;

pub struct RuleMatcher {
    domain_exact_block: HashMap<String, (BlockCategory, String)>,
    domain_suffix_block: HashMap<String, (BlockCategory, String)>,
    domain_suffix_exceptions: HashSet<String>,
    substring_rules: Vec<(String, BlockCategory, String)>,
    substring_exceptions: Vec<(String, String)>,
    regex_rules: Vec<(regex::Regex, BlockCategory, String)>,
    regex_exceptions: Vec<(regex::Regex, String)>,
    ac_matcher: Option<AhoCorasick>,
    ac_rule_indices: Vec<usize>,
}

impl Default for RuleMatcher {
    fn default() -> Self {
        Self::new()
    }
}

impl RuleMatcher {
    pub fn new() -> Self {
        Self {
            domain_exact_block: HashMap::new(),
            domain_suffix_block: HashMap::new(),
            domain_suffix_exceptions: HashSet::new(),
            substring_rules: Vec::new(),
            substring_exceptions: Vec::new(),
            regex_rules: Vec::new(),
            regex_exceptions: Vec::new(),
            ac_matcher: None,
            ac_rule_indices: Vec::new(),
        }
    }

    pub fn add_rule(&mut self, rule: NetworkRule) {
        if rule.is_exception {
            match rule.pattern {
                RulePattern::DomainExact(d) | RulePattern::DomainSuffix(d) => {
                    self.domain_suffix_exceptions.insert(d);
                }
                RulePattern::Substring(s) => {
                    self.substring_exceptions.push((s, rule.raw));
                }
                RulePattern::RegexPattern(r) => {
                    self.regex_exceptions.push((r, rule.raw));
                }
            }
        } else {
            match rule.pattern {
                RulePattern::DomainExact(d) => {
                    self.domain_exact_block.insert(d, (rule.category, rule.raw));
                }
                RulePattern::DomainSuffix(d) => {
                    self.domain_suffix_block.insert(d, (rule.category, rule.raw));
                }
                RulePattern::Substring(s) => {
                    self.substring_rules.push((s, rule.category, rule.raw));
                }
                RulePattern::RegexPattern(r) => {
                    self.regex_rules.push((r, rule.category, rule.raw));
                }
            }
        }
    }

    pub fn rebuild_indexes(&mut self) {
        if self.substring_rules.is_empty() {
            self.ac_matcher = None;
            self.ac_rule_indices.clear();
            return;
        }

        let patterns: Vec<&str> = self.substring_rules.iter().map(|(pat, _, _)| pat.as_str()).collect();
        let ac = AhoCorasickBuilder::new()
            .ascii_case_insensitive(true)
            .build(&patterns);

        if let Ok(ac_engine) = ac {
            self.ac_matcher = Some(ac_engine);
            self.ac_rule_indices = (0..self.substring_rules.len()).collect();
        }
    }

    pub fn check_url(&self, request_url: &str, source_origin: Option<&str>, _is_third_party: bool) -> FilterDecision {
        let parsed = match Url::parse(request_url) {
            Ok(u) => u,
            Err(_) => {
                return FilterDecision {
                    should_block: false,
                    category: BlockCategory::None,
                    matched_rule: None,
                    is_exception: false,
                };
            }
        };

        let host = match parsed.host_str() {
            Some(h) => h.trim_end_matches('.').to_lowercase(),
            None => {
                return FilterDecision {
                    should_block: false,
                    category: BlockCategory::None,
                    matched_rule: None,
                    is_exception: false,
                };
            }
        };

        // Sovereign Whitelist: Never block sovereign Azokle domains, Cloudflare, or essential endpoints
        if host == "azokle.com"
            || host.ends_with(".azokle.com")
            || host == "cloudflare.com"
            || host.ends_with(".cloudflare.com")
            || host.ends_with(".cloudflareinsights.com")
            || host.ends_with(".hcaptcha.com")
            || host.ends_with(".recaptcha.net")
            || host.contains("googlevideo.com")
        {
            return FilterDecision {
                should_block: false,
                category: BlockCategory::None,
                matched_rule: None,
                is_exception: true,
            };
        }

        // Check if origin matches target host (first-party exemption if applicable)
        if let Some(src) = source_origin {
            if let Ok(src_url) = Url::parse(src) {
                if let Some(src_host) = src_url.host_str() {
                    let clean_src_host = src_host.trim_end_matches('.').to_lowercase();
                    if clean_src_host == host && host.contains("youtube.com") {
                        let path = parsed.path();
                        if path.contains("/player") || path.contains("/api/stats") || path.contains("/videoplayback") {
                            return FilterDecision {
                                should_block: false,
                                category: BlockCategory::None,
                                matched_rule: None,
                                is_exception: true,
                            };
                        }
                    }
                }
            }
        }

        // 1. Check Exceptions (Whitelist / Exclusions)
        if self.domain_suffix_exceptions.contains(&host) {
            return FilterDecision {
                should_block: false,
                category: BlockCategory::None,
                matched_rule: Some(format!("@@||{}", host)),
                is_exception: true,
            };
        }

        // Domain suffix matching for exceptions
        let parts: Vec<&str> = host.split('.').collect();
        if parts.len() > 2 {
            for i in 1..parts.len() - 1 {
                let sub = parts[i..].join(".");
                if self.domain_suffix_exceptions.contains(&sub) {
                    return FilterDecision {
                        should_block: false,
                        category: BlockCategory::None,
                        matched_rule: Some(format!("@@||{}", sub)),
                        is_exception: true,
                    };
                }
            }
        }

        for (exc_sub, raw) in &self.substring_exceptions {
            if request_url.contains(exc_sub) {
                return FilterDecision {
                    should_block: false,
                    category: BlockCategory::None,
                    matched_rule: Some(raw.clone()),
                    is_exception: true,
                };
            }
        }

        for (re, raw) in &self.regex_exceptions {
            if re.is_match(request_url) {
                return FilterDecision {
                    should_block: false,
                    category: BlockCategory::None,
                    matched_rule: Some(raw.clone()),
                    is_exception: true,
                };
            }
        }

        // 2. Check Exact Domain Block
        if let Some((cat, raw)) = self.domain_exact_block.get(&host) {
            return FilterDecision {
                should_block: true,
                category: *cat,
                matched_rule: Some(raw.clone()),
                is_exception: false,
            };
        }

        // 3. Check Domain Suffix Block
        if let Some((cat, raw)) = self.domain_suffix_block.get(&host) {
            return FilterDecision {
                should_block: true,
                category: *cat,
                matched_rule: Some(raw.clone()),
                is_exception: false,
            };
        }

        if parts.len() > 2 {
            for i in 1..parts.len() - 1 {
                let sub = parts[i..].join(".");
                if let Some((cat, raw)) = self.domain_suffix_block.get(&sub) {
                    return FilterDecision {
                        should_block: true,
                        category: *cat,
                        matched_rule: Some(raw.clone()),
                        is_exception: false,
                    };
                }
            }
        }

        // 4. Check Substring Rules with Aho-Corasick
        let lower_url = request_url.to_lowercase();
        if let Some(ac) = &self.ac_matcher {
            if let Some(mat) = ac.find(&lower_url) {
                let rule_idx = mat.pattern().as_usize();
                if let Some((_, cat, raw)) = self.substring_rules.get(rule_idx) {
                    return FilterDecision {
                        should_block: true,
                        category: *cat,
                        matched_rule: Some(raw.clone()),
                        is_exception: false,
                    };
                }
            }
        } else {
            for (sub, cat, raw) in &self.substring_rules {
                if lower_url.contains(sub) {
                    return FilterDecision {
                        should_block: true,
                        category: *cat,
                        matched_rule: Some(raw.clone()),
                        is_exception: false,
                    };
                }
            }
        }

        // 5. Check Regex Rules
        for (re, cat, raw) in &self.regex_rules {
            if re.is_match(request_url) {
                return FilterDecision {
                    should_block: true,
                    category: *cat,
                    matched_rule: Some(raw.clone()),
                    is_exception: false,
                };
            }
        }

        FilterDecision {
            should_block: false,
            category: BlockCategory::None,
            matched_rule: None,
            is_exception: false,
        }
    }
}
