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

    /// Returns the route for request.
    pub fn get_route_for_request(&self, url: &str, is_private_tab: bool) -> NetworkRoute {
        let clean = url.trim().to_lowercase();
        let guard = self.config.read();

        // 1. Any .onion URL always routes through Tor SOCKS5 (Fail-Closed)
        if clean.contains(".onion") {
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

        format!(
            r#"function FindProxyForURL(url, host) {{
    if (shExpMatch(host, "*.onion") || shExpMatch(url, "*.onion/*")) {{
        return "SOCKS5 127.0.0.1:{}; SOCKS 127.0.0.1:{}";
    }}
    if ({}) {{
        return "SOCKS5 127.0.0.1:{}; DIRECT";
    }}
    return "DIRECT";
}}"#,
            port, port, is_tor, port
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

        // .onion always gets TorIsolated even if global Tor is off
        let route = router.get_route_for_request(
            "http://duckduckgogg42xjoc72x3sjasowoarfbgcmvfimaftt6twagswzczad.onion",
            false,
        );
        assert_eq!(route, NetworkRoute::TorIsolated { socks_port: 9050 });

        // Regular site gets Direct when Tor is off
        let reg_route = router.get_route_for_request("https://search.azokle.com", false);
        assert_eq!(reg_route, NetworkRoute::Direct);
    }

    #[test]
    fn test_proxy_pac_script() {
        let router = ProxyRouter::new();
        router.set_tor_mode(true, 9050);
        let pac = router.generate_pac_script();
        assert!(pac.contains("FindProxyForURL"));
        assert!(pac.contains("127.0.0.1:9050"));
    }
}
