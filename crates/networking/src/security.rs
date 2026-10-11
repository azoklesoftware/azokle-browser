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

use crate::error::NetworkError;
use std::net::{IpAddr, Ipv4Addr};
use url::Url;

/// Sovereign SSRF & Local Loopback Filter
///
/// Guards against Server-Side Request Forgery (SSRF) and local intranet port-scanning
/// attacks initiated by untrusted web content or malicious redirects.
pub struct SecurityPolicy;

impl SecurityPolicy {
    /// Evaluates whether an IP address belongs to a private network, loopback, or link-local subnet.
    ///
    /// # Protected Subnets:
    /// - IPv4 Loopback: `127.0.0.0/8`
    /// - IPv4 RFC 1918 Private: `10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`
    /// - IPv4 Link-Local & Cloud Metadata: `169.254.0.0/16` (e.g. AWS/GCP `169.254.169.254`)
    /// - IPv4 Broadcast & Unspecified: `255.255.255.255`, `0.0.0.0/32`
    /// - IPv6 Loopback & Unspecified: `::1`, `::`
    /// - IPv6 Unique Local (ULA): `fc00::/7`
    /// - IPv6 Link-Local: `fe80::/10`
    /// - IPv4-mapped/compatible IPv6 addresses (e.g. `::ffff:127.0.0.1`)
    pub fn is_private_or_loopback_ip(ip: &IpAddr) -> bool {
        match ip {
            IpAddr::V4(v4) => {
                // Loopback (127.0.0.0/8)
                v4.is_loopback() ||
                // Private Class A (10.0.0.0/8)
                v4.octets()[0] == 10 ||
                // Private Class B (172.16.0.0/12)
                (v4.octets()[0] == 172 && (16..=31).contains(&v4.octets()[1])) ||
                // Private Class C (192.168.0.0/16)
                (v4.octets()[0] == 192 && v4.octets()[1] == 168) ||
                // Link-local / Cloud metadata (169.254.0.0/16)
                v4.is_link_local() ||
                // Broadcast / Unspecified
                v4.is_broadcast() ||
                v4.is_unspecified() ||
                *v4 == Ipv4Addr::new(0, 0, 0, 0)
            }
            IpAddr::V6(v6) => {
                v6.is_loopback() ||
                v6.is_unspecified() ||
                // Unique Local Addresses (fc00::/7)
                ((v6.segments()[0] & 0xfe00) == 0xfc00) ||
                // Link-Local (fe80::/10)
                ((v6.segments()[0] & 0xffc0) == 0xfe80) ||
                // IPv4-mapped / IPv4-compatible loopback/private (e.g. ::ffff:127.0.0.1 or ::127.0.0.1)
                match v6.to_ipv4_mapped().or_else(|| v6.to_ipv4()) {
                    Some(v4) => Self::is_private_or_loopback_ip(&IpAddr::V4(v4)),
                    None => false,
                }
            }
        }
    }

    /// Validates a candidate request URL against security policies.
    ///
    /// - Rejects non-HTTP(S) schemes.
    /// - If `allow_private` is false, blocks `localhost` domain patterns and private IP addresses.
    pub fn validate_url(url_str: &str, allow_private: bool) -> Result<Url, NetworkError> {
        let parsed = Url::parse(url_str).map_err(|e| NetworkError::InvalidUrl(e.to_string()))?;

        // Scheme validation
        let scheme = parsed.scheme().to_lowercase();
        if scheme != "http" && scheme != "https" {
            return Err(NetworkError::InvalidUrl(format!(
                "Unsupported scheme: {}",
                scheme
            )));
        }

        if !allow_private {
            if let Some(host) = parsed.host_str() {
                let host_clean = host.trim().to_lowercase();
                if host_clean == "localhost"
                    || host_clean.ends_with(".localhost")
                    || host_clean == "127.0.0.1"
                    || host_clean == "::1"
                {
                    return Err(NetworkError::SsrfBlocked(format!(
                        "Access to localhost ({}) is prohibited",
                        host_clean
                    )));
                }

                // If host is a raw IP literal
                if let Ok(ip) = host_clean.parse::<IpAddr>() {
                    if Self::is_private_or_loopback_ip(&ip) {
                        return Err(NetworkError::SsrfBlocked(format!(
                            "Access to private/loopback IP ({}) is prohibited",
                            ip
                        )));
                    }
                }
            }
        }

        Ok(parsed)
    }

    /// Validates HTTP redirect targets to block insecure scheme downgrades (`https://` to `http://`).
    pub fn validate_redirect(from: &Url, to: &Url) -> Result<(), NetworkError> {
        if from.scheme() == "https" && to.scheme() == "http" {
            return Err(NetworkError::InsecureRedirectBlocked(format!(
                "Refusing insecure redirect downgrade from {} to {}",
                from, to
            )));
        }
        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_ssrf_filter() {
        assert!(SecurityPolicy::is_private_or_loopback_ip(
            &"127.0.0.1".parse().unwrap()
        ));
        assert!(SecurityPolicy::is_private_or_loopback_ip(
            &"10.0.0.1".parse().unwrap()
        ));
        assert!(SecurityPolicy::is_private_or_loopback_ip(
            &"172.20.0.1".parse().unwrap()
        ));
        assert!(SecurityPolicy::is_private_or_loopback_ip(
            &"192.168.1.1".parse().unwrap()
        ));
        assert!(SecurityPolicy::is_private_or_loopback_ip(
            &"169.254.169.254".parse().unwrap()
        ));
        assert!(SecurityPolicy::is_private_or_loopback_ip(
            &"::1".parse().unwrap()
        ));
        assert!(SecurityPolicy::is_private_or_loopback_ip(
            &"::ffff:127.0.0.1".parse().unwrap()
        ));
        assert!(SecurityPolicy::is_private_or_loopback_ip(
            &"::ffff:10.0.0.1".parse().unwrap()
        ));

        assert!(!SecurityPolicy::is_private_or_loopback_ip(
            &"8.8.8.8".parse().unwrap()
        ));
        assert!(!SecurityPolicy::is_private_or_loopback_ip(
            &"::ffff:8.8.8.8".parse().unwrap()
        ));
        assert!(!SecurityPolicy::is_private_or_loopback_ip(
            &"1.1.1.1".parse().unwrap()
        ));
        assert!(!SecurityPolicy::is_private_or_loopback_ip(
            &"104.21.5.12".parse().unwrap()
        ));
    }

    #[test]
    fn test_validate_url_ssrf() {
        assert!(SecurityPolicy::validate_url("http://127.0.0.1:8080", false).is_err());
        assert!(SecurityPolicy::validate_url("http://localhost/admin", false).is_err());
        assert!(SecurityPolicy::validate_url("http://192.168.0.1/status", false).is_err());
        assert!(SecurityPolicy::validate_url("https://search.azokle.com", false).is_ok());
    }

    #[test]
    fn test_redirect_scheme_downgrade() {
        let from_https = Url::parse("https://example.com/login").unwrap();
        let to_http = Url::parse("http://example.com/insecure").unwrap();
        assert!(SecurityPolicy::validate_redirect(&from_https, &to_http).is_err());

        let from_http = Url::parse("http://example.com/page").unwrap();
        let to_https = Url::parse("https://example.com/page").unwrap();
        assert!(SecurityPolicy::validate_redirect(&from_http, &to_https).is_ok());
    }
}
