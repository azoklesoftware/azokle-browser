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

use thiserror::Error;

#[derive(Error, Debug, Clone, PartialEq, Eq)]
pub enum NetworkError {
    #[error("Invalid URL: {0}")]
    InvalidUrl(String),

    #[error("DNS resolution failed: {0}")]
    DnsError(String),

    #[error("DNS leak detected: prohibited direct lookup of {0}")]
    DnsLeakPrevented(String),

    #[error("SSRF protection blocked request to private/loopback address: {0}")]
    SsrfBlocked(String),

    #[error("Proxy unreachable or failed handshake on {0}")]
    ProxyUnreachable(String),

    #[error("Connection timed out after {0:?}")]
    Timeout(std::time::Duration),

    #[error("Connection failed: {0}")]
    ConnectionFailed(String),

    #[error("TLS handshake failed: {0}")]
    TlsError(String),

    #[error("HTTP error: status code {0}")]
    HttpStatus(u16),

    #[error("Too many redirects (max {0})")]
    TooManyRedirects(usize),

    #[error("Insecure redirect from HTTPS to HTTP blocked: {0}")]
    InsecureRedirectBlocked(String),

    #[error("Serialization / Deserialization error: {0}")]
    SerializationError(String),

    #[error("Network I/O error: {0}")]
    IoError(String),
}
