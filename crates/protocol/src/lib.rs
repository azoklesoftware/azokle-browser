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

pub mod domains;
pub mod endpoint;
pub mod envelope;
pub mod error;
pub mod version;

pub use domains::*;
pub use endpoint::*;
pub use envelope::*;
pub use error::*;
pub use version::*;

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_protocol_version_and_envelope() {
        let env = ProtocolEnvelope::new(
            MessageEndpoint::DesktopUI,
            MessageEndpoint::RustCore,
            MessagePayload::Request(RequestPayload::DnsResolve {
                host: "search.azokle.com".to_string(),
            }),
        );

        assert_eq!(env.version, ProtocolVersion::CURRENT);
        assert!(env.message_id.starts_with("msg_"));
    }

    #[test]
    fn test_fuzz_malformed_json_resilience() {
        let malformed_inputs = [
            "",
            "{",
            "{\"version\": \"1.0.0\"}",
            "{\"version\": {\"major\": 99, \"minor\": 0, \"patch\": 0}}",
            "null",
            "12345",
            "{\"message_id\": 42}",
            "{\"version\": {\"major\": 1, \"minor\": 0, \"patch\": 0}, \"payload\": \"invalid\"}",
            "<!DOCTYPE html><html><body>Not JSON</body></html>",
            "\0\0\0\0",
        ];

        for input in &malformed_inputs {
            let res = ProtocolEnvelope::from_json(input);
            assert!(res.is_err(), "Expected error on malformed input: {}", input);
        }
    }

    #[test]
    fn test_fuzz_corrupted_cbor_resilience() {
        let corrupted_byte_arrays: [&[u8]; 6] = [
            &[],
            &[0xff, 0xff, 0xff, 0xff],
            &[0x00],
            &[0xa1, 0x64, 0x74, 0x65, 0x73, 0x74],
            &[0xbf, 0xff],
            &[0x82, 0x01, 0x02, 0x03],
        ];

        for bytes in &corrupted_byte_arrays {
            let res = ProtocolEnvelope::from_cbor(bytes);
            assert!(res.is_err(), "Expected error on corrupted CBOR bytes");
        }
    }

    #[test]
    fn test_incompatible_major_version_rejected() {
        let json = serde_json::json!({
            "version": { "major": 2, "minor": 0, "patch": 0 },
            "message_id": "msg_123_1",
            "correlation_id": null,
            "timestamp_ms": 1700000000000u64,
            "source": "AndroidUI",
            "target": "RustCore",
            "payload": {
                "Command": {
                    "TabCloseAll": { "include_private": false }
                }
            }
        })
        .to_string();

        let res = ProtocolEnvelope::from_json(&json);
        match res {
            Err(ProtocolError::IncompatibleVersion(req, supported)) => {
                assert_eq!(req, "2.0.0");
                assert_eq!(supported, "1.0.0");
            }
            other => panic!("Expected IncompatibleVersion error, got {:?}", other),
        }
    }
}
