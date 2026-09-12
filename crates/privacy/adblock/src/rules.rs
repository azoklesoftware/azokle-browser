use crate::category::BlockCategory;
use regex::Regex;

/// Pattern matching strategy for network filtering rules.
#[derive(Debug, Clone)]
pub enum RulePattern {
    /// Matches exact domain strings (e.g. `|example.com|`).
    DomainExact(String),
    /// Matches a domain and all its subdomains (e.g. `||example.com^`).
    DomainSuffix(String),
    /// Fast substring search pattern evaluated against full request URLs.
    Substring(String),
    /// Regular expression pattern compiled for complex wildcards or boundary assertions.
    RegexPattern(Regex),
}

/// Represents a parsed Adblock Plus / uBlock Origin compatible network filter rule.
#[derive(Debug, Clone)]
pub struct NetworkRule {
    /// The compiled pattern variant used for URL matching.
    pub pattern: RulePattern,
    /// Indicates whether this rule is an exception / whitelist rule (`@@`).
    pub is_exception: bool,
    /// Privacy category assigned to this rule (Ads, Trackers, Miners, etc.).
    pub category: BlockCategory,
    /// If true, rule only applies to cross-site requests.
    pub third_party_only: bool,
    /// If true, rule only applies to same-site requests.
    pub first_party_only: bool,
    /// Original unparsed filter rule line retained for diagnostic and telemetry reporting.
    pub raw: String,
}

impl NetworkRule {
    /// Parses a single line from an Adblock Plus or uBlock Origin filter list.
    ///
    /// Ignores comments (`!`, `#`), metadata headers (`[Adblock...`), and cosmetic
    /// hiding rules (`##`, `#@#`, `#?#`), which are handled by the cosmetic engine.
    /// Supports exception prefixes (`@@`), domain anchors (`||`, `|`), regex syntax (`/.../`),
    /// and filter options (`$third-party`, `$first-party`, `$badfilter`).
    pub fn parse(line: &str, default_category: BlockCategory) -> Option<Self> {
        let trimmed = line.trim();
        if trimmed.is_empty()
            || trimmed.starts_with('!')
            || trimmed.starts_with('#')
            || trimmed.starts_with("[Adblock")
        {
            return None;
        }

        // Cosmetic filter lines are handled by the security/cosmetic crate
        if trimmed.contains("##") || trimmed.contains("#@#") || trimmed.contains("#?#") {
            return None;
        }

        let (is_exception, content) = if let Some(stripped) = trimmed.strip_prefix("@@") {
            (true, stripped)
        } else {
            (false, trimmed)
        };

        // Parse ABP options after $
        let (rule_part, options_part) = match content.split_once('$') {
            Some((r, opts)) => (r, Some(opts)),
            None => (content, None),
        };

        let mut third_party = false;
        let mut first_party = false;
        let category = default_category;

        if let Some(opts) = options_part {
            for opt in opts.split(',') {
                let opt = opt.trim().to_lowercase();
                if opt == "third-party" || opt == "3p" {
                    third_party = true;
                } else if opt == "~third-party" || opt == "~3p" || opt == "first-party" || opt == "1p" {
                    first_party = true;
                } else if opt == "badfilter" {
                    return None;
                }
            }
        }

        let pattern = if let Some(domain_pat) = rule_part.strip_prefix("||") {
            let domain_clean = domain_pat
                .trim_end_matches('^')
                .trim_end_matches('/')
                .trim_end_matches('|');
            if domain_clean.contains('/') || domain_clean.contains('*') {
                let mut regex_str = format!(
                    "^(?:https?://)?(?:[a-zA-Z0-9_.-]+\\.)?{}",
                    regex::escape(domain_clean)
                );
                regex_str = regex_str.replace("\\*", ".*");
                regex_str = regex_str.replace("\\^", "(?:[^a-zA-Z0-9_.-]|$)");
                if let Ok(re) = Regex::new(&regex_str) {
                    RulePattern::RegexPattern(re)
                } else {
                    RulePattern::Substring(domain_clean.to_lowercase())
                }
            } else {
                RulePattern::DomainSuffix(domain_clean.to_lowercase())
            }
        } else if rule_part.starts_with('/') && rule_part.ends_with('/') && rule_part.len() > 2 {
            let regex_str = &rule_part[1..rule_part.len() - 1];
            if regex_str.len() <= 512 {
                if let Ok(re) = Regex::new(regex_str) {
                    RulePattern::RegexPattern(re)
                } else {
                    RulePattern::Substring(rule_part.to_lowercase())
                }
            } else {
                RulePattern::Substring(rule_part.to_lowercase())
            }
        } else if rule_part.starts_with('|') && rule_part.ends_with('|') {
            let exact = rule_part.trim_matches('|');
            RulePattern::DomainExact(exact.to_lowercase())
        } else if let Some(prefix) = rule_part.strip_prefix('|') {
            let clean = prefix.trim_end_matches('^').trim_end_matches('|');
            let regex_str = format!("^{}", regex::escape(clean).replace("\\*", ".*").replace("\\^", "(?:[^a-zA-Z0-9_.-]|$)"));
            if let Ok(re) = Regex::new(&regex_str) {
                RulePattern::RegexPattern(re)
            } else {
                RulePattern::Substring(clean.to_lowercase())
            }
        } else if let Some(suffix) = rule_part.strip_suffix('|') {
            let clean = suffix.trim_start_matches('^');
            let regex_str = format!("{}$", regex::escape(clean).replace("\\*", ".*").replace("\\^", "(?:[^a-zA-Z0-9_.-]|$)"));
            if let Ok(re) = Regex::new(&regex_str) {
                RulePattern::RegexPattern(re)
            } else {
                RulePattern::Substring(clean.to_lowercase())
            }
        } else {
            RulePattern::Substring(rule_part.to_lowercase())
        };

        Some(NetworkRule {
            pattern,
            is_exception,
            category,
            third_party_only: third_party,
            first_party_only: first_party,
            raw: trimmed.to_string(),
        })
    }
}
