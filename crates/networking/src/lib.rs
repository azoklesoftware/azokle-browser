pub mod client;
pub mod dns;
pub mod error;
pub mod proxy;
pub mod security;
pub mod services;
pub mod types;

pub use client::*;
pub use dns::*;
pub use error::*;
pub use proxy::*;
pub use security::*;
pub use services::*;
pub use types::*;

use serde::{Deserialize, Serialize};
use std::sync::Arc;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ParsedWebPage {
    pub url: String,
    pub title: String,
    pub status_code: u16,
    pub headers: Vec<String>,
    pub headings: Vec<String>,
    pub paragraphs: Vec<String>,
    pub links: Vec<(String, String)>, // (label, url)
    pub raw_html_snippet: String,
}

pub struct HttpClient;

impl HttpClient {
    pub fn fetch_url(url: &str, is_tor: bool) -> Result<ParsedWebPage, String> {
        let client = SovereignHttpClient::default();
        let mut req = HttpRequest::get(url);
        if is_tor {
            req.route = NetworkRoute::TorIsolated { socks_port: 9050 };
        }
        let response = client.execute(req).map_err(|e| e.to_string())?;
        let body = response.body_as_string();
        Ok(HtmlExtractor::parse_html(url, &body))
    }
}

pub struct HtmlExtractor;

impl HtmlExtractor {
    pub fn parse_html(url: &str, html: &str) -> ParsedWebPage {
        let mut title = "Web Document".to_string();
        if let Some(start) = html.find("<title>") {
            if let Some(end) = html[start + 7..].find("</title>") {
                title = html[start + 7..start + 7 + end].trim().to_string();
            }
        } else if let Some(start) = html.find("<TITLE>") {
            if let Some(end) = html[start + 7..].find("</TITLE>") {
                title = html[start + 7..start + 7 + end].trim().to_string();
            }
        }

        let mut headings = Vec::new();
        let mut paragraphs = Vec::new();
        let mut links = Vec::new();

        // Extract H1 / H2
        for tag in &["<h1", "<h2", "<H1", "<H2"] {
            let mut pos = 0;
            while let Some(idx) = html[pos..].find(tag) {
                let actual_idx = pos + idx;
                if let Some(close_tag) = html[actual_idx..].find('>') {
                    let text_start = actual_idx + close_tag + 1;
                    if let Some(end_tag) = html[text_start..].find('<') {
                        let text = html[text_start..text_start + end_tag].trim();
                        if !text.is_empty() && text.len() < 120 {
                            headings.push(Self::clean_text(text));
                        }
                    }
                }
                pos = actual_idx + 4;
                if headings.len() >= 6 {
                    break;
                }
            }
        }

        // Extract Paragraphs / Text Blocks
        let mut pos = 0;
        while let Some(idx) = html[pos..].find("<p") {
            let actual_idx = pos + idx;
            if let Some(close_tag) = html[actual_idx..].find('>') {
                let text_start = actual_idx + close_tag + 1;
                if let Some(end_tag) = html[text_start..].find("</p>") {
                    let text = html[text_start..text_start + end_tag].trim();
                    let clean = Self::clean_text(text);
                    if clean.len() > 15 {
                        paragraphs.push(clean);
                    }
                }
            }
            pos = actual_idx + 3;
            if paragraphs.len() >= 8 {
                break;
            }
        }

        // Extract Links <a href="...">
        let mut pos = 0;
        while let Some(idx) = html[pos..].find("<a ") {
            let actual_idx = pos + idx;
            if let Some(href_idx) = html[actual_idx..].find("href=\"") {
                let link_start = actual_idx + href_idx + 6;
                if let Some(link_end) = html[link_start..].find('"') {
                    let link_url = &html[link_start..link_start + link_end];
                    if link_url.starts_with("http") {
                        let close_tag = html[link_start + link_end..].find('>').unwrap_or(0);
                        let text_start = link_start + link_end + close_tag + 1;
                        let text_end = html[text_start..].find("</a>").unwrap_or(0);
                        let label = html[text_start..text_start + text_end].trim();
                        if !label.is_empty() {
                            links.push((Self::clean_text(label), link_url.to_string()));
                        }
                    }
                }
            }
            pos = actual_idx + 3;
            if links.len() >= 12 {
                break;
            }
        }

        let raw_snippet = if html.len() > 500 {
            html[..500].to_string()
        } else {
            html.to_string()
        };

        ParsedWebPage {
            url: url.to_string(),
            title,
            status_code: 200,
            headers: Vec::new(),
            headings,
            paragraphs,
            links,
            raw_html_snippet: raw_snippet,
        }
    }

    fn clean_text(text: &str) -> String {
        let no_tags = text
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'");
        no_tags.split_whitespace().collect::<Vec<_>>().join(" ")
    }
}

/// Sovereign Networking Engine Singleton / Core Coordinator.
pub struct SovereignNetworkingEngine {
    pub dns: Arc<SovereignDnsResolver>,
    pub proxy: Arc<ProxyRouter>,
    pub http: Arc<SovereignHttpClient>,
    pub search_api: Arc<SearchApiClient>,
}

impl Default for SovereignNetworkingEngine {
    fn default() -> Self {
        Self::new()
    }
}

impl SovereignNetworkingEngine {
    pub fn new() -> Self {
        let dns = Arc::new(SovereignDnsResolver::new());
        let proxy = Arc::new(ProxyRouter::new());
        let http = Arc::new(SovereignHttpClient::new(dns.clone()));
        let search_api = Arc::new(SearchApiClient::new(
            http.clone(),
            "https://search.azokle.com",
        ));

        Self {
            dns,
            proxy,
            http,
            search_api,
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_html_parsing() {
        let html = "<html><head><title>Test Page</title></head><body><h1>Main Heading</h1><p>This is a paragraph with sufficient length for test.</p><a href=\"https://azokle.com\">Azokle Search</a></body></html>";
        let parsed = HtmlExtractor::parse_html("https://example.com", html);
        assert_eq!(parsed.title, "Test Page");
        assert_eq!(parsed.headings.len(), 1);
        assert_eq!(parsed.headings[0], "Main Heading");
        assert_eq!(parsed.links.len(), 1);
        assert_eq!(parsed.links[0].1, "https://azokle.com");
    }

    #[test]
    fn test_networking_engine_init() {
        let engine = SovereignNetworkingEngine::new();
        assert_eq!(engine.dns.get_provider(), DnsProvider::Cloudflare);
    }
}
