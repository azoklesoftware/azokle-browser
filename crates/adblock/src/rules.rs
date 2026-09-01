use crate::category::BlockCategory;
use regex::Regex;

#[derive(Debug, Clone)]
pub enum RulePattern {
    DomainExact(String),
    DomainSuffix(String),
    Substring(String),
    RegexPattern(Regex),
}

#[derive(Debug, Clone)]
pub struct NetworkRule {
    pub pattern: RulePattern,
    pub is_exception: bool,
    pub category: BlockCategory,
    pub third_party_only: bool,
    pub raw: String,
}

impl NetworkRule {
    pub fn parse(line: &str, default_category: BlockCategory) -> Option<Self> {
        let trimmed = line.trim();
        if trimmed.is_empty() || trimmed.starts_with('!') || trimmed.starts_with('#') || trimmed.starts_with("[Adblock") {
            return None;
        }

        // Cosmetic filter lines are handled by the shield crate
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
        if let Some(opts) = options_part {
            for opt in opts.split(',') {
                let opt = opt.trim();
                if opt == "third-party" || opt == "3p" {
                    third_party = true;
                }
            }
        }

        let pattern = if let Some(domain_pat) = rule_part.strip_prefix("||") {
            let domain_clean = domain_pat.trim_end_matches('^').trim_end_matches('/').trim_end_matches('|');
            if domain_clean.contains('/') || domain_clean.contains('*') {
                let mut regex_str = format!("^(?:https?://)?(?:[a-zA-Z0-9_-]+\\.)?{}", regex::escape(domain_clean));
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
            if let Ok(re) = Regex::new(regex_str) {
                RulePattern::RegexPattern(re)
            } else {
                RulePattern::Substring(rule_part.to_lowercase())
            }
        } else if rule_part.starts_with('|') && rule_part.ends_with('|') {
            let exact = rule_part.trim_matches('|');
            RulePattern::DomainExact(exact.to_lowercase())
        } else {
            RulePattern::Substring(rule_part.to_lowercase())
        };

        Some(NetworkRule {
            pattern,
            is_exception,
            category: default_category,
            third_party_only: third_party,
            raw: trimmed.to_string(),
        })
    }
}
