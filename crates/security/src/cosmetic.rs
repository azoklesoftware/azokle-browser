use std::collections::{HashMap, HashSet};

pub struct CosmeticEngine {
    global_selectors: HashSet<String>,
    domain_selectors: HashMap<String, HashSet<String>>,
    domain_exceptions: HashMap<String, HashSet<String>>,
}

impl Default for CosmeticEngine {
    fn default() -> Self {
        Self::new()
    }
}

impl CosmeticEngine {
    pub fn new() -> Self {
        let mut global_selectors = HashSet::new();
        // Built-in standard ad containers & banner classes
        let default_globals = [
            ".adsbygoogle",
            "amp-ad",
            "amp-embed",
            "div[id^='google_ads_iframe']",
            "div[id^='ad-div-']",
            "div[class*='ad-banner']",
            "div[class*='ad-container']",
            "div[class*='ad-placement']",
            "div[class*='sponsored-post']",
            "div[data-ad-unit]",
            "div[data-google-query-id]",
            "ins.adsbygoogle",
            ".taboola-ad",
            ".outbrain-ad",
        ];

        for s in default_globals {
            global_selectors.insert(s.to_string());
        }

        Self {
            global_selectors,
            domain_selectors: HashMap::new(),
            domain_exceptions: HashMap::new(),
        }
    }

    pub fn parse_rule_line(&mut self, line: &str) -> bool {
        let trimmed = line.trim();
        if trimmed.is_empty() || trimmed.starts_with('!') || trimmed.starts_with('[') {
            return false;
        }

        // Check if cosmetic exception: domain#@#selector
        if let Some((domains_part, selector)) = trimmed.split_once("#@#") {
            let sel = selector.trim();
            if sel.is_empty() {
                return false;
            }
            for d in domains_part.split(',') {
                let d = d.trim().to_lowercase();
                if !d.is_empty() {
                    self.domain_exceptions
                        .entry(d)
                        .or_default()
                        .insert(sel.to_string());
                }
            }
            return true;
        }

        // Check if cosmetic rule: domain##selector or ##selector
        if let Some((domains_part, selector)) = trimmed.split_once("##") {
            let sel = selector.trim();
            if sel.is_empty() || sel.starts_with("+js(") {
                return false;
            }

            if domains_part.is_empty() {
                self.global_selectors.insert(sel.to_string());
            } else {
                for d in domains_part.split(',') {
                    let d = d.trim().to_lowercase();
                    if !d.is_empty() && !d.starts_with('~') {
                        self.domain_selectors
                            .entry(d)
                            .or_default()
                            .insert(sel.to_string());
                    }
                }
            }
            return true;
        }

        // Raw CSS selector line (e.g. from cosmetic_css.txt)
        if !trimmed.contains("##") && !trimmed.contains("||") && !trimmed.starts_with('/') {
            self.global_selectors.insert(trimmed.to_string());
            return true;
        }

        false
    }

    pub fn load_from_text(&mut self, content: &str) -> usize {
        let mut count = 0;
        for line in content.lines() {
            if self.parse_rule_line(line) {
                count += 1;
            }
        }
        count
    }

    pub fn get_css_for_domain(&self, domain: &str) -> String {
        let clean_domain = domain.to_lowercase();
        let mut selectors: HashSet<&str> = self.global_selectors.iter().map(|s| s.as_str()).collect();

        // Add domain-specific selectors
        if let Some(domain_set) = self.domain_selectors.get(&clean_domain) {
            for s in domain_set {
                selectors.insert(s.as_str());
            }
        }

        // Check parent domains
        let parts: Vec<&str> = clean_domain.split('.').collect();
        if parts.len() > 2 {
            for i in 1..parts.len() - 1 {
                let sub = parts[i..].join(".");
                if let Some(sub_set) = self.domain_selectors.get(&sub) {
                    for s in sub_set {
                        selectors.insert(s.as_str());
                    }
                }
            }
        }

        // Remove exceptions
        if let Some(exceptions) = self.domain_exceptions.get(&clean_domain) {
            for exc in exceptions {
                selectors.remove(exc.as_str());
            }
        }

        if selectors.is_empty() {
            return String::new();
        }

        let combined = selectors.into_iter().collect::<Vec<&str>>().join(",\n");
        format!("{} {{\n  display: none !important;\n  visibility: hidden !important;\n  height: 0 !important;\n  min-height: 0 !important;\n  opacity: 0 !important;\n  pointer-events: none !important;\n}}", combined)
    }
}
