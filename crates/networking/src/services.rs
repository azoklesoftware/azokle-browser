use crate::client::SovereignHttpClient;
use crate::error::NetworkError;
use crate::types::HttpRequest;
use serde::{Deserialize, Serialize};
use std::sync::Arc;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SearchResultItem {
    pub title: String,
    pub url: String,
    pub snippet: String,
    pub engine: String,
    pub thumbnail: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SearchApiResponse {
    pub query: String,
    pub page: u32,
    pub results: Vec<SearchResultItem>,
    pub total_results: u64,
    pub search_time: f64,
    pub error: Option<String>,
}

#[derive(Clone)]
pub struct SearchApiClient {
    http: Arc<SovereignHttpClient>,
    base_url: String,
}

impl Default for SearchApiClient {
    fn default() -> Self {
        Self::new(
            Arc::new(SovereignHttpClient::default()),
            "https://search.azokle.com",
        )
    }
}

impl SearchApiClient {
    pub fn new(http: Arc<SovereignHttpClient>, base_url: &str) -> Self {
        Self {
            http,
            base_url: base_url.trim_end_matches('/').to_string(),
        }
    }

    /// Fetches live auto-suggestions for the omnibar.
    pub fn get_autosuggestions(&self, query: &str) -> Result<Vec<String>, NetworkError> {
        let clean = query.trim();
        if clean.len() < 2 {
            return Ok(Vec::new());
        }

        let body_json = serde_json::json!({ "query": clean }).to_string();
        let mut req = HttpRequest::post(
            &format!("{}/api/v1/autosuggest", self.base_url),
            body_json.into_bytes(),
        );
        req.timeout = std::time::Duration::from_secs(4);

        let response = self.http.execute(req)?;
        if !response.is_success() {
            return Ok(Vec::new());
        }

        let body_str = response.body_as_string();
        let parsed: serde_json::Value = serde_json::from_str(&body_str)
            .map_err(|e| NetworkError::SerializationError(e.to_string()))?;

        let mut suggestions = Vec::new();
        if let Some(arr) = parsed.get("suggestions").and_then(|v| v.as_array()) {
            for item in arr {
                if let Some(s) = item.as_str() {
                    suggestions.push(s.to_string());
                }
            }
        }
        Ok(suggestions)
    }

    /// Executes full Azokle Search API query.
    pub fn execute_search(
        &self,
        query: &str,
        page: u32,
    ) -> Result<SearchApiResponse, NetworkError> {
        let clean = query.trim();
        let body_json = serde_json::json!({
            "query": clean,
            "page": page
        })
        .to_string();

        let mut req = HttpRequest::post(
            &format!("{}/api/v1/search", self.base_url),
            body_json.into_bytes(),
        );
        req.timeout = std::time::Duration::from_secs(8);

        let response = self.http.execute(req)?;
        let body_str = response.body_as_string();

        let parsed: SearchApiResponse =
            serde_json::from_str(&body_str).unwrap_or_else(|_| SearchApiResponse {
                query: clean.to_string(),
                page,
                results: Vec::new(),
                total_results: 0,
                search_time: 0.0,
                error: Some("Failed to parse search results".to_string()),
            });

        Ok(parsed)
    }
}
