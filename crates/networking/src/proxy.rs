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

use crate::types::NetworkRoute;
use parking_lot::RwLock;
use serde::{Deserialize, Serialize};
use std::sync::Arc;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ProxyConfig {
    pub default_route: NetworkRoute,
    pub tor_socks_port: u16,
    pub is_tor_enabled: bool,
}

impl Default for ProxyConfig {
    /// Returns the default instance.
    fn default() -> Self {
        Self {
            default_route: NetworkRoute::Direct,
            tor_socks_port: 9050,
            is_tor_enabled: false,
        }
    }
}

#[derive(Clone)]
pub struct ProxyRouter {
    config: Arc<RwLock<ProxyConfig>>,
}

impl Default for ProxyRouter {
    /// Returns the default instance.
    fn default() -> Self {
        Self::new()
    }
}

impl ProxyRouter {
    /// Creates a new instance with default configuration.
    pub fn new() -> Self {
        Self {
            config: Arc::new(RwLock::new(ProxyConfig::default())),
        }
    }

    /// Updates the tor mode.
    pub fn set_tor_mode(&self, enabled: bool, socks_port: u16) {
        let mut guard = self.config.write();
        guard.is_tor_enabled = enabled;
        guard.tor_socks_port = socks_port;
    }

    /// Safely extracts the authority host from a URL string without allocating.
    fn extract_host(url: &str) -> &str {
        let clean = url.trim();
        let without_scheme = if let Some(idx) = clean.find("://") {
            &clean[idx + 3..]
        } else {
            clean
        };
        let host_part = without_scheme
            .split('/')
            .next()
            .unwrap_or("")
            .split('?')
            .next()
            .unwrap_or("")
            .split('#')
            .next()
            .unwrap_or("");
        if host_part.starts_with('[') {
            host_part.split(']').next().unwrap_or("").trim_start_matches('[')
        } else {
            host_part.split(':').next().unwrap_or("")
        }
    }

    /// Returns the route for request.
    pub fn get_route_for_request(&self, url: &str, is_private_tab: bool) -> NetworkRoute {
        let host = Self::extract_host(url).trim_end_matches('.');
        let is_onion = host.eq_ignore_ascii_case("onion") || host.to_lowercase().ends_with(".onion");
        let guard = self.config.read();

        // 1. Authentic .onion host always routes through Tor SOCKS5 (Fail-Closed)
        if is_onion {
            return NetworkRoute::TorIsolated {
                socks_port: guard.tor_socks_port,
            };
        }

        // 2. Tor Mode active in Private/Tor Tab
        if guard.is_tor_enabled && is_private_tab {
            return NetworkRoute::TorIsolated {
                socks_port: guard.tor_socks_port,
            };
        }

        guard.default_route.clone()
    }

    /// Generates standard PAC (Proxy Auto-Config) JavaScript for GeckoView.
    pub fn generate_pac_script(&self) -> String {
        let guard = self.config.read();
        let port = guard.tor_socks_port;
        let is_tor = guard.is_tor_enabled;

        // AZK-HIGH-06 & AZK-VULN-08: Strict fail-closed Tor proxy PAC script.
        // Matches only authentic .onion hostnames and never substring path matches.
        format!(
            r#"function FindProxyForURL(url, host) {{
    var cleanHost = (host || "").toLowerCase();
    if (cleanHost === "onion" || cleanHost.endsWith(".onion") || shExpMatch(cleanHost, "*.onion")) {{
        return "SOCKS5 127.0.0.1:{port}; SOCKS 127.0.0.1:{port}";
    }}
    if ({is_tor}) {{
        return "SOCKS5 127.0.0.1:{port}; SOCKS 127.0.0.1:{port}";
    }}
    return "DIRECT";
}}"#,
            port = port,
            is_tor = is_tor
        )
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_proxy_routing_onion() {
        let router = ProxyRouter::new();
        router.set_tor_mode(false, 9050);

        // Authentic .onion always gets TorIsolated even if global Tor is off
        let route = router.get_route_for_request(
            "http://duckduckgogg42xjoc72x3sjasowoarfbgcmvfimaftt6twagswzczad.onion",
            false,
        );
        assert_eq!(route, NetworkRoute::TorIsolated { socks_port: 9050 });

        // Regular site gets Direct when Tor is off
        let reg_route = router.get_route_for_request("https://search.azokle.com", false);
        assert_eq!(reg_route, NetworkRoute::Direct);

        // AZK-VULN-08: Clearnet URL containing .onion in query/path MUST NOT route to Tor
        let query_onion = router.get_route_for_request(
            "https://search.azokle.com/search?q=something.onion&ref=guide",
            false,
        );
        assert_eq!(query_onion, NetworkRoute::Direct);

        let path_onion = router.get_route_for_request(
            "https://example.com/downloads/tor.onion/manual.pdf",
            false,
        );
        assert_eq!(path_onion, NetworkRoute::Direct);
    }

    #[test]
    fn test_proxy_pac_script() {
        let router = ProxyRouter::new();
        router.set_tor_mode(true, 9050);
        let pac = router.generate_pac_script();
        assert!(pac.contains("FindProxyForURL"));
        assert!(pac.contains("127.0.0.1:9050"));
        // AZK-HIGH-06 regression test: Tor PAC script must NEVER contain fail-open DIRECT fallback
        assert!(!pac.contains("SOCKS5 127.0.0.1:9050; DIRECT"));
        // AZK-VULN-08 regression test: Must match cleanHost, not *.onion/* on url
        assert!(!pac.contains("*.onion/*"));
    }
}

