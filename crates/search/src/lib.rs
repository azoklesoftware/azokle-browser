use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SearchProvider {
    pub name: String,
    pub query_template: String,
    pub suggestion_template: Option<String>,
}

pub struct SearchEngine;

impl SearchEngine {
    pub fn build_query_url(query: &str) -> String {
        let trimmed = query.trim();
        if trimmed.starts_with("http://") || trimmed.starts_with("https://") {
            return trimmed.to_string();
        }

        if trimmed.contains('.') && !trimmed.contains(' ') {
            return format!("https://{}", trimmed);
        }

        // Sovereign Azokle Search URL
        format!("https://search.azokle.com/search?q={}", urlencoding_simple(trimmed))
    }

    pub fn resolve_bang(query: &str) -> Option<String> {
        let trimmed = query.trim();
        if let Some(rest) = trimmed.strip_prefix("!g ") {
            Some(format!("https://www.google.com/search?q={}", urlencoding_simple(rest)))
        } else if let Some(rest) = trimmed.strip_prefix("!gh ") {
            Some(format!("https://github.com/search?q={}", urlencoding_simple(rest)))
        } else if let Some(rest) = trimmed.strip_prefix("!w ") {
            Some(format!("https://en.wikipedia.org/wiki/Special:Search?search={}", urlencoding_simple(rest)))
        } else if let Some(rest) = trimmed.strip_prefix("!yt ") {
            Some(format!("https://www.youtube.com/results?search_query={}", urlencoding_simple(rest)))
        } else {
            None
        }
    }
}

fn urlencoding_simple(s: &str) -> String {
    s.replace(' ', "+")
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_search_resolution() {
        assert_eq!(SearchEngine::build_query_url("rustlang"), "https://search.azokle.com/search?q=rustlang");
        assert_eq!(SearchEngine::build_query_url("github.com"), "https://github.com");
        assert_eq!(SearchEngine::build_query_url("https://example.com"), "https://example.com");
        assert_eq!(SearchEngine::resolve_bang("!gh tokio").unwrap(), "https://github.com/search?q=tokio");
    }
}
