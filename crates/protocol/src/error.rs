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
use thiserror::Error;

#[derive(Error, Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum ProtocolError {
    // General & Protocol Errors (1000..1999)
    #[error("[1001] Incompatible protocol version: requested {0}, supported {1}")]
    IncompatibleVersion(String, String),

    #[error("[1002] Invalid envelope: {0}")]
    InvalidEnvelope(String),

    #[error("[1003] Request timed out after {0} ms")]
    Timeout(u64),

    #[error("[1004] Operation cancelled by caller")]
    Cancelled,

    #[error("[1005] Unknown or unsupported command: {0}")]
    UnsupportedCommand(String),

    #[error("[1006] Deserialization / Serialization failed: {0}")]
    SerializationFailed(String),

    // Tabs & Navigation Errors (2000..2999)
    #[error("[2001] Tab not found: {0}")]
    TabNotFound(String),

    #[error("[2002] Maximum tab capacity reached ({0})")]
    MaxTabsExceeded(usize),

    #[error("[2003] Invalid URL: {0}")]
    InvalidUrl(String),

    // Network & Tor Errors (3000..3999)
    #[error("[3001] DNS resolution failed for host: {0}")]
    DnsResolutionFailed(String),

    #[error("[3002] DNS leak prevented: direct query for {0} is blocked")]
    DnsLeakPrevented(String),

    #[error("[3003] SOCKS5 / Tor proxy unreachable on port {0}")]
    ProxyUnreachable(u16),

    #[error("[3004] Tor circuit rotation failed: {0}")]
    TorRotationFailed(String),

    // Security & Policy Errors (4000..4999)
    #[error("[4001] SSRF protection blocked access to private address: {0}")]
    SsrfBlocked(String),

    #[error("[4002] Insecure redirect from HTTPS to HTTP blocked: {0}")]
    InsecureRedirectBlocked(String),

    #[error("[4003] Permission denied for origin: {0}")]
    PermissionDenied(String),

    // Vault & Crypto Errors (5000..5999)
    #[error("[5001] Secure Vault is locked or uninitialized")]
    VaultLocked,

    #[error("[5002] Decryption failed: invalid master key or corrupted data")]
    DecryptionFailed,

    #[error("[5003] Passkey credential not found for relying party: {0}")]
    PasskeyNotFound(String),
}

impl ProtocolError {
    /// Performs error code operation.
    pub fn error_code(&self) -> u32 {
        match self {
            Self::IncompatibleVersion(..) => 1001,
            Self::InvalidEnvelope(..) => 1002,
            Self::Timeout(..) => 1003,
            Self::Cancelled => 1004,
            Self::UnsupportedCommand(..) => 1005,
            Self::SerializationFailed(..) => 1006,

            Self::TabNotFound(..) => 2001,
            Self::MaxTabsExceeded(..) => 2002,
            Self::InvalidUrl(..) => 2003,

            Self::DnsResolutionFailed(..) => 3001,
            Self::DnsLeakPrevented(..) => 3002,
            Self::ProxyUnreachable(..) => 3003,
            Self::TorRotationFailed(..) => 3004,

            Self::SsrfBlocked(..) => 4001,
            Self::InsecureRedirectBlocked(..) => 4002,
            Self::PermissionDenied(..) => 4003,

            Self::VaultLocked => 5001,
            Self::DecryptionFailed => 5002,
            Self::PasskeyNotFound(..) => 5003,
        }
    }
}
