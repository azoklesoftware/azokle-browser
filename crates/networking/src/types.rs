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
    /// Performs as str operation.
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

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize, Default)]
pub enum NetworkRoute {
    #[default]
    Direct,
    ProxySocks5 {
        host: String,
        port: u16,
    },
    TorIsolated {
        socks_port: u16,
    },
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
    /// Performs get operation.
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

    /// Performs post operation.
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
    /// Checks whether success is satisfied.
    pub fn is_success(&self) -> bool {
        self.status >= 200 && self.status < 300
    }

    /// Performs body as string operation.
    pub fn body_as_string(&self) -> String {
        String::from_utf8_lossy(&self.body).to_string()
    }
}
