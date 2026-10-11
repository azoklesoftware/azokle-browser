// Copyright (c) 2026 The Azokle Authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

use std::collections::{HashMap, HashSet};

pub struct CosmeticEngine {
    global_selectors: HashSet<String>,
    domain_selectors: HashMap<String, HashSet<String>>,
    domain_exceptions: HashMap<String, HashSet<String>>,
}

impl Default for CosmeticEngine {
    /// Returns the default instance.
    fn default() -> Self {
        Self::new()
    }
}

impl CosmeticEngine {
    /// Creates a new instance with default configuration.
    pub fn new() -> Self {
        let mut global_selectors = HashSet::new();
        // Built-in safe standard ad provider tags & verified classes
        let default_globals = [
            ".adsbygoogle",
            "amp-ad",
            "amp-embed",
            "ins.adsbygoogle",
            "div[id^='google_ads_iframe']",
            ".taboola-ad",
            ".outbrain-ad",
            "div[data-ad-unit]",
            "div[data-google-query-id]",
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

    /// Parses rule line from the given input.
    pub fn parse_rule_line(&mut self, line: &str) -> bool {
        let trimmed = line.trim();
        if trimmed.is_empty() || trimmed.starts_with('!') || trimmed.starts_with('[') {
            return false;
        }

        // Security check: reject rules attempting CSS breakout or comment injection
        if trimmed.contains('{')
            || trimmed.contains('}')
            || trimmed.contains("/*")
            || trimmed.contains("*/")
        {
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

        // Raw CSS selector line
        if !trimmed.contains("##") && !trimmed.contains("||") && !trimmed.starts_with('/') {
            // Only accept specific, non-destructive selector classes
            if trimmed.starts_with('.') || trimmed.starts_with('#') || trimmed.starts_with('[') {
                self.global_selectors.insert(trimmed.to_string());
                return true;
            }
        }

        false
    }

    /// Performs load from text operation.
    pub fn load_from_text(&mut self, content: &str) -> usize {
        let mut count = 0;
        for line in content.lines() {
            if self.parse_rule_line(line) {
                count += 1;
            }
        }
        count
    }

    /// Returns the css for domain.
    pub fn get_css_for_domain(&self, domain: &str) -> String {
        let clean_domain = domain.to_lowercase();
        let mut selectors: HashSet<&str> =
            self.global_selectors.iter().map(|s| s.as_str()).collect();

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
