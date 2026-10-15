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

use serde::{Deserialize, Serialize};

/// Search engine provider metadata containing URL templates.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SearchProvider {
    /// Human-readable search engine name.
    pub name: String,
    /// Query formatting template with `%s` or `{searchTerms}` placeholder.
    pub query_template: String,
    /// Query suggestions API endpoint template.
    pub suggestion_template: Option<String>,
}

/// Address bar query parser and privacy search routing engine.
pub struct SearchEngine;

impl SearchEngine {
    /// Transforms raw address-bar input into a navigable destination URL.
    ///
    /// - Preserves explicitly specified `http://` or `https://` URLs.
    /// - Automatically prepends `https://` for valid domain-like strings without spaces.
    /// - Routes keywords and queries to the sovereign Azokle Search endpoint.
    pub fn build_query_url(query: &str) -> String {
        let trimmed = query.trim();
        if trimmed.starts_with("http://") || trimmed.starts_with("https://") {
            return trimmed.to_string();
        }

        if trimmed.contains('.') && !trimmed.contains(' ') {
            return format!("https://{}", trimmed);
        }

        // Sovereign Azokle Search URL
        format!(
            "https://search.azokle.com/search?q={}",
            urlencoding_simple(trimmed)
        )
    }

    /// Resolves standard bang prefix commands (e.g. `!gh`, `!g`, `!w`, `!yt`) to target services.
    pub fn resolve_bang(query: &str) -> Option<String> {
        let trimmed = query.trim();
        if let Some(rest) = trimmed.strip_prefix("!g ") {
            Some(format!(
                "https://www.google.com/search?q={}",
                urlencoding_simple(rest)
            ))
        } else if let Some(rest) = trimmed.strip_prefix("!gh ") {
            Some(format!(
                "https://github.com/search?q={}",
                urlencoding_simple(rest)
            ))
        } else if let Some(rest) = trimmed.strip_prefix("!w ") {
            Some(format!(
                "https://en.wikipedia.org/wiki/Special:Search?search={}",
                urlencoding_simple(rest)
            ))
        } else {
            trimmed.strip_prefix("!yt ").map(|rest| {
                format!(
                    "https://www.youtube.com/results?search_query={}",
                    urlencoding_simple(rest)
                )
            })
        }
    }
}

/// Performs percent-encoding for search query parameters.
fn urlencoding_simple(s: &str) -> String {
    url::form_urlencoded::byte_serialize(s.as_bytes()).collect::<String>()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_search_resolution() {
        assert_eq!(
            SearchEngine::build_query_url("rustlang"),
            "https://search.azokle.com/search?q=rustlang"
        );
        assert_eq!(
            SearchEngine::build_query_url("rust & tokio? #1"),
            "https://search.azokle.com/search?q=rust+%26+tokio%3F+%231"
        );
        assert_eq!(
            SearchEngine::build_query_url("github.com"),
            "https://github.com"
        );
        assert_eq!(
            SearchEngine::build_query_url("https://example.com"),
            "https://example.com"
        );
        assert_eq!(
            SearchEngine::resolve_bang("!gh tokio").unwrap(),
            "https://github.com/search?q=tokio"
        );
    }
}
