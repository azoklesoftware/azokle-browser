use crate::category::BlockCategory;
use crate::rules::{NetworkRule, RulePattern};
use aho_corasick::{AhoCorasick, AhoCorasickBuilder};
use std::collections::HashMap;
use url::Url;

pub use azokle_common::FilterDecision;

/// Metadata attached to a compiled blocking rule for post-match evaluation.
#[derive(Debug, Clone)]
struct RuleMeta {
    category: BlockCategory,
    third_party_only: bool,
    first_party_only: bool,
    raw: String,
}

/// Metadata attached to an exception rule (`@@`) to cancel blocking.
#[derive(Debug, Clone)]
struct ExceptionMeta {
    third_party_only: bool,
    first_party_only: bool,
    raw: String,
}

/// High-throughput network filtering engine that evaluates URLs against adblock rules.
///
/// Employs a tiered indexing strategy:
/// 1. Exact domain lookups (`O(1)` hash map)
/// 2. Suffix/subdomain lookups (`O(k)` where k is domain depth)
/// 3. Multi-pattern substring matching via an ASCII-case-insensitive Aho-Corasick automaton
/// 4. Regex pattern matching fallback for complex dynamic rules
///
/// Exception rules (`@@`) strictly supersede blocking rules across all tiers.
pub struct RuleMatcher {
    domain_exact_block: HashMap<String, RuleMeta>,
    domain_suffix_block: HashMap<String, RuleMeta>,
    domain_exact_exceptions: HashMap<String, ExceptionMeta>,
    domain_suffix_exceptions: HashMap<String, ExceptionMeta>,
    substring_rules: Vec<(String, RuleMeta)>,
    substring_exceptions: Vec<(String, ExceptionMeta)>,
    regex_rules: Vec<(regex::Regex, RuleMeta)>,
    regex_exceptions: Vec<(regex::Regex, ExceptionMeta)>,
    ac_matcher: Option<AhoCorasick>,
    ac_rule_indices: Vec<usize>,
}

impl Default for RuleMatcher {
    fn default() -> Self {
        Self::new()
    }
}

impl RuleMatcher {
    /// Creates an empty `RuleMatcher` with initialized index buckets.
    pub fn new() -> Self {
        Self {
            domain_exact_block: HashMap::new(),
            domain_suffix_block: HashMap::new(),
            domain_exact_exceptions: HashMap::new(),
            domain_suffix_exceptions: HashMap::new(),
            substring_rules: Vec::new(),
            substring_exceptions: Vec::new(),
            regex_rules: Vec::new(),
            regex_exceptions: Vec::new(),
            ac_matcher: None,
            ac_rule_indices: Vec::new(),
        }
    }

    /// Registers a parsed [`NetworkRule`] into its corresponding bucket.
    ///
    /// Rules are routed based on whether they are exception rules (`@@`) and
    /// their target [`RulePattern`] variant (exact domain, suffix, substring, regex).
    /// If substring rules are added, [`rebuild_indexes`](Self::rebuild_indexes) should
    /// be invoked once batch loading completes to compile the Aho-Corasick automaton.
    pub fn add_rule(&mut self, rule: NetworkRule) {
        if rule.is_exception {
            let meta = ExceptionMeta {
                third_party_only: rule.third_party_only,
                first_party_only: rule.first_party_only,
                raw: rule.raw,
            };
            match rule.pattern {
                RulePattern::DomainExact(d) => {
                    self.domain_exact_exceptions.insert(d, meta);
                }
                RulePattern::DomainSuffix(d) => {
                    self.domain_suffix_exceptions.insert(d, meta);
                }
                RulePattern::Substring(s) => {
                    self.substring_exceptions.push((s, meta));
                }
                RulePattern::RegexPattern(r) => {
                    self.regex_exceptions.push((r, meta));
                }
            }
        } else {
            let meta = RuleMeta {
                category: rule.category,
                third_party_only: rule.third_party_only,
                first_party_only: rule.first_party_only,
                raw: rule.raw,
            };
            match rule.pattern {
                RulePattern::DomainExact(d) => {
                    self.domain_exact_block.insert(d, meta);
                }
                RulePattern::DomainSuffix(d) => {
                    self.domain_suffix_block.insert(d, meta);
                }
                RulePattern::Substring(s) => {
                    self.substring_rules.push((s, meta));
                }
                RulePattern::RegexPattern(r) => {
                    self.regex_rules.push((r, meta));
                }
            }
        }
    }

    /// Rebuilds the Aho-Corasick automaton from registered substring filter rules.
    ///
    /// Must be called after populating rules into the engine to enable vectorized
    /// linear-time multi-pattern matching across target URLs.
    pub fn rebuild_indexes(&mut self) {
        if self.substring_rules.is_empty() {
            self.ac_matcher = None;
            self.ac_rule_indices.clear();
            return;
        }

        let patterns: Vec<&str> = self.substring_rules.iter().map(|(pat, _)| pat.as_str()).collect();
        let ac = AhoCorasickBuilder::new()
            .ascii_case_insensitive(true)
            .build(&patterns);

        if let Ok(ac_engine) = ac {
            self.ac_matcher = Some(ac_engine);
            self.ac_rule_indices = (0..self.substring_rules.len()).collect();
        }
    }

    /// Evaluates whether a rule's party restriction matches the request context.
    ///
    /// Returns `true` if the rule is permitted to match, or `false` if it must be skipped.
    fn check_party_filter(third_party_only: bool, first_party_only: bool, is_third_party: bool) -> bool {
        if third_party_only && !is_third_party {
            return false;
        }
        if first_party_only && is_third_party {
            return false;
        }
        true
    }

    /// Evaluates a candidate network request against all compiled filtering rules.
    ///
    /// # Evaluation Pipeline & Precedence:
    /// 1. Parse URL & extract lowercase hostname (stripping any trailing FQDN dot).
    /// 2. Derive effective party context by comparing `source_origin` and request host via eTLD+1.
    /// 3. Sovereign Infrastructure Pass: Bypasses filtering for core Azokle endpoints and critical
    ///    anti-bot challenge infrastructure (e.g. hCaptcha, reCAPTCHA, Cloudflare Turnstile).
    /// 4. Exception Rules (`@@`): Evaluated across exact domain, suffix, substring, and regex buckets.
    ///    Any match immediately yields an allow decision (`should_block: false`).
    /// 5. Blocking Rules: Evaluated across exact domain, suffix domain, Aho-Corasick substring matches,
    ///    and regex patterns.
    ///
    /// # Returns
    /// A [`FilterDecision`] indicating whether the request should be blocked, the category, and matched rule.
    pub fn check_url(&self, request_url: &str, source_origin: Option<&str>, is_third_party: bool) -> FilterDecision {
        let parsed = match Url::parse(request_url) {
            Ok(u) => u,
            Err(_) => {
                return FilterDecision {
                    should_block: false,
                    category: BlockCategory::None,
                    matched_rule: None,
                    is_exception: false,
                    ..Default::default()
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
                    ..Default::default()
                };
            }
        };

        // Determine effective party context (first-party vs third-party)
        let effective_third_party = if let Some(src) = source_origin {
            if let Ok(src_url) = Url::parse(src) {
                if let Some(src_host) = src_url.host_str() {
                    let clean_src = src_host.trim_end_matches('.').to_lowercase();
                    !Self::is_same_site(&clean_src, &host)
                } else {
                    is_third_party
                }
            } else {
                is_third_party
            }
        } else {
            is_third_party
        };

        // Sovereign Infrastructure Whitelist: Only Azokle sovereign domains & bot challenge infrastructure
        if host == "azokle.com"
            || host.ends_with(".azokle.com")
            || host == "cloudflare.com"
            || host.ends_with(".cloudflare.com")
            || host.ends_with(".cloudflareinsights.com")
            || host.ends_with(".hcaptcha.com")
            || host.ends_with(".recaptcha.net")
        {
            return FilterDecision {
                should_block: false,
                category: BlockCategory::None,
                matched_rule: Some("@@||sovereign-whitelist".to_string()),
                is_exception: true,
                ..Default::default()
            };
        }

        // =====================================================================
        // 1. Check Exceptions (Whitelist / Exclusions) - Checked first
        // =====================================================================

        // 1a. Exact domain exceptions
        if let Some(exc) = self.domain_exact_exceptions.get(&host) {
            if Self::check_party_filter(exc.third_party_only, exc.first_party_only, effective_third_party) {
                return FilterDecision {
                    should_block: false,
                    category: BlockCategory::None,
                    matched_rule: Some(exc.raw.clone()),
                    is_exception: true,
                    ..Default::default()
                };
            }
        }

        // 1b. Suffix domain exceptions
        if let Some(exc) = self.domain_suffix_exceptions.get(&host) {
            if Self::check_party_filter(exc.third_party_only, exc.first_party_only, effective_third_party) {
                return FilterDecision {
                    should_block: false,
                    category: BlockCategory::None,
                    matched_rule: Some(exc.raw.clone()),
                    is_exception: true,
                    ..Default::default()
                };
            }
        }

        let parts: Vec<&str> = host.split('.').collect();
        if parts.len() > 2 {
            for i in 1..parts.len() - 1 {
                let sub = parts[i..].join(".");
                if let Some(exc) = self.domain_suffix_exceptions.get(&sub) {
                    if Self::check_party_filter(exc.third_party_only, exc.first_party_only, effective_third_party) {
                        return FilterDecision {
                            should_block: false,
                            category: BlockCategory::None,
                            matched_rule: Some(exc.raw.clone()),
                            is_exception: true,
                            ..Default::default()
                        };
                    }
                }
            }
        }

        // 1c. Substring exceptions
        let lower_url = request_url.to_lowercase();
        for (exc_sub, meta) in &self.substring_exceptions {
            if lower_url.contains(exc_sub)
                && Self::check_party_filter(meta.third_party_only, meta.first_party_only, effective_third_party)
            {
                return FilterDecision {
                    should_block: false,
                    category: BlockCategory::None,
                    matched_rule: Some(meta.raw.clone()),
                    is_exception: true,
                    ..Default::default()
                };
            }
        }

        // 1d. Regex exceptions
        for (re, meta) in &self.regex_exceptions {
            if re.is_match(request_url)
                && Self::check_party_filter(meta.third_party_only, meta.first_party_only, effective_third_party)
            {
                return FilterDecision {
                    should_block: false,
                    category: BlockCategory::None,
                    matched_rule: Some(meta.raw.clone()),
                    is_exception: true,
                    ..Default::default()
                };
            }
        }

        // =====================================================================
        // 2. Check Exact Domain Block
        // =====================================================================
        if let Some(rule) = self.domain_exact_block.get(&host) {
            if Self::check_party_filter(rule.third_party_only, rule.first_party_only, effective_third_party) {
                return FilterDecision {
                    should_block: true,
                    category: rule.category,
                    matched_rule: Some(rule.raw.clone()),
                    is_exception: false,
                    ..Default::default()
                };
            }
        }

        // =====================================================================
        // 3. Check Domain Suffix Block
        // =====================================================================
        if let Some(rule) = self.domain_suffix_block.get(&host) {
            if Self::check_party_filter(rule.third_party_only, rule.first_party_only, effective_third_party) {
                return FilterDecision {
                    should_block: true,
                    category: rule.category,
                    matched_rule: Some(rule.raw.clone()),
                    is_exception: false,
                    ..Default::default()
                };
            }
        }

        if parts.len() > 2 {
            for i in 1..parts.len() - 1 {
                let sub = parts[i..].join(".");
                if let Some(rule) = self.domain_suffix_block.get(&sub) {
                    if Self::check_party_filter(rule.third_party_only, rule.first_party_only, effective_third_party) {
                        return FilterDecision {
                            should_block: true,
                            category: rule.category,
                            matched_rule: Some(rule.raw.clone()),
                            is_exception: false,
                            ..Default::default()
                        };
                    }
                }
            }
        }

        // =====================================================================
        // 4. Check Substring Rules with Aho-Corasick
        // =====================================================================
        if let Some(ac) = &self.ac_matcher {
            for mat in ac.find_iter(&lower_url) {
                let rule_idx = mat.pattern().as_usize();
                if let Some((_, rule)) = self.substring_rules.get(rule_idx) {
                    if Self::check_party_filter(rule.third_party_only, rule.first_party_only, effective_third_party) {
                        return FilterDecision {
                            should_block: true,
                            category: rule.category,
                            matched_rule: Some(rule.raw.clone()),
                            is_exception: false,
                            ..Default::default()
                        };
                    }
                }
            }
        } else {
            for (sub, rule) in &self.substring_rules {
                if lower_url.contains(sub)
                    && Self::check_party_filter(rule.third_party_only, rule.first_party_only, effective_third_party)
                {
                    return FilterDecision {
                        should_block: true,
                        category: rule.category,
                        matched_rule: Some(rule.raw.clone()),
                        is_exception: false,
                        ..Default::default()
                    };
                }
            }
        }

        // =====================================================================
        // 5. Check Regex Rules
        // =====================================================================
        for (re, rule) in &self.regex_rules {
            if re.is_match(request_url)
                && Self::check_party_filter(rule.third_party_only, rule.first_party_only, effective_third_party)
            {
                return FilterDecision {
                    should_block: true,
                    category: rule.category,
                    matched_rule: Some(rule.raw.clone()),
                    is_exception: false,
                    ..Default::default()
                };
            }
        }

        FilterDecision {
            should_block: false,
            category: BlockCategory::None,
            matched_rule: None,
            is_exception: false,
            ..Default::default()
        }
    }

    /// Determines whether two hostnames belong to the same site (eTLD+1).
    ///
    /// Utilizes the Public Suffix List via `adblock::url_parser` to correctly identify
    /// registrable domains (e.g. `sub.example.com` and `example.com` are same-site,
    /// while `foo.github.io` and `bar.github.io` are distinct cross-site entities).
    fn is_same_site(host_a: &str, host_b: &str) -> bool {
        if host_a == host_b {
            return true;
        }
        let url_a = format!("https://{host_a}");
        let url_b = format!("https://{host_b}");
        if let (Some(parsed_a), Some(parsed_b)) = (
            adblock::url_parser::parse_url(&url_a),
            adblock::url_parser::parse_url(&url_b),
        ) {
            let dom_a = parsed_a.domain();
            let dom_b = parsed_b.domain();
            if !dom_a.is_empty() && !dom_b.is_empty() && dom_a == dom_b {
                return true;
            }
        }
        false
    }
}

