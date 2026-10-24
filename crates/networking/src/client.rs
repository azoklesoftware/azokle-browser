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

use crate::dns::SovereignDnsResolver;
use crate::error::NetworkError;
use crate::security::SecurityPolicy;
use crate::types::{HttpMethod, HttpRequest, HttpResponse, NetworkRoute};
use std::collections::HashMap;
use std::io::{Read, Write};
use std::net::{SocketAddr, TcpStream};
use std::sync::Arc;
use std::time::Instant;

#[derive(Clone)]
pub struct SovereignHttpClient {
    dns: Arc<SovereignDnsResolver>,
    user_agent: String,
}

impl Default for SovereignHttpClient {
    /// Returns the default instance.
    fn default() -> Self {
        Self::new(Arc::new(SovereignDnsResolver::new()))
    }
}

impl SovereignHttpClient {
    /// Creates a new instance with default configuration.
    pub fn new(dns: Arc<SovereignDnsResolver>) -> Self {
        Self {
            dns,
            user_agent: "Mozilla/5.0 (Android; Mobile; rv:128.0) Gecko/128.0 Firefox/128.0 (Azokle Sovereign)".to_string(),
        }
    }

    /// Pure-Rust SOCKS5 RFC 1928 handshake
    fn socks5_connect(
        proxy_addr: SocketAddr,
        target_host: &str,
        target_port: u16,
        timeout: std::time::Duration,
    ) -> Result<TcpStream, NetworkError> {
        let mut stream = TcpStream::connect_timeout(&proxy_addr, timeout).map_err(|e| {
            NetworkError::ProxyUnreachable(format!(
                "Failed to connect to SOCKS5 proxy at {}: {}",
                proxy_addr, e
            ))
        })?;

        stream.set_read_timeout(Some(timeout)).ok();
        stream.set_write_timeout(Some(timeout)).ok();

        // 1. Auth negotiation: VER=5, NMETHODS=1, METHOD=0 (No Auth)
        stream.write_all(&[0x05, 0x01, 0x00]).map_err(|e| {
            NetworkError::ProxyUnreachable(format!("SOCKS5 auth write failed: {}", e))
        })?;

        let mut auth_resp = [0u8; 2];
        stream.read_exact(&mut auth_resp).map_err(|e| {
            NetworkError::ProxyUnreachable(format!("SOCKS5 auth read failed: {}", e))
        })?;

        if auth_resp[0] != 0x05 || auth_resp[1] != 0x00 {
            return Err(NetworkError::ProxyUnreachable(
                "SOCKS5 proxy rejected authentication".to_string(),
            ));
        }

        // 2. Connect request: VER=5, CMD=1 (Connect), RSV=0, ATYP=3 (Domain name)
        let host_bytes = target_host.as_bytes();
        if host_bytes.len() > 255 {
            return Err(NetworkError::InvalidUrl(
                "Host length exceeds 255 bytes".to_string(),
            ));
        }

        let mut req = Vec::with_capacity(7 + host_bytes.len());
        req.extend_from_slice(&[0x05, 0x01, 0x00, 0x03, host_bytes.len() as u8]);
        req.extend_from_slice(host_bytes);
        req.extend_from_slice(&target_port.to_be_bytes());

        stream.write_all(&req).map_err(|e| {
            NetworkError::ProxyUnreachable(format!("SOCKS5 connect request failed: {}", e))
        })?;

        // 3. Response header: VER=5, REP=0 (Success), RSV=0, ATYP=...
        let mut resp_hdr = [0u8; 4];
        stream.read_exact(&mut resp_hdr).map_err(|e| {
            NetworkError::ProxyUnreachable(format!("SOCKS5 connect response read failed: {}", e))
        })?;

        if resp_hdr[0] != 0x05 || resp_hdr[1] != 0x00 {
            return Err(NetworkError::ProxyUnreachable(format!(
                "SOCKS5 connect failed with reply code: {:#x}",
                resp_hdr[1]
            )));
        }

        // Consume remaining bind address in SOCKS5 response
        match resp_hdr[3] {
            0x01 => {
                // IPv4 (4 bytes) + Port (2 bytes)
                let mut dummy = [0u8; 6];
                stream.read_exact(&mut dummy).ok();
            }
            0x03 => {
                // Domain (1 len byte + len bytes) + Port (2 bytes)
                let mut len = [0u8; 1];
                if stream.read_exact(&mut len).is_ok() {
                    let mut dummy = vec![0u8; len[0] as usize + 2];
                    stream.read_exact(&mut dummy).ok();
                }
            }
            0x04 => {
                // IPv6 (16 bytes) + Port (2 bytes)
                let mut dummy = [0u8; 18];
                stream.read_exact(&mut dummy).ok();
            }
            _ => {}
        }

        Ok(stream)
    }

    /// Executes an HTTP request with SSRF validation, DNS resolution, and pure-Rust SOCKS5 proxy routing.
    pub fn execute(&self, request: HttpRequest) -> Result<HttpResponse, NetworkError> {
        let start_time = Instant::now();

        // 1. SSRF & URL validation
        let parsed_url = SecurityPolicy::validate_url(&request.url, request.allow_private_ips)?;
        let host = parsed_url
            .host_str()
            .ok_or_else(|| NetworkError::InvalidUrl("Missing host in URL".to_string()))?;

        // Fail closed on HTTPS: raw TCP socket cannot safely negotiate TLS.
        // Plaintext transmission over port 443 violates transport security guarantees.
        if parsed_url.scheme() == "https" {
            return Err(NetworkError::TlsError(
                "Plaintext socket cannot negotiate HTTPS. TLS is not configured for this raw socket client.".to_string(),
            ));
        }

        let port = parsed_url.port().unwrap_or(80);

        // 2. Establish connection based on routing
        let mut stream = match request.route {
            NetworkRoute::TorIsolated { socks_port } => {
                let proxy_addr = SocketAddr::from(([127, 0, 0, 1], socks_port));
                Self::socks5_connect(proxy_addr, host, port, request.timeout)?
            }
            NetworkRoute::ProxySocks5 {
                host: ref p_host,
                port: p_port,
            } => {
                let resolved_proxy_ips = self.dns.resolve(p_host)?;
                let proxy_ip = resolved_proxy_ips.first().ok_or_else(|| {
                    NetworkError::ProxyUnreachable("Proxy IP resolution failed".to_string())
                })?;
                if !request.allow_private_ips
                    && crate::security::SecurityPolicy::is_private_or_loopback_ip(proxy_ip)
                {
                    return Err(NetworkError::SsrfBlocked(format!(
                        "Proxy IP ({}) for proxy host '{}' is a prohibited private/loopback address",
                        proxy_ip, p_host
                    )));
                }
                let proxy_addr = SocketAddr::new(*proxy_ip, p_port);
                Self::socks5_connect(proxy_addr, host, port, request.timeout)?
            }
            NetworkRoute::Direct => {
                let is_onion =
                    host.eq_ignore_ascii_case("onion") || host.to_lowercase().ends_with(".onion");
                if !is_onion {
                    let ips = self.dns.resolve(host)?;
                    let target_ip = ips
                        .first()
                        .ok_or_else(|| NetworkError::DnsError(host.to_string()))?;

                    // AZK-SEC-03: Validate all resolved IPs against SSRF policy before connection to prevent DNS rebinding attacks
                    if !request.allow_private_ips {
                        for ip in &ips {
                            if crate::security::SecurityPolicy::is_private_or_loopback_ip(ip) {
                                return Err(NetworkError::SsrfBlocked(format!(
                                    "Resolved IP ({}) for host '{}' is a prohibited private/loopback address (DNS rebinding prevented)",
                                    ip, host
                                )));
                            }
                        }
                    }

                    let target_addr = SocketAddr::new(*target_ip, port);
                    let s =
                        TcpStream::connect_timeout(&target_addr, request.timeout).map_err(|e| {
                            NetworkError::ConnectionFailed(format!(
                                "Direct connection failed to {}: {}",
                                target_addr, e
                            ))
                        })?;
                    s.set_read_timeout(Some(request.timeout)).ok();
                    s.set_write_timeout(Some(request.timeout)).ok();
                    s
                } else {
                    // .onion directly requires Tor routing
                    return Err(NetworkError::DnsLeakPrevented(format!("Direct resolution of .onion address ({}) is prohibited. Route through Tor.", host)));
                }
            }
        };

        // 3. Format HTTP/1.1 Wire Request
        let method_str = match request.method {
            HttpMethod::Get => "GET",
            HttpMethod::Post => "POST",
            HttpMethod::Put => "PUT",
            HttpMethod::Delete => "DELETE",
            HttpMethod::Head => "HEAD",
            HttpMethod::Options => "OPTIONS",
            HttpMethod::Patch => "PATCH",
        };

        let path = if parsed_url.path().is_empty() {
            "/"
        } else {
            parsed_url.path()
        };
        let path_and_query = match parsed_url.query() {
            Some(q) => format!("{}?{}", path, q),
            None => path.to_string(),
        };

        let mut http_req = format!(
            "{} {} HTTP/1.1\r\nHost: {}\r\nUser-Agent: {}\r\nConnection: close\r\n",
            method_str, path_and_query, host, self.user_agent
        );

        for (k, v) in &request.headers {
            if k.contains('\r') || k.contains('\n') || v.contains('\r') || v.contains('\n') {
                return Err(NetworkError::InvalidUrl(format!(
                    "CRLF injection detected in header '{}: {}'",
                    k, v
                )));
            }
            http_req.push_str(&format!("{}: {}\r\n", k, v));
        }

        if let Some(ref body) = request.body {
            http_req.push_str(&format!("Content-Length: {}\r\n", body.len()));
        }

        http_req.push_str("\r\n");

        // 4. Send Request
        stream
            .write_all(http_req.as_bytes())
            .map_err(|e| NetworkError::ConnectionFailed(format!("Write failed: {}", e)))?;

        if let Some(ref body) = request.body {
            stream
                .write_all(body)
                .map_err(|e| NetworkError::ConnectionFailed(format!("Body write failed: {}", e)))?;
        }

        // 5. Read Response with upper bound (10 MB) to prevent OOM DOS
        const MAX_RESPONSE_BYTES: u64 = 10 * 1024 * 1024;
        let mut raw_response = Vec::new();
        (&mut stream)
            .take(MAX_RESPONSE_BYTES)
            .read_to_end(&mut raw_response)
            .map_err(|e| NetworkError::ConnectionFailed(format!("Read failed: {}", e)))?;

        let latency = start_time.elapsed();

        // 6. Parse Status Line, Headers, and Body
        let mut response_headers = HashMap::new();
        let (status, body) =
            if let Some(pos) = raw_response.windows(4).position(|w| w == b"\r\n\r\n") {
                let header_part = String::from_utf8_lossy(&raw_response[..pos]);
                let mut lines = header_part.lines();
                let status_code = lines
                    .next()
                    .and_then(|line| line.split_whitespace().nth(1))
                    .and_then(|code| code.parse::<u16>().ok())
                    .unwrap_or(200);

                for line in lines {
                    if let Some((name, val)) = line.split_once(':') {
                        response_headers.insert(name.trim().to_string(), val.trim().to_string());
                    }
                }

                let body_part = raw_response[(pos + 4)..].to_vec();
                (status_code, body_part)
            } else {
                (200, raw_response)
            };

        Ok(HttpResponse {
            status,
            headers: response_headers,
            body,
            latency,
            remote_addr: None,
        })
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_https_scheme_fails_closed_without_tls() {
        let client = SovereignHttpClient::default();
        let req = HttpRequest::get("https://example.com/api");
        let result = client.execute(req);
        match result {
            Err(NetworkError::TlsError(msg)) => {
                assert!(msg.contains("Plaintext socket cannot negotiate HTTPS"));
            }
            other => panic!("Expected TlsError, got {:?}", other),
        }
    }

    #[test]
    fn test_crlf_header_injection_rejected() {
        let client = SovereignHttpClient::default();
        let mut req = HttpRequest::get("http://example.com/");
        req.headers.insert(
            "X-Inject".to_string(),
            "foo\r\nInjected-Header: evil".to_string(),
        );
        let result = client.execute(req);
        match result {
            Err(NetworkError::InvalidUrl(msg)) => {
                assert!(msg.contains("CRLF injection detected"));
            }
            other => panic!(
                "Expected InvalidUrl error for CRLF injection, got {:?}",
                other
            ),
        }
    }

    #[test]
    fn test_onion_direct_resolution_blocked() {
        let client = SovereignHttpClient::default();
        let req = HttpRequest::get(
            "http://duckduckgogg42xjoc72x3sjasowoarfbgcmvfimaftt6twagswzczad.onion/test",
        );
        let result = client.execute(req);
        match result {
            Err(NetworkError::DnsLeakPrevented(msg)) => {
                assert!(msg.contains("prohibited"));
            }
            other => panic!("Expected DnsLeakPrevented, got {:?}", other),
        }
    }

    #[test]
    fn test_resolved_ip_ssrf_blocked() {
        let dns = Arc::new(SovereignDnsResolver::new());
        // AZK-SEC-03: Simulate DNS rebinding: public-looking hostname resolving to loopback IP
        dns.insert_cache_for_test(
            "rebind.attacker.test",
            vec!["127.0.0.1".parse().unwrap()],
        );
        let client = SovereignHttpClient::new(dns);
        let req = HttpRequest::get("http://rebind.attacker.test/");
        let result = client.execute(req);
        match result {
            Err(NetworkError::SsrfBlocked(msg)) => {
                assert!(msg.contains("prohibited private/loopback address"));
            }
            other => panic!("Expected SsrfBlocked, got {:?}", other),
        }
    }
}
