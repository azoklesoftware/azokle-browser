use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::net::SocketAddr;
use std::time::Duration;

#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum HttpMethod {
    Get,
    Post,
    Put,
    Delete,
    Head,
    Options,
    Patch,
}

impl HttpMethod {
    pub fn as_str(&self) -> &'static str {
        match self {
            Self::Get => "GET",
            Self::Post => "POST",
            Self::Put => "PUT",
            Self::Delete => "DELETE",
            Self::Head => "HEAD",
            Self::Options => "OPTIONS",
            Self::Patch => "PATCH",
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum NetworkRoute {
    Direct,
    ProxySocks5 { host: String, port: u16 },
    TorIsolated { socks_port: u16 },
}

impl Default for NetworkRoute {
    fn default() -> Self {
        Self::Direct
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct HttpRequest {
    pub url: String,
    pub method: HttpMethod,
    pub headers: HashMap<String, String>,
    pub body: Option<Vec<u8>>,
    pub timeout: Duration,
    pub route: NetworkRoute,
    pub follow_redirects: bool,
    pub max_redirects: usize,
    pub allow_private_ips: bool,
}

impl HttpRequest {
    pub fn get(url: &str) -> Self {
        Self {
            url: url.to_string(),
            method: HttpMethod::Get,
            headers: HashMap::new(),
            body: None,
            timeout: Duration::from_secs(10),
            route: NetworkRoute::Direct,
            follow_redirects: true,
            max_redirects: 5,
            allow_private_ips: false,
        }
    }

    pub fn post(url: &str, body: Vec<u8>) -> Self {
        let mut headers = HashMap::new();
        headers.insert("Content-Type".to_string(), "application/json".to_string());
        Self {
            url: url.to_string(),
            method: HttpMethod::Post,
            headers,
            body: Some(body),
            timeout: Duration::from_secs(10),
            route: NetworkRoute::Direct,
            follow_redirects: true,
            max_redirects: 5,
            allow_private_ips: false,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct HttpResponse {
    pub status: u16,
    pub headers: HashMap<String, String>,
    pub body: Vec<u8>,
    pub latency: Duration,
    pub remote_addr: Option<SocketAddr>,
}

impl HttpResponse {
    pub fn is_success(&self) -> bool {
        self.status >= 200 && self.status < 300
    }

    pub fn body_as_string(&self) -> String {
        String::from_utf8_lossy(&self.body).to_string()
    }
}
