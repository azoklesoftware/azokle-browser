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

use serde::Deserialize;
use std::collections::HashMap;
use url::Url;

/// Deserialized representation of an upstream bounce tracking rule.
#[derive(Debug, Clone, Deserialize)]
pub struct DebounceRuleRaw {
    /// Inverted or glob patterns of redirect URLs to intercept.
    pub include: Vec<String>,
    #[serde(default)]
    pub exclude: Vec<String>,
    #[serde(default)]
    pub action: String,
    /// Target query parameter containing the unencoded destination URL.
    pub param: String,
}

#[derive(Debug, Clone)]
struct CompiledPattern {
    host_suffix: String,
    path_prefix: Option<String>,
    param_name: String,
}

/// Anti-bounce-tracking engine that detects intermediary redirectors and unwraps destinations.
///
/// Bounce tracking routes users through an intermediate tracker domain solely to drop cookies
/// or log tracking state before forwarding to the actual destination. This engine extracts
/// the embedded destination query parameter and redirects the browser immediately, preventing
/// tracker session creation.
#[derive(Default)]
pub struct DebounceEngine {
    // Map host suffix to compiled rules
    host_rules: HashMap<String, Vec<CompiledPattern>>,
}

impl DebounceEngine {
    /// Creates an uninitialized debounce engine.
    pub fn new() -> Self {
        Self {
            host_rules: HashMap::new(),
        }
    }

    /// Loads and compiles bounce tracking rules from JSON format (e.g. Brave debounce list).
    pub fn load_from_json(&mut self, json_str: &str) -> Result<usize, String> {
        let raw_rules: Vec<DebounceRuleRaw> = serde_json::from_str(json_str)
            .map_err(|e| format!("Failed to parse debounce JSON: {e}"))?;

        let mut count = 0;
        for rule in raw_rules {
            if rule.action != "redirect" && !rule.action.is_empty() {
                continue;
            }
            let param_name = rule.param;
            for inc in rule.include {
                if let Some(pat) = Self::compile_pattern(&inc, &param_name) {
                    self.host_rules
                        .entry(pat.host_suffix.clone())
                        .or_default()
                        .push(pat);
                    count += 1;
                }
            }
        }
        Ok(count)
    }

    /// Compiles a URL pattern glob string into an indexed [`CompiledPattern`].
    fn compile_pattern(pattern: &str, param_name: &str) -> Option<CompiledPattern> {
        // Typical patterns: "*://cc.zdnet.com/v1/otc/*", "*://*.prsm1.com/r?*"
        let stripped = pattern
            .trim_start_matches("*://")
            .trim_start_matches("https://")
            .trim_start_matches("http://");

        let (host_part, path_part) = match stripped.split_once('/') {
            Some((h, p)) => (h, Some(p)),
            None => (stripped, None),
        };

        let host_clean = host_part.trim_start_matches("*.").to_lowercase();
        if host_clean.is_empty() {
            return None;
        }

        let path_prefix = path_part.map(|p| {
            let clean = p.trim_end_matches('*').trim_end_matches('?');
            format!("/{clean}")
        });

        Some(CompiledPattern {
            host_suffix: host_clean,
            path_prefix,
            param_name: param_name.to_string(),
        })
    }

    /// Evaluates a candidate navigation URL and unwraps it if it matches a known redirector.
    ///
    /// # Security and Validation:
    /// - Matches against compiled host suffixes and path prefixes.
    /// - Scans query parameters case-insensitively for the redirect target parameter.
    /// - Verifies candidate destination starts with `http://` or `https://` and successfully parses
    ///   as a valid URL to prevent protocol escalation or `javascript:` URI injection.
    ///
    /// Returns `Some(unwrapped_target_url)` when successfully unwrapped, or `None` if normal navigation.
    pub fn unwrap_bounce_url(&self, raw_url: &str) -> Option<String> {
        let parsed = Url::parse(raw_url).ok()?;
        let host = parsed.host_str()?.to_lowercase();
        let path = parsed.path();

        // Check each host suffix match
        for (suffix, patterns) in &self.host_rules {
            if host == *suffix || host.ends_with(&format!(".{suffix}")) {
                for pat in patterns {
                    if let Some(ref req_prefix) = pat.path_prefix {
                        if !path.starts_with(req_prefix) {
                            continue;
                        }
                    }

                    // Extract target query param
                    for (k, v) in parsed.query_pairs() {
                        if k.eq_ignore_ascii_case(&pat.param_name) {
                            let candidate = v.trim();
                            // If the candidate looks like a valid http/https URL, return it
                            if (candidate.starts_with("http://")
                                || candidate.starts_with("https://"))
                                && Url::parse(candidate).is_ok()
                            {
                                return Some(candidate.to_string());
                            }
                        }
                    }
                }
            }
        }

        None
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_debounce_unwrapping() {
        let mut engine = DebounceEngine::new();
        let sample_json = r#"[
            {
                "include": [
                    "*://cc.zdnet.com/v1/otc/*",
                    "*://*.tkqlhce.com/click-*"
                ],
                "action": "redirect",
                "param": "url"
            }
        ]"#;

        assert_eq!(engine.load_from_json(sample_json).unwrap(), 2);

        let bounce_url = "https://cc.zdnet.com/v1/otc/redirect?tracker=123&url=https%3A%2F%2Fexample.com%2Ftarget-product";
        let unwrapped = engine.unwrap_bounce_url(bounce_url);
        assert_eq!(
            unwrapped,
            Some("https://example.com/target-product".to_string())
        );

        let regular_url = "https://example.com/about?team=1";
        assert_eq!(engine.unwrap_bounce_url(regular_url), None);
    }
}
